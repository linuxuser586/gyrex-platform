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
package org.eclipse.gyrex.preferences;

import org.eclipse.gyrex.preferences.internal.PreferencesActivator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;

/**
 * Object representing the platform scope in the Eclipse preferences hierarchy.
 * Can be used as a context for searching for preference values (in the
 * IPreferencesService APIs) or for determining the correct preference node to
 * set values in the store.
 * <p>
 * Platform preferences are stored on a global basis. Usually they are shared
 * across a whole cluster of nodes. The actual storage type is an deployment
 * detail which clients should not depend on. It's a decision made by operators
 * of the system. For example, for a standalone system it's perfectly valid to
 * just store the preferences on the local disc. A clustered system with
 * distributed nodes might use an LDAP server or a database.
 * </p>
 * <p>
 * It's important to be aware of the distributed nature of platform preferences.
 * The preferences may not be available all the time. Instead, clients relying
 * on platform preferences should implement methods to be resistant against such
 * issues (eg. by registering listeners or delayed retries after failed
 * attempts).
 * </p>
 * <p>
 * No {@link #getLocation() location} is provided for platform preferences.
 * </p>
 * <p>
 * The path for preferences defined in the platform scope hierarchy is as
 * follows: <code>/platform/&lt;qualifier&gt;</code>
 * </p>
 * <p>
 * This class is not intended to be subclassed. This class may be instantiated.
 * </p>
 */
public final class PlatformScope implements IScopeContext {

	/**
	 * String constant (value of <code>"platform"</code>) used for the scope
	 * name for the platform preference scope.
	 */
	public static final String NAME = "platform"; //$NON-NLS-1$

	@Override
	public boolean equals(final Object o) {
		// org.eclipse.core.internal.preferences.AbstractScope#equals(Object o)
		if (this == o) {
			return true;
		}
		if ((null == o) || !(o instanceof IScopeContext)) {
			return false;
		}
		final IScopeContext other = (IScopeContext) o;
		if (!getName().equals(other.getName())) {
			return false;
		}
		final IPath location = getLocation();
		return location == null ? other.getLocation() == null : location.equals(other.getLocation());
	}

	/**
	 * Returns <code>null</code> to indicate that the platform scope does not
	 * support a file system location for sharing files/content.
	 * 
	 * @return <code>null</code> (no location)
	 */
	@Override
	public IPath getLocation() {
		return null; // no location
	}

	@Override
	public String getName() {
		return NAME;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.core.runtime.preferences.IScopeContext#getNode(java.lang.String)
	 * @throws IllegalArgumentException
	 *             if the qualifier is invalid
	 * @throws IllegalStateException
	 *             if the preference system is in an inactive state (eg. not
	 *             connected to the cloud)
	 */
	@Override
	public IEclipsePreferences getNode(final String qualifier) throws IllegalArgumentException, IllegalStateException {
		if (qualifier == null) {
			throw new IllegalArgumentException("qualifier must not be null");
		}
		try {
			return (IEclipsePreferences) PreferencesActivator.getInstance().getPreferencesService().getRootNode().node(getName()).node(qualifier);
		} catch (final IllegalStateException e) {
			throw new IllegalStateException("The Gyrex Preferences System is in an inactive state. Please release any references and try again later.", e);
		}
	}

	@Override
	public int hashCode() {
		// org.eclipse.core.internal.preferences.AbstractScope#hashCode()
		return NAME.hashCode();
	}

}
