/*******************************************************************************
 * Copyright (c) 2008, 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context.internal.preferences;

import org.eclipse.gyrex.context.internal.ContextActivator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;

/**
 * Utility for working with Eclipse preferences.
 */
class EclipsePreferencesUtil {

	public static final String PATH_SEPARATOR = String.valueOf(IPath.SEPARATOR);
	public static final String DOUBLE_SLASH = "//"; //$NON-NLS-1$
	public static final String EMPTY_STRING = ""; //$NON-NLS-1$

	/**
	 * Returns a 2 element String array.
	 * <ul>
	 * <li>element 0 - the path</li>
	 * <li>element 1 - the key</li>
	 * </ul>
	 * The path may be <code>null</code>. The key is never <code>null</code>.
	 * <p>
	 * Source:
	 * <code>org.eclipse.core.internal.preferences.EclipsePreferences#decodePath(String)</code>
	 * </p>
	 * 
	 * @param fullPath
	 *            the path
	 * @return 2 element String array
	 */
	public static String[] decodePath(final String fullPath) {
		String key = null;
		String path = null;

		// check to see if we have an indicator which tells us where the path ends
		final int index = fullPath.indexOf(DOUBLE_SLASH);
		if (index == -1) {
			// we don't have a double-slash telling us where the path ends
			// so the path is up to the last slash character
			final int lastIndex = fullPath.lastIndexOf(IPath.SEPARATOR);
			if (lastIndex == -1) {
				key = fullPath;
			} else {
				path = fullPath.substring(0, lastIndex);
				key = fullPath.substring(lastIndex + 1);
			}
		} else {
			// the child path is up to the double-slash and the key
			// is the string after it
			path = fullPath.substring(0, index);
			key = fullPath.substring(index + 2);
		}

		// adjust if we have an absolute path
		if (path != null) {
			if (path.length() == 0) {
				path = null;
			} else if (path.charAt(0) == IPath.SEPARATOR) {
				path = path.substring(1);
			}
		}

		return new String[] { path, key };
	}

	public static IPreferencesService getPreferencesService() {
		return ContextActivator.getInstance().getPreferencesService();
	}

	public static IEclipsePreferences getRootNode() {
		return ContextActivator.getInstance().getPreferencesService().getRootNode();
	}

	/**
	 * Return a relative path.
	 * <p>
	 * Source:
	 * <code>org.eclipse.core.internal.preferences.EclipsePreferences#makeRelative(String)</code>
	 * </p>
	 * 
	 * @param path
	 * @return a relative path (never <code>null</code>)
	 */
	public static String makeRelative(final String path) {
		String result = path;
		if (path == null) {
			return EMPTY_STRING;
		}
		if ((path.length() > 0) && (path.charAt(0) == IPath.SEPARATOR)) {
			result = path.length() == 1 ? EMPTY_STRING : path.substring(1);
		}
		return result;
	}

	/**
	 * Hidden
	 */
	private EclipsePreferencesUtil() {
		// empty
	}
}
