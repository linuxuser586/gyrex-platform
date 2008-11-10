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
package org.eclipse.cloudfree.common.debug;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;


import org.eclipse.cloudfree.common.logging.LogAudience;
import org.eclipse.cloudfree.common.logging.LogImportance;
import org.eclipse.cloudfree.common.runtime.BaseBundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Common superclass for debug options. Provides convenience methods for
 * debugging and tracing.
 * <p>
 * Typically sub-class declare non-final public static boolean fields. The field
 * values are looked up from the debug options during bundle activation.
 * </p>
 */
public abstract class BundleDebug {

	private static final int MOD_EXPECTED = Modifier.PUBLIC | Modifier.STATIC;
	private static final int MOD_MASK = MOD_EXPECTED | Modifier.FINAL;

	/**
	 * Prints the specified debug message to the console.
	 * 
	 * @param message
	 *            the message
	 */
	public static void debug(final String message) {
		if (null == message) {
			return;
		}
		System.out.println(message);
	}

	/**
	 * Prints the specified debug message and exception the console.
	 * 
	 * @param message
	 *            the message
	 */
	public static void debug(final String message, final Throwable cause) {
		if (null != message) {
			System.err.println(message);
		}
		if (null != cause) {
			cause.printStackTrace(System.err);
		}
	}

	/**
	 * Initialize the given class with the debug option from the bundle.
	 * <p>
	 * This method can also be used to re-initialize the debug options at any
	 * point in time.
	 * </p>
	 * 
	 * @param bundleActivator
	 *            the bundle activator
	 * @param clazz
	 *            the class where the constants will exist
	 */
	public static void initializeDebugOptions(final BaseBundleActivator bundleActivator, final Class clazz) {
		if (System.getSecurityManager() == null) {
			load(bundleActivator, clazz);
			return;
		}
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			public Object run() {
				load(bundleActivator, clazz);
				return null;
			}
		});
	}

	/**
	 * Loads the debug options from the specified bundle and initializes the
	 * fields in the specified debug options class.
	 * 
	 * @param bundleActivator
	 * @param clazz
	 */
	private static void load(final BaseBundleActivator bundleActivator, final Class clazz) {
		final BundleContext context = bundleActivator.getBundle().getBundleContext();
		final ServiceReference serviceReference = context.getServiceReference(org.eclipse.osgi.service.debug.DebugOptions.class.getName());
		if (null == serviceReference) {
			return;
		}

		// get service	
		final org.eclipse.osgi.service.debug.DebugOptions platformDebugOptions = (org.eclipse.osgi.service.debug.DebugOptions) context.getService(serviceReference);
		if (null == platformDebugOptions) {
			return;
		}

		try {
			final String symbolicName = bundleActivator.getSymbolicName();
			if (symbolicName == null) {
				return;
			}

			// get global debug option
			final boolean isDebug = platformDebugOptions.getBooleanOption(symbolicName + "/debug", false);//$NON-NLS-1$

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
						field.set(null, new Boolean(isDebug && platformDebugOptions.getBooleanOption(key, false)));
					} else if (field.getType() == String.class) {
						String value = platformDebugOptions.getOption(key);

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
		} finally {
			// unget service
			context.ungetService(serviceReference);
		}
	}

}
