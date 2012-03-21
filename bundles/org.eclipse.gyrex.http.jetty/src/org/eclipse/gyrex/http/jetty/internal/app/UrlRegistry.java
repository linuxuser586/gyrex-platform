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
 *******************************************************************************/
package org.eclipse.gyrex.http.jetty.internal.app;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.gyrex.http.internal.application.gateway.HttpGatewayBinding;
import org.eclipse.gyrex.http.internal.application.gateway.IUrlRegistry;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationRegistration;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationRegistration.DestroyListener;
import org.eclipse.gyrex.http.jetty.internal.JettyDebug;

import org.eclipse.jetty.server.Handler;
import org.eclipse.osgi.util.NLS;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A URL registry which maintains Jetty contexts for registered URLs.
 */
public class UrlRegistry implements IUrlRegistry {

	private static final Logger LOG = LoggerFactory.getLogger(UrlRegistry.class);

	private final JettyGateway jettyGateway;
	private final HttpGatewayBinding applicationManager;

	private final ConcurrentMap<String, String> applicationIdsByUrl = new ConcurrentHashMap<String, String>(30);
	private final ConcurrentMap<String, Handler> appHandlerByAppId = new ConcurrentHashMap<String, Handler>();

	/**
	 * Creates a new instance.
	 * 
	 * @param applicationManager
	 * @param server
	 */
	public UrlRegistry(final JettyGateway jettyGateway, final HttpGatewayBinding applicationManager) {
		this.jettyGateway = jettyGateway;
		this.applicationManager = applicationManager;
	}

	@Override
	public void applicationUnregistered(final String applicationId) {
		if (JettyDebug.debug) {
			LOG.debug("application unregistered: {}", applicationId);
		}

		// remove all urls
		for (final Entry<String, String> entry : applicationIdsByUrl.entrySet()) {
			if (entry.getValue().equals(applicationId)) {
				applicationIdsByUrl.remove(entry.getKey());
				if (JettyDebug.debug) {
					LOG.debug("removed url: {}", entry.getKey());
				}
			}
		}

		// get handler
		final Handler handler = appHandlerByAppId.remove(applicationId);
		if (handler == null) {
			return;
		}

		// remove from Jetty
		try {
			jettyGateway.removeApplicationHandler(handler, true);
		} catch (final Exception e) {
			LOG.warn("Error while removing app '{}' from the underlying Jetty engine. There might be resources leaking. {}", applicationId, e.getMessage());
		}

		// destroy
		handler.destroy();
	}

	private Handler ensureHandler(final String applicationId) {
		Handler applicationContextHandler = appHandlerByAppId.get(applicationId);
		if (applicationContextHandler == null) {
			// get the application registration
			final ApplicationRegistration applicationRegistration = applicationManager.getApplicationRegistration(applicationId);
			if (null == applicationRegistration) {
				throw new IllegalStateException(NLS.bind("Application \"{0}\" could not be retreived from the registry!", applicationId));
			}
			if (null == appHandlerByAppId.putIfAbsent(applicationId, jettyGateway.customize(new ApplicationHandler(applicationRegistration)))) {
				applicationRegistration.addDestroyListener(new DestroyListener() {
					@Override
					public void applicationDestroyed(final ApplicationRegistration registration) {
						// handle as application unregistered
						applicationUnregistered(applicationRegistration.getApplicationId());
					}
				});
			}
			applicationContextHandler = appHandlerByAppId.get(applicationId);
		}
		return applicationContextHandler;
	}

	@Override
	public String registerIfAbsent(final String url, final String applicationId) {
		// try to "register" the url
		final String existingRegistration = applicationIdsByUrl.putIfAbsent(url, applicationId);
		if (null != existingRegistration) {
			// already got an existing handler, abort
			return existingRegistration;
		}

		// create application handle
		final Handler handler = ensureHandler(applicationId);

		// add url to handler
		jettyGateway.getApplicationHandler(handler).addUrl(url);

		try {
			// register handler
			jettyGateway.addApplicationHandlerIfAbsent(handler);
		} catch (final Exception e) {
			// try immediate unregistering
			try {
				jettyGateway.removeApplicationHandler(handler, true);
			} catch (final Exception e2) {
				// ignore
			}
			// throw exception
			throw new IllegalStateException("Error while binding application '" + applicationId + "' to url '" + url + "' with the underlying Jetty engine. " + e.getMessage(), e);
		}

		// no application previously registered using that url
		return null;
	}

	@Override
	public String unregister(final String url) {
		final String appId = applicationIdsByUrl.remove(url);
		if (null == appId) {
			return null;
		}

		// get handler
		Handler handler = ensureHandler(appId);

		// remove url from handler
		jettyGateway.getApplicationHandler(handler).removeUrl(url);

		// remove from Jetty if possible
		try {
			if (jettyGateway.removeApplicationHandler(handler, false)) {
				// remove from internal map
				handler = appHandlerByAppId.remove(appId);
				// destroy
				handler.destroy();
			}
		} catch (final Exception e) {
			LOG.warn("Error while unbinding url '{}' from the underlying Jetty engine. There might be resources leaking. {}", new Object[] { url, ExceptionUtils.getRootCauseMessage(e), e });
		}

		// return app id
		return appId;
	}

}
