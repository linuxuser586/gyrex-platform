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
package org.eclipse.cloudfree.boot.internal.app;


import org.eclipse.cloudfree.common.debug.BundleDebug;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * A server role
 */
public class ServerRole {

	private final String id;
	private final String name;
	private final String[] bundleSymbolicNames;

	/**
	 * Creates a new instance.
	 * 
	 * @param id
	 * @param name
	 * @param requiredBundleSymbolicNames
	 *            list of bundles which need to be started for this role
	 */
	public ServerRole(final String id, final String name, final String[] requiredBundleSymbolicNames) {
		this.id = id;
		this.name = name;
		bundleSymbolicNames = requiredBundleSymbolicNames;
	}

	/**
	 * Activates the role.
	 * 
	 * @throws BundleException
	 */
	public void activate() throws ActivationException {
		if (AppDebug.debugRoles) {
			BundleDebug.debug("Activating server role " + getId());
		}
		for (final String bundleName : bundleSymbolicNames) {
			try {
				startBundle(bundleName);
			} catch (final Exception e) {
				throw new ActivationException(NLS.bind("Error starting bundle \"{0}\": {1}", bundleName, e.getMessage()), e);
			}
		}
	}

	/**
	 * Returns the id.
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Starts a bundle transient so that it is not restarted automatically on
	 * the next start.
	 * 
	 * @param symbolicName
	 * @throws BundleException
	 */
	private void startBundle(final String symbolicName) throws BundleException, IllegalStateException {
		final Bundle bundle = AppActivator.getInstance().getBundle(symbolicName);
		if (null != bundle) {
			final int originalState = bundle.getState();
			if ((originalState & Bundle.ACTIVE) != 0) {
				return; // bundle is already active
			}
			try {
				// attempt to activate the bundle
				bundle.start(Bundle.START_TRANSIENT);
			} catch (final BundleException e) {
				if ((bundle.getState() & Bundle.ACTIVE) != 0) {
					// this can happen if the bundle was started by a different thread (the framework) already
					return;
				}
				if (((originalState & Bundle.STARTING) != 0) && ((bundle.getState() & Bundle.STARTING) != 0)) {
					// this can happen if the bundle was in the process of being activated on this thread, just return
					return;
				}
				throw e;
			}
		}
	}
}
