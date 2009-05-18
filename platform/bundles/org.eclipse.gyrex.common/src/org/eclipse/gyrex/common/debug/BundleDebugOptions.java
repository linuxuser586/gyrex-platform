/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.common.debug;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.gyrex.common.lifecycle.IShutdownParticipant;
import org.eclipse.gyrex.common.logging.LogAudience;
import org.eclipse.gyrex.common.logging.LogImportance;
import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * Common superclass for debug options.
 * <p>
 * Typically sub-class declare non-final public static boolean fields. The field
 * values are looked up from the debug options when the bundle is started and
 * from there on updated automatically whenever the bundle's debug options are
 * modified until the bundle is stopped.
 * </p>
 * <p>
 * Note, the prefered way of integrating with {@link BaseBundleActivator} is to
 * simply overwrite its <code>getDebugOptions</code> method and return the
 * sub-class containing the fields to initialize.
 * </p>
 * <p>
 * This class intentionally does not provide any convenience methods for
 * tracing/logging. The recommendation is to use your prefered logging API or
 * SLF4J which is recommended by Gyrex. Gyrex integrates with SLF4J in a way
 * which allows you to configure tracing/debug options as well as targets (eg. a
 * trace/debug file, a browser console, etc.) at runtime.
 * </p>
 */
public abstract class BundleDebugOptions {

	/**
	 * The {@link DebugOptionsListener} for initializing and updating the debug
	 * options.
	 */
	static final class BundleDebugOptionsListener implements DebugOptionsListener, IShutdownParticipant {
		private BaseBundleActivator bundleActivator;
		private Class bundleDebugClass;

		public BundleDebugOptionsListener(final BaseBundleActivator bundleActivator, final Class bundleDebugClass) {
			this.bundleActivator = bundleActivator;
			this.bundleDebugClass = bundleDebugClass;
		}

		@Override
		public void optionsChanged(final DebugOptions options) {
			final BaseBundleActivator bundleActivator = this.bundleActivator;
			final Class clazz = bundleDebugClass;
			if ((null != bundleActivator) && (null != clazz)) {
				// TODO: we may need to wrap this into a doPriv call
				loadDebugOptions(bundleActivator, clazz, options);
			}
		}

		@Override
		public void shutdown() throws Exception {
			// release references
			bundleActivator = null;
			bundleDebugClass = null;
		}
	}

	private static final int MOD_EXPECTED = Modifier.PUBLIC | Modifier.STATIC;
	private static final int MOD_MASK = MOD_EXPECTED | Modifier.FINAL;

	/**
	 * Initialize the given class with the debug option from the bundle.
	 * <p>
	 * Calling this method will register a {@link DebugOptionsListener} on
	 * behalf of the specified bundle. Thus, whenever the bundle's debug options
	 * are updated the class constants will be updated too.
	 * </p>
	 * 
	 * @param bundleActivator
	 *            the bundle activator
	 * @param clazz
	 *            the class where the constants will exist
	 */
	public static void initializeDebugOptions(final BaseBundleActivator bundleActivator, final Class clazz) {
		// get context
		final BundleContext context = bundleActivator.getBundle().getBundleContext();

		// create the debug options listener
		final Dictionary<String, Object> props = new Hashtable<String, Object>(4);
		props.put(DebugOptions.LISTENER_SYMBOLICNAME, bundleActivator.getSymbolicName());
		props.put(Constants.SERVICE_VENDOR, "Eclipse Gyrex Project");
		props.put(Constants.SERVICE_DESCRIPTION, "Debug options listener.");
		final BundleDebugOptionsListener debugOptionsListener = new BundleDebugOptionsListener(bundleActivator, clazz);

		// register the service for loading the debug options
		context.registerService(DebugOptionsListener.class.getName(), debugOptionsListener, props);
		bundleActivator.addShutdownParticipant(debugOptionsListener);
	}

	/**
	 * Loads the debug options and initializes the fields in the specified debug
	 * options class.
	 * 
	 * @param bundleActivator
	 * @param clazz
	 * @param debugOptions
	 */
	static void loadDebugOptions(final BaseBundleActivator bundleActivator, final Class clazz, final DebugOptions debugOptions) {
		// get the symbolic names
		final String symbolicName = bundleActivator.getSymbolicName();

		// get global debug option
		final boolean isDebug = debugOptions.getBooleanOption(symbolicName + "/debug", false);//$NON-NLS-1$

		final boolean isAccessible = (clazz.getModifiers() & Modifier.PUBLIC) != 0;
		final Field[] fieldArray = clazz.getDeclaredFields();
		for (final Field field : fieldArray) {

			// can only set value of public static non-final fields
			if ((field.getModifiers() & MOD_MASK) != MOD_EXPECTED) {
				continue;
			}

			try {
				// Check to see if we are allowed to modify the field. If we aren't (for instance
				// if the class is not public) then change the accessible attribute of the field
				// before trying to set the value.
				if (!isAccessible) {
					field.setAccessible(true);
				}

				// Set the value into the field. We should never get an exception here because
				// we know we have a public static non-final field. If we do get an exception, silently
				// log it and continue. This means that the field will (most likely) be un-initialized and
				// will fail later in the code and if so then we will see both the NPE and this error.
				final String key = symbolicName + "/" + field.getName();
				if (field.getType() == Boolean.TYPE) {
					field.set(null, new Boolean(isDebug && debugOptions.getBooleanOption(key, false)));
				} else if (field.getType() == String.class) {
					String value = debugOptions.getOption(key);

					// be smart and set defaults if possible
					if (field.getName().equals("debugPrefix")) {
						final int lastDot = symbolicName.lastIndexOf('.');
						value = lastDot != -1 ? symbolicName.substring(lastDot) : symbolicName;
					}

					field.set(null, null != value ? value : "");
				} else {
					// ignore type
				}
			} catch (final Exception e) {
				bundleActivator.getLog().log(MessageFormat.format("Exception setting debug option \"{0}\" in class \"{1}\": {2}", field.getName(), clazz.getName(), e.getMessage()), e, (Object) null, LogImportance.WARNING, LogAudience.DEVELOPER); //$NON-NLS-1$
			}
		}
	}
}
