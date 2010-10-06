/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.gyrex.http.internal.application.gateway.IUrlRegistry;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationManager;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.util.log.Log;

/**
 * A URL registry which maintains Jetty contexts for registered URLs.
 */
public class UrlRegistry implements IUrlRegistry {

	private final JettyGateway jettyGateway;
	private final ApplicationManager applicationManager;

	private final ConcurrentMap<String, UrlToApplicationHandler> urlHandlerByUrl = new ConcurrentHashMap<String, UrlToApplicationHandler>(30);
	private final ConcurrentMap<String, Handler> applicationContextByAppId = new ConcurrentHashMap<String, Handler>();

	/**
	 * Creates a new instance.
	 * 
	 * @param applicationManager
	 * @param server
	 */
	public UrlRegistry(final JettyGateway jettyGateway, final ApplicationManager applicationManager) {
		this.jettyGateway = jettyGateway;
		this.applicationManager = applicationManager;
	}

	@Override
	public void applicationUnregistered(final String applicationId) {
		final List<String> urlsToRemove = new ArrayList<String>();
		for (final Entry<String, UrlToApplicationHandler> entry : urlHandlerByUrl.entrySet()) {
			if (entry.getValue().getApplicationId().equals(applicationId)) {
				urlsToRemove.add(entry.getKey());
			}
		}
		for (final String url : urlsToRemove) {
			try {
				unregister(url);
			} catch (final Exception e) {
				Log.ignore(e);
			}
		}

		final Handler appContextHandler = applicationContextByAppId.remove(applicationId);
		if (null != appContextHandler) {
			try {
				appContextHandler.stop();
				appContextHandler.destroy();
			} catch (final Exception e) {
				Log.ignore(e);
			}
		}
	}

	public Handler getHandler(final String applicationId) {
		Handler applicationContextHandler = applicationContextByAppId.get(applicationId);
		if (null == applicationContextHandler) {
			applicationContextByAppId.putIfAbsent(applicationId, jettyGateway.customize(new ApplicationContextHandler(applicationId, applicationManager)));
			applicationContextHandler = applicationContextByAppId.get(applicationId);
		}
		return applicationContextHandler;
	}

	@Override
	public String registerIfAbsent(final URL url, final String applicationId) {
		// create an application handler for the specified url
		final UrlToApplicationHandler applicationHandler = new UrlToApplicationHandler(applicationId, url);

		// try to "register" the created app handler
		final UrlToApplicationHandler existingAppHandler = urlHandlerByUrl.putIfAbsent(url.toExternalForm(), applicationHandler);
		if (null != existingAppHandler) {
			// already got an existing handler, abort
			return existingAppHandler.getApplicationId();
		}

		// activate the handler
		applicationHandler.configure(this);

		// register handler
		try {
			jettyGateway.addUrlHandler(applicationHandler);
		} catch (final Exception e) {
			// try immediate unregistering
			try {
				jettyGateway.removeUrlHandler(applicationHandler);
			} catch (final Exception e2) {
				// ignore
			}
			// throw exception
			throw new IllegalStateException("Error while binding application '" + applicationId + "' to url '" + url.toExternalForm() + "' with the underlying Jetty engine. " + e.getMessage(), e);
		}

		// no application previously registered using that url
		return null;
	}

	private String unregister(final String url) {
		final UrlToApplicationHandler handler = urlHandlerByUrl.remove(url);
		if (null == handler) {
			return null;
		}

		// unregister handler
		try {
			jettyGateway.removeUrlHandler(handler);
		} catch (final Exception e) {
			Log.warn("Error while unbinding url '" + url + "' from the underlying Jetty engine. There might be resources leaking. {}", e.getMessage());
		}

		// return app id
		return handler.getApplicationId();
	}

	@Override
	public String unregister(final URL url) {
		return unregister(url.toExternalForm());
	}

}
