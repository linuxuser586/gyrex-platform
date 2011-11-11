/*******************************************************************************
 * Copyright (c) 2008, 2011 Gunnar Wagenknecht and others.
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
import org.eclipse.gyrex.preferences.internal.util.EclipsePreferencesUtil;

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;

import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Object representing the cloud scope in the Eclipse preferences hierarchy.
 * <p>
 * The cloud scope can be used as a context for searching for preference values
 * (in the IPreferencesService APIs) or for determining the correct preference
 * node to set values in the store.
 * </p>
 * <p>
 * Cloud preferences are stored on a global basis. Usually they are shared
 * across a whole cluster of nodes. The actual storage type is an deployment
 * detail which clients should not depend on. It's a decision made by operators
 * of the system. For example, for a standalone system it's perfectly valid to
 * just store the preferences on the local disc. A clustered system with
 * distributed nodes might use an LDAP server or a database.
 * </p>
 * <p>
 * It's important to be aware of the distributed nature of cloud preferences.
 * The preferences may not be available all the time. Instead, clients relying
 * on cloud preferences should implement methods to be resistant against such
 * issues (eg. by registering listeners or delayed retries after failed
 * attempts).
 * </p>
 * <p>
 * No {@link #getLocation() location} is provided for cloud preferences.
 * </p>
 * <p>
 * The path for preferences defined in the cloud scope hierarchy is as follows:
 * <code>/cloud/&lt;qualifier&gt;</code>
 * </p>
 * <p>
 * This class is not intended to be subclassed. A shared instance may be
 * obtained via {@link #INSTANCE}
 * </p>
 * 
 * @see ModificationConflictException
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class CloudScope implements IScopeContext {

	private static final Logger LOG = LoggerFactory.getLogger(CloudScope.class);

	/**
	 * String constant (value of <code>"cloud"</code>) used for the scope name
	 * for the cloud preference scope.
	 */
	public static final String NAME = "cloud"; //$NON-NLS-1$

	/** the shared instance */
	public static final CloudScope INSTANCE = new CloudScope();

	private static volatile IEclipsePreferences cloudRootNode;

	private static Preferences getCloudScopeNode() {
		if (null == cloudRootNode) {
			int pass = 0;
			while (null == cloudRootNode) {
				pass++;
				try {
					// this is really weird but our scope factory can only be understood by the preference service
					// if the extension registry is available, we therefor force its activation here
					// (see http://bugs.eclipse.org/340243)
					PreferencesActivator.getInstance().getService(IExtensionRegistry.class);

					// now instantiate the node
					cloudRootNode = (IEclipsePreferences) EclipsePreferencesUtil.getPreferencesService().getRootNode().node(NAME);
				} catch (final IllegalStateException e) {
					if (pass > 5) {
						LOG.error("Unable to activating the cloud preferences scope. Please check the server logs and the server configuration. ", e);
						throw e;
					} else {
						LOG.warn("Error while activating the cloud preferences scope. Will try again. (Pass {}) {}", pass, ExceptionUtils.getRootCauseMessage(e));
						try {
							Thread.sleep(150);
						} catch (final InterruptedException e1) {
							// abort
							Thread.currentThread().interrupt();
							throw e;
						}
					}
				}
			}
		}
		return cloudRootNode;
	}

	/**
	 * Creates a new scope instance.
	 * <p>
	 * Although public this constructor only exists for dependency injection
	 * purposes. Wherever possible clients should reuse {@link #INSTANCE the
	 * shared instance}.
	 * </p>
	 * 
	 * @noreference This constructor is not intended to be referenced by
	 *              clients.
	 */
	public CloudScope() {
		// empty
	}

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
	 * Returns <code>null</code> to indicate that the cloud scope does not
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
		return (IEclipsePreferences) getCloudScopeNode().node(qualifier);
	}

	@Override
	public int hashCode() {
		// org.eclipse.core.internal.preferences.AbstractScope#hashCode()
		return NAME.hashCode();
	}

}
