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
package org.eclipse.gyrex.cloud.admin;

import org.eclipse.gyrex.cloud.internal.CloudActivator;
import org.eclipse.gyrex.common.identifiers.IdHelper;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;

import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Object representing the node scope in the Eclipse preferences hierarchy.
 * <p>
 * The node scope can be used as a context for searching for preference values
 * (in the IPreferencesService APIs) or for determining the correct preference
 * node to set values in the store.
 * </p>
 * <p>
 * Node preferences are stored on a global basis. Usually they are shared across
 * a whole cluster of nodes. The actual storage type is an deployment detail
 * which clients should not depend on. It's a decision made by operators of the
 * system. For example, for a standalone system it's perfectly valid to just
 * store the preferences on the local disc. A clustered system with distributed
 * nodes might use an LDAP server or a database.
 * </p>
 * <p>
 * It's important to be aware of the distributed nature of node preferences. The
 * preferences may not be available all the time. Instead, clients relying on
 * node preferences should implement methods to be resistant against such issues
 * (eg. by registering listeners or delayed retries after failed attempts).
 * </p>
 * <p>
 * No {@link #getLocation() location} is provided for node preferences.
 * </p>
 * <p>
 * The path for preferences defined in the platform scope hierarchy is as
 * follows: <code>/node/&lt;nodeId&gt;/&lt;qualifier&gt;</code>
 * </p>
 * <p>
 * This class is not intended to be subclassed.
 * </p>
 */
public final class NodeScope implements IScopeContext {

	private static final Logger LOG = LoggerFactory.getLogger(NodeScope.class);

	/**
	 * String constant (value of <code>"node"</code>) used for the scope name
	 * for the node preference scope.
	 */
	public static final String NAME = "node"; //$NON-NLS-1$

	private static volatile IEclipsePreferences scopeRootNode;

	private static Preferences getCloudScopeNode() {
		if (null == scopeRootNode) {
			int pass = 0;
			while (null == scopeRootNode) {
				pass++;
				try {
					scopeRootNode = (IEclipsePreferences) CloudActivator.getInstance().getService(IPreferencesService.class).getRootNode().node(NAME);
				} catch (final IllegalStateException e) {
					if (pass > 5) {
						LOG.error("Unable to activating the node preferences scope. Please check the server logs and the server configuration. ", e);
						throw e;
					} else {
						LOG.warn("Error while activating the node preferences scope. Will try again. (Pass {}) {}", pass, ExceptionUtils.getRootCauseMessage(e));
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
		return scopeRootNode;
	}

	private final String nodeId;

	/**
	 * Creates a new scope instance.
	 * 
	 * @param nodeId
	 *            the node id
	 */
	public NodeScope(final String nodeId) {
		if (!IdHelper.isValidId(nodeId)) {
			throw new IllegalArgumentException("invalid id");
		}
		this.nodeId = nodeId;
	}

	@Override
	public boolean equals(final Object o) {
		// org.eclipse.core.internal.preferences.AbstractScope#equals(Object o)
		if (this == o) {
			return true;
		}
		if ((null == o) || !(o instanceof NodeScope)) {
			return false;
		}
		final NodeScope other = (NodeScope) o;
		if (!getName().equals(other.getName())) {
			return false;
		}
		if (!nodeId.equals(other.nodeId)) {
			return false;
		}
		final IPath location = getLocation();
		return location == null ? other.getLocation() == null : location.equals(other.getLocation());
	}

	/**
	 * Returns <code>null</code> to indicate that the node scope does not
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
	 *             connected to the node)
	 */
	@Override
	public IEclipsePreferences getNode(final String qualifier) throws IllegalArgumentException, IllegalStateException {
		if (qualifier == null) {
			throw new IllegalArgumentException("qualifier must not be null");
		}
		return (IEclipsePreferences) getCloudScopeNode().node(nodeId).node(qualifier);
	}

	@Override
	public int hashCode() {
		// org.eclipse.core.internal.preferences.AbstractScope#hashCode()
		return NAME.hashCode() + nodeId.hashCode();
	}

}
