/*******************************************************************************
 * Copyright (c) 2010, 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *     Mike Tschierschke - small enhancement
 *******************************************************************************/
package org.eclipse.gyrex.http.equinoxhttpservice.internal;

import java.net.MalformedURLException;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.http.application.manager.ApplicationRegistrationException;
import org.eclipse.gyrex.http.application.manager.IApplicationManager;
import org.eclipse.gyrex.http.application.manager.MountConflictException;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service component for activating the default HttpService application.
 */
public class HttpServiceAppComponent {

	private static final String DEFAULT_APP_ID = HttpServiceActivator.SYMBOLIC_NAME.concat(".application");

	private static final Logger LOG = LoggerFactory.getLogger(HttpServiceAppComponent.class);

	private volatile IRuntimeContextRegistry runtimeContextRegistry;
	private volatile IApplicationManager applicationManager;

	/**
	 * Activates the component and registers the default application providing
	 * the <code>HttpService</code>.
	 */
	public void activate() {
		// only register in development mode
		// otherwise it must be manually enabled
		if (!Platform.inDevelopmentMode() || Boolean.TRUE.toString().equals(System.getProperty("gyrex.http.equinoxhttpservice.default.disabled"))) {
			return;
		}

		// get the root context
		final IRuntimeContext context = getRuntimeContextRegistry().get(Path.ROOT);
		if (null == context) {
			LOG.warn("Unable to start default HttpService application; context {} not available", Path.ROOT.toString());
			return;
		}

		final IApplicationManager manager = getApplicationManager();

		try {
			// try registration
			manager.register(DEFAULT_APP_ID, HttpServiceAppProvider.ID, context, null);

			// install mounts
			try {
				manager.mount("http:/", DEFAULT_APP_ID);
				manager.mount("https:/", DEFAULT_APP_ID);
			} catch (final MountConflictException e) {
				LOG.debug("HttpService application {} will not be mounted again. {} ", DEFAULT_APP_ID, e.getMessage());
				return;
			} catch (final MalformedURLException e) {
				// should not happen, URLs are hard-coded at development time
				return;
			}

			// activate
			manager.activate(DEFAULT_APP_ID);
		} catch (final ApplicationRegistrationException e) {
			// already registered
			LOG.info("HttpService application with id {} already registered.", DEFAULT_APP_ID);
			return;
		}
	}

	/**
	 * Deactivates the component.
	 */
	public void deactivate() {
		// empty
	}

	/**
	 * Returns the applicationManager.
	 * 
	 * @return the applicationManager
	 */
	public IApplicationManager getApplicationManager() {
		final IApplicationManager manager = applicationManager;
		if (manager == null) {
			throw new IllegalStateException("application manager not available");
		}
		return manager;
	}

	/**
	 * Returns the runtimeContextRegistry.
	 * 
	 * @return the runtimeContextRegistry
	 */
	public IRuntimeContextRegistry getRuntimeContextRegistry() {
		final IRuntimeContextRegistry registry = runtimeContextRegistry;
		if (registry == null) {
			throw new IllegalStateException("context registry not available");
		}
		return registry;
	}

	/**
	 * Sets the applicationManager.
	 * 
	 * @param applicationManager
	 *            the applicationManager to set
	 */
	public void setApplicationManager(final IApplicationManager applicationManager) {
		this.applicationManager = applicationManager;
	}

	/**
	 * Sets the runtimeContextRegistry.
	 * 
	 * @param runtimeContextRegistry
	 *            the runtimeContextRegistry to set
	 */
	public void setRuntimeContextRegistry(final IRuntimeContextRegistry runtimeContextRegistry) {
		this.runtimeContextRegistry = runtimeContextRegistry;
	}

}
