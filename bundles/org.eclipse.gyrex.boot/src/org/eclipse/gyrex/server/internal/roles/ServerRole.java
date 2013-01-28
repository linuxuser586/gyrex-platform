/*******************************************************************************
 * Copyright (c) 2008, 2012 Gunnar Wagenknecht and others.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.gyrex.boot.internal.BootActivator;
import org.eclipse.gyrex.boot.internal.BootDebug;

import org.eclipse.osgi.util.NLS;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.application.ApplicationException;
import org.osgi.service.application.ApplicationHandle;

import org.apache.commons.lang.StringUtils;

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

	private final LinkedHashMap<String, ApplicationHandle> launchedApps = new LinkedHashMap<String, ApplicationHandle>(3);

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
	void activate() throws ActivationException {
		if (!active.compareAndSet(false, true)) {
			if (BootDebug.roles) {
				LOG.debug("Server role {} already active!", getId());
			}
			return;
		}

		// log a message
		LOG.info("Activating {}.", StringUtils.isNotBlank(getName()) ? getName() : getId());

		// start bundles
		for (final String bundleName : requiredBundleNames) {
			try {
				startBundle(bundleName);
			} catch (final Exception e) {
				throw new ActivationException(NLS.bind("Error starting bundle \"{0}\": {1}", bundleName, e.getMessage()), e);
			}
		}

		// start applications
		for (final String applicationId : requiredApplicationIds) {
			try {
				startApplication(applicationId);
			} catch (final Exception e) {
				throw new ActivationException(NLS.bind("Error starting application \"{0}\": {1}", applicationId, e.getMessage()), e);
			}
		}

		if (BootDebug.roles) {
			LOG.debug("Activating server role {}...", getId());
		}
	}

	/**
	 * Deactivates the server role.
	 */
	void deactivate() {
		if (!active.compareAndSet(true, false)) {
			if (BootDebug.roles) {
				LOG.debug("Server role {} already inactive!", getId());
			}
			return;
		}

		// log a message
		LOG.info("Deactivating {}.", StringUtils.isNotBlank(getName()) ? getName() : getId());

		// stop applications in reverse order
		final List<String> launchedAppIds = new ArrayList<String>(launchedApps.keySet());
		Collections.reverse(launchedAppIds);
		for (final String applicationId : launchedAppIds) {
			stopApplication(applicationId);
		}

		if (BootDebug.roles) {
			LOG.debug("Deactivated server role {}...", getId());
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
		if (BootDebug.roles) {
			LOG.debug("Starting application {}", applicationId);
		}
		// FIXME: we need to check for running instances before we launch
		final ApplicationDescriptor applicationDescriptor = BootActivator.getInstance().getEclipseApplication(applicationId);
		if (applicationDescriptor == null)
			throw new IllegalStateException(NLS.bind("Application {0} not found!", applicationId));
		final ApplicationHandle handle = applicationDescriptor.launch(null);
		launchedApps.put(applicationId, handle);

		// wait for application to start
		long timeout = 2000l;
		String state = null;
		do {
			state = handle.getState();
			if (!StringUtils.equals(state, ApplicationHandle.RUNNING)) {
				try {
					timeout -= 150;
					Thread.sleep(150);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		} while ((timeout > 0) && !StringUtils.equals(state, ApplicationHandle.RUNNING));

		// log warning if it didn't start
		if (!StringUtils.equals(state, ApplicationHandle.RUNNING)) {
			LOG.warn("Application {} did not reach RUNNING state within timely manner (state ist {}). Server role {} might by dysfunctional!", new Object[] { applicationId, state, getId() });
		} else if (BootDebug.roles) {
			LOG.debug("Application {} started.", applicationId);
		}
	}

	/**
	 * Starts a bundle transient so that it is not restarted automatically on
	 * the next start.
	 * 
	 * @param symbolicName
	 * @throws BundleException
	 */
	private void startBundle(final String symbolicName) throws BundleException, IllegalStateException {
		if (BootDebug.roles) {
			LOG.debug("Starting bundle {}", symbolicName);
		}
		final Bundle bundle = BootActivator.getInstance().getBundle(symbolicName);
		if (bundle == null) {
			LOG.warn("Bundle {} not avaiable. Server role {} might by dysfunctional!", symbolicName, getId());
			return;
		}
		final int originalState = bundle.getState();
		if ((originalState & Bundle.ACTIVE) != 0)
			return; // bundle is already active
		try {
			// attempt to activate the bundle
			bundle.start(Bundle.START_TRANSIENT);
		} catch (final BundleException e) {
			if ((bundle.getState() & Bundle.ACTIVE) != 0)
				// this can happen if the bundle was started by a different thread (the framework) already
				return;
			if (((originalState & Bundle.STARTING) != 0) && ((bundle.getState() & Bundle.STARTING) != 0))
				// this can happen if the bundle was in the process of being activated on this thread, just return
				return;
			throw e;
		}

	}

	/**
	 * @param applicationId
	 */
	private void stopApplication(final String applicationId) {
		if (BootDebug.roles) {
			LOG.debug("Stopping application {}", applicationId);
		}
		try {
			final ApplicationHandle handle = launchedApps.get(applicationId);
			if (null == handle) {
				LOG.warn("Application handle for application {} not found! Unable to stop application.", applicationId);
				return;
			}

			// shutdown the app
			try {
				handle.destroy();
			} catch (final IllegalStateException e) {
				// this is thrown when the service is not unregistered
				if (BootDebug.roles) {
					LOG.debug("Application {} not active.", applicationId);
				}
			}

			// wait for application to shutdown
			long timeout = 2000l;
			String state = null;
			try {
				do {
					state = handle.getState();
					if (BootDebug.roles) {
						LOG.debug("Application {} state: {}", applicationId, state);
					}
					if (StringUtils.equals(state, ApplicationHandle.STOPPING)) {
						try {
							timeout -= 150;
							Thread.sleep(150);
						} catch (final InterruptedException e) {
							Thread.currentThread().interrupt();
							break;
						}
					}
				} while ((timeout > 0) && StringUtils.equals(state, ApplicationHandle.STOPPING));
			} catch (final IllegalStateException e) {
				// this is thrown when the service has been unregistered
				if (BootDebug.roles) {
					LOG.debug("Application {} state {}: {}", new Object[] { applicationId, state, e.getMessage() });
				}
			}

			// log warning if it didn't stop
			if (StringUtils.equals(state, ApplicationHandle.STOPPING)) {
				LOG.warn("Application {} still in STOPPING state after waiting for ordered shutdown. Server role {} might not be shutdown cleanly!", new Object[] { applicationId, state, getId() });
			} else if (BootDebug.roles) {
				LOG.debug("Application {} stopped.", applicationId);
			}
		} catch (final Exception e) {
			LOG.warn("Error during shutdown of application {} while deactivating role {}. {}", new Object[] { applicationId, getId(), e.getMessage() }, e);
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ServerRole [id=").append(id).append(", name=").append(name).append(", active=").append(active).append("]");
		return builder.toString();
	}
}
