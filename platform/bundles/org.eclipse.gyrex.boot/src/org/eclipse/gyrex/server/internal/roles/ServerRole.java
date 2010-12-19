/*******************************************************************************
 * Copyright (c) 2008, 2009 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.server.internal.roles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.gyrex.boot.internal.app.ActivationException;
import org.eclipse.gyrex.boot.internal.app.AppActivator;
import org.eclipse.gyrex.boot.internal.app.BootDebug;

import org.eclipse.osgi.util.NLS;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.application.ApplicationException;
import org.osgi.service.application.ApplicationHandle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A server role
 */
public class ServerRole {

	private static final Logger LOG = LoggerFactory.getLogger(ServerRole.class);

	private final String id;
	private final String name;
	private final List<String> requiredBundleNames;
	private final List<String> requiredApplicationIds;

	private final Map<String, ApplicationHandle> launchedApps = new HashMap<String, ApplicationHandle>(3);

	private final AtomicBoolean active = new AtomicBoolean();

	ServerRole(final String id, final String name, final List<String> requiredBundleNames, final List<String> requiredApplicationIds) {
		this.id = id;
		this.name = name;
		this.requiredBundleNames = requiredBundleNames;
		this.requiredApplicationIds = requiredApplicationIds;
	}

	/**
	 * Activates the role.
	 * 
	 * @throws BundleException
	 */
	public void activate() throws ActivationException {
		if (!active.compareAndSet(false, true)) {
			return;
		}

		if (BootDebug.debugRoles) {
			LOG.debug("Activating server role " + getId());
		}
		for (final String bundleName : requiredBundleNames) {
			try {
				startBundle(bundleName);
			} catch (final Exception e) {
				throw new ActivationException(NLS.bind("Error starting bundle \"{0}\": {1}", bundleName, e.getMessage()), e);
			}
		}
		for (final String applicationId : requiredApplicationIds) {
			try {
				startApplication(applicationId);
			} catch (final Exception e) {
				throw new ActivationException(NLS.bind("Error starting application \"{0}\": {1}", applicationId, e.getMessage()), e);
			}
		}
	}

	/**
	 * Deactivates the server role.
	 */
	public void deactivate() {
		if (!active.compareAndSet(true, false)) {
			return;
		}

		if (BootDebug.debugRoles) {
			LOG.debug("Deactivating server role " + getId());
		}

		for (final Entry<String, ApplicationHandle> application : launchedApps.entrySet()) {
			if (BootDebug.debugRoles) {
				LOG.debug("Stopping application {}", application.getKey());
			}
			try {
				application.getValue().destroy();
			} catch (final IllegalStateException e) {
				if (BootDebug.debugRoles) {
					LOG.debug("Application {} already stopped.", application.getKey());
				}
			} catch (final Exception e) {
				LOG.warn("Error during shutdown of application {} while deactivating role {}. {}", new Object[] { application.getKey(), getId(), e.getMessage() }, e);
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
	 * Starts an application and keeps track of started instances.
	 * 
	 * @param applicationId
	 * @throws IllegalStateException
	 * @throws ApplicationException
	 */
	private void startApplication(final String applicationId) throws IllegalStateException, ApplicationException {
		if (BootDebug.debugRoles) {
			LOG.debug("Starting application {}", applicationId);
		}
		final ApplicationDescriptor applicationDescriptor = AppActivator.getInstance().getEclipseApplication(applicationId);
		if (applicationDescriptor == null) {
			throw new IllegalStateException(NLS.bind("Application {0} not found!", applicationId));
		}
		final ApplicationHandle handle = applicationDescriptor.launch(null);
		launchedApps.put(applicationId, handle);
	}

	/**
	 * Starts a bundle transient so that it is not restarted automatically on
	 * the next start.
	 * 
	 * @param symbolicName
	 * @throws BundleException
	 */
	private void startBundle(final String symbolicName) throws BundleException, IllegalStateException {
		if (BootDebug.debugRoles) {
			LOG.debug("Starting bundle {}", symbolicName);
		}
		final Bundle bundle = AppActivator.getInstance().getBundle(symbolicName);
		if (bundle == null) {
			LOG.warn("Bundle {} not avaiable. Server role {} might by dysfunctional!", symbolicName, getId());
			return;
		}
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

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ServerRole [id=").append(id).append(", name=").append(name).append(", active=").append(active).append("]");
		return builder.toString();
	}
}
