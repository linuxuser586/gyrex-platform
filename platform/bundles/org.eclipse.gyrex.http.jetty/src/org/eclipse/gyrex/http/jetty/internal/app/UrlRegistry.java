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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.gyrex.http.internal.application.gateway.IUrlRegistry;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationManager;

import org.eclipse.jetty.server.Handler;

/**
 * A URL registry which maintains Jetty contexts for registered URLs.
 */
public class UrlRegistry implements IUrlRegistry {

	private final JettyGateway jettyGateway;
	private final ApplicationManager applicationManager;

	private final ConcurrentMap<String, UrlToApplicationHandler> knownUrls = new ConcurrentHashMap<String, UrlToApplicationHandler>(30);
	private final ConcurrentMap<String, Handler> contextByAppId = new ConcurrentHashMap<String, Handler>();

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

	public Handler getHandler(final String applicationId) {
		Handler applicationContextHandler = contextByAppId.get(applicationId);
		if (null == applicationContextHandler) {
			contextByAppId.putIfAbsent(applicationId, jettyGateway.customize(new ApplicationContextHandler(applicationId, applicationManager)));
			applicationContextHandler = contextByAppId.get(applicationId);
		}
		return applicationContextHandler;
	}

	@Override
	public String registerIfAbsent(final URL url, final String applicationId) {
		// create an application handler for the specified url
		final UrlToApplicationHandler applicationHandler = new UrlToApplicationHandler(applicationId, url);

		// try to "register" the created app handler
		final UrlToApplicationHandler existingAppHandler = knownUrls.putIfAbsent(url.toExternalForm(), applicationHandler);
		if (null != existingAppHandler) {
			// already got an existing handler, abort
			return existingAppHandler.getApplicationId();
		}

		// activate the handler
		applicationHandler.configure(this);

		// register handler
		jettyGateway.addUrlHandler(applicationHandler);

		// no application previously registered using that url
		return null;
	}

	@Override
	public String unregister(final URL url) {
		final UrlToApplicationHandler handler = knownUrls.remove(url.toExternalForm());
		if (null == handler) {
			return null;
		}

		// unregister handler
		jettyGateway.removeUrlHandler(handler);

		// return app id
		return handler.getApplicationId();
	}

}
