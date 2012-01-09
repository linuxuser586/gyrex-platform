/**
 * Copyright (c) 2009, 2011 AGETO Service GmbH and others.
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

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.internal.ContextActivator;
import org.eclipse.gyrex.context.internal.ContextDebug;
import org.eclipse.gyrex.preferences.CloudScope;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

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
	 *         for the context)
	 */
	public static String findFilter(IPath contextPath, final String typeName) {
		// get preferences root node
		final IEclipsePreferences rootNode = getRootNodeForContextPreferences();

		// lookup filter in this context
		String filter = readFilterFromPreferences(rootNode, contextPath, typeName);
		if (null != filter) {
			return filter;
		}

		// search parent contexts
		while ((null == filter) && !contextPath.isRoot()) {
			filter = readFilterFromPreferences(rootNode, contextPath = contextPath.removeLastSegments(1), typeName);
		}

		// return what we have (may be nothing)
		return filter;
	}

	private static String getPreferencesPathForContextObjectFilterSetting(final IPath contextPath) {
		return contextPath.makeRelative().toString();
	}

	public static IEclipsePreferences getRootNodeForContextPreferences() {
		return (IEclipsePreferences) CloudScope.INSTANCE.getNode(ContextActivator.SYMBOLIC_NAME).node(CONTEXTS);
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
	private static String readFilterFromPreferences(final IEclipsePreferences root, final IPath contextPath, final String typeName) {
		// get the preferences
		final String preferencesPath = getPreferencesPathForContextObjectFilterSetting(contextPath);
		try {
			if (!root.nodeExists(preferencesPath)) {
				return null;
			}
		} catch (final BackingStoreException e) {
			LOG.warn("Error while accessing the preferences backend for context path \"{}\": {}", contextPath, e.getMessage());
			return null;
		}

		// get the filter string
		final String filterString = root.node(preferencesPath).get(typeName, null);
		if (null == filterString) {
			return null;
		}

		// return the filter
		return filterString;
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
	public static void setFilter(final IRuntimeContext context, final String typeName, final String filter) {
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
				contextPreferences.put(typeName, filter);
			} else {
				contextPreferences.remove(typeName);
			}
			contextPreferences.flush();
		} catch (final BackingStoreException e) {
			LOG.warn("Error while accessing the preferences backend for context path \"{}\": {}", contextPath, e.getMessage());
		}

		// TODO we need to flush the context hierarch for the type name here
		// the flush can potentially be smart to only flush the contexts which are affected by the change
	};

	/**
	 * Hidden constructor.
	 */
	private ContextConfiguration() {
		// empty
	}
}
