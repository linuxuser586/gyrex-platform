/**
 * Copyright (c) 2009, 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.context.preferences;

import org.eclipse.gyrex.context.IRuntimeContext;

import org.eclipse.osgi.util.NLS;

/**
 * This class may be used to obtain the context preferences.
 * <p>
 * This class is not intended to be subclassed or instantiated. It provides
 * static methods to streamline the preferences access.
 * </p>
 * 
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @deprecated Use {@link IRuntimeContext#getPreferences()}
 */
@Deprecated
public final class PreferencesUtil {

	/**
	 * Returns the context preferences.
	 * <p>
	 * This is a convenience method which simply asks the context of the
	 * {@link IRuntimeContextPreferences} object. If the context doesn't have
	 * such an object it will fail with an {@link IllegalStateException} because
	 * it is expected that the context (or the system) is in an unusable state
	 * anyway. The operation may be retried later.
	 * </p>
	 * <p>
	 * Callers should not hold onto the returned object for a longer time. The
	 * context is allowed to be reconfigured at runtime. Additionally, bundles
	 * contributing objects are allowed to come and got at any time in a dynamic
	 * system.
	 * </p>
	 * 
	 * @param context
	 *            the context for preferences lookup (may not be
	 *            <code>null</code>)
	 * @return the preferences object of querying context preferences
	 * @throws IllegalArgumentException
	 *             if the context is <code>null</code> or invalid
	 * @throws IllegalStateException
	 *             if no suitable manager implementation is currently available
	 * @see IRuntimeContext#get(Class)
	 * @deprecated Use {@link IRuntimeContext#getPreferences()}
	 */
	@Deprecated
	public static IRuntimeContextPreferences getPreferences(final IRuntimeContext context) throws IllegalArgumentException, IllegalStateException {
		if (null == context) {
			throw new IllegalArgumentException("context must not be null");
		}

		// get preferences
		final IRuntimeContextPreferences preferences = context.getPreferences();
		if (null == preferences) {
			throw new IllegalStateException(NLS.bind("no preferences available for context \"{0}\"", context));
		}

		return preferences;
	}

	/**
	 * Hidden constructor
	 */
	private PreferencesUtil() {
		// empty
	}
}
