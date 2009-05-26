/**
 * Copyright (c) 2009 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.context.internal.configuration;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.gyrex.common.logging.LogAudience;
import org.eclipse.gyrex.common.logging.LogImportance;
import org.eclipse.gyrex.common.logging.LogSource;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.internal.ContextActivator;
import org.eclipse.gyrex.context.internal.ContextDebug;
import org.eclipse.gyrex.preferences.PlatformScope;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for context configuration.
 */
public final class ContextConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(ContextConfiguration.class);

	/** CONTEXTS */
	public static final String CONTEXTS = "contexts";

	/** the path to the context preferences root node */
	public static final IPath CONTEXT_PREF_ROOT = new Path(ContextActivator.SYMBOLIC_NAME).append(CONTEXTS).makeRelative();

	/**
	 * Finds a filter if available from the context configuration.
	 * 
	 * @param context
	 *            the context
	 * @param typeName
	 *            the requested type name
	 * @return the filter (maybe <code>null</code> if none is explicitly defined
	 *         for the context
	 */
	public static Filter findFilter(final IRuntimeContext context, final String typeName) {
		// the context path
		IPath contextPath = context.getContextPath();

		// get preferences root node
		final IEclipsePreferences rootNode = getRootNodeForContextPreferences();

		// lookup filter in this context
		Filter filter = readFilterFromPreferences(context, rootNode, contextPath, typeName);
		if (null != filter) {
			return filter;
		}

		// search parent contexts
		while ((null == filter) && !contextPath.isRoot()) {
			filter = readFilterFromPreferences(context, rootNode, contextPath = contextPath.removeLastSegments(1), typeName);
		}

		// return what we have (may be nothing)
		return filter;
	}

	private static String getPreferencesPathForContextObjectFilterSetting(final IPath contextPath) {
		return contextPath.makeRelative().toString();
	}

	public static IEclipsePreferences getRootNodeForContextPreferences() {
		return (IEclipsePreferences) new PlatformScope().getNode(ContextActivator.SYMBOLIC_NAME).node(CONTEXTS);
	}

	/**
	 * Reads a filter from the preferences of the specified context path.
	 * 
	 * @param context
	 *            the context
	 * @param root
	 *            the preferences root node
	 * @param contextPath
	 *            the context path
	 * @param typeName
	 *            the type name
	 * @return
	 */
	private static Filter readFilterFromPreferences(final IRuntimeContext context, final IEclipsePreferences root, final IPath contextPath, final String typeName) {
		// get the preferences
		final String preferencesPath = getPreferencesPathForContextObjectFilterSetting(contextPath);
		try {
			if (!root.nodeExists(preferencesPath)) {
				return null;
			}
		} catch (final BackingStoreException e) {
			ContextActivator.getInstance().getLog().log(MessageFormat.format("Error while accessing the preferences backend for context path \"{0}\": {1}", contextPath, e.getMessage()), e, context, LogAudience.ADMIN, LogImportance.WARNING, LogSource.PLATFORM);
			return null;
		}

		// get the filter string
		final String filterString = root.node(preferencesPath).get(typeName, null);
		if (null == filterString) {
			return null;
		}

		// create the filter
		try {
			return FrameworkUtil.createFilter(filterString);
		} catch (final InvalidSyntaxException e) {
			ContextActivator.getInstance().getLog().log(MessageFormat.format("Invalid syntax in context path \"{0}\" key \"{1}\": {2} ", contextPath, typeName, e.getMessage()), e, context, LogAudience.ADMIN, LogImportance.WARNING, LogSource.PLATFORM);
			return null;
		}
	}

	/**
	 * Configures a context to use the specified filter for the given type name.
	 * 
	 * @param context
	 *            the context
	 * @param typeName
	 *            the requested type name
	 * @return the filter (maybe <code>null</code> if none is explicitly defined
	 *         for the context
	 */
	public static void setFilter(final IRuntimeContext context, final String typeName, final Filter filter) {
		// the context path
		final IPath contextPath = context.getContextPath();

		// get preferences root node
		final IEclipsePreferences rootNode = getRootNodeForContextPreferences();

		// log a debug message
		if (ContextDebug.objectLifecycle) {
			LOG.debug("Setting filter in context {} for type {} to {}", new Object[] { context, typeName, filter });
		}

		// set the preferences
		final String preferencesPath = getPreferencesPathForContextObjectFilterSetting(contextPath);
		try {
			final Preferences contextPreferences = rootNode.node(preferencesPath);
			if (null != filter) {
				contextPreferences.put(typeName, filter.toString());
			} else {
				contextPreferences.remove(typeName);
			}
			contextPreferences.flush();
		} catch (final BackingStoreException e) {
			ContextActivator.getInstance().getLog().log(MessageFormat.format("Error while accessing the preferences backend for context path \"{0}\": {1}", contextPath, e.getMessage()), e, context, LogAudience.ADMIN, LogImportance.WARNING, LogSource.PLATFORM);
		}
	};

	/**
	 * Hidden constructor.
	 */
	private ContextConfiguration() {
		// empty
	}
}
