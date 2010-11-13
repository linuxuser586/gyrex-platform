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

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.gyrex.http.internal.application.gateway.IHttpGateway;
import org.eclipse.gyrex.http.internal.application.gateway.IUrlRegistry;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationManager;
import org.eclipse.gyrex.http.jetty.internal.HttpJettyDebug;
import org.eclipse.gyrex.http.jetty.internal.handlers.DefaultErrorHandler;
import org.eclipse.gyrex.http.jetty.internal.handlers.DefaultErrorHandlerResourcesHandler;
import org.eclipse.gyrex.http.jetty.internal.handlers.DefaultFaviconHandler;
import org.eclipse.gyrex.http.jetty.internal.handlers.DefaultHandler;

import org.eclipse.core.runtime.Path;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HandlerContainer;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.osgi.service.datalocation.Location;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jetty based HTTP gateway.
 */
public class JettyGateway implements IHttpGateway {

	private static final Logger LOG = LoggerFactory.getLogger(JettyGateway.class);

	private final ConcurrentMap<ApplicationManager, UrlRegistry> urlRegistryByManager = new ConcurrentHashMap<ApplicationManager, UrlRegistry>(1);
	private final Server server;
	private final ApplicationHandlerCollection appHandlerCollection;
	private final File logsBaseDirectory;

	/**
	 * Creates a new instance.
	 * 
	 * @param server
	 * @param instanceLocation
	 */
	public JettyGateway(final Server server, final Location instanceLocation) {
		this.server = server;

		logsBaseDirectory = new Path(instanceLocation.getURL().getFile()).append("logs").append("jetty").toFile();
		logsBaseDirectory.mkdirs();

		final HandlerCollection serverHandlers = new HandlerCollection();

		// handler for serving error page resource
		// (takes precedence over all other handlers for proper serving of error resources)
		serverHandlers.addHandler(new DefaultErrorHandlerResourcesHandler());

		// set server error handling
		server.addBean(new DefaultErrorHandler());

		// primary handler for applications
		appHandlerCollection = new ApplicationHandlerCollection(this);
		serverHandlers.addHandler(appHandlerCollection);

		// default favicon handler
		serverHandlers.addHandler(new DefaultFaviconHandler());

		// default handler for all other requests
		serverHandlers.addHandler(new DefaultHandler());

		server.setHandler(serverHandlers);
		server.setSendServerVersion(true);
		server.setSendDateHeader(true);
		server.setGracefulShutdown(1000);
	}

	/**
	 * Adds an application handler to the underlying Jetty server.
	 * 
	 * @param handler
	 * @throws Exception
	 *             if the handler could not be started
	 */
	public boolean addApplicationHandlerIfAbsent(final Handler handler) throws Exception {
		final boolean added = appHandlerCollection.addIfAbsent(handler);
		appHandlerCollection.mapUrls();
		if (HttpJettyDebug.handlers) {
			LOG.debug("{} URL handler {}", added ? "Added" : "Updated", handler);
			LOG.debug(server.dump());
		}
		return added;
	}

	/**
	 * Closes the gateway.
	 */
	public void close() {
		urlRegistryByManager.clear();
	}

	/**
	 * Customized the specified {@link ApplicationHandler} with configured
	 * specifics (eg. request logging).
	 * 
	 * @param applicationHandler
	 *            the handler to customized
	 * @return a handler which wraps the passed in {@link ApplicationHandler}
	 */
	public Handler customize(final ApplicationHandler applicationHandler) {
		final HandlerCollection applicationHandlers = new HandlerCollection();

		// primary handler
		applicationHandlers.addHandler(applicationHandler);

		// request logging
		final File appLogDir = new File(logsBaseDirectory, applicationHandler.getApplicationId());
		appLogDir.mkdirs();
		final NCSARequestLog requestLog = new NCSARequestLog();
		requestLog.setFilename(new File(appLogDir, "yyyy_mm_dd.request.log").getAbsolutePath());
		requestLog.setFilenameDateFormat("yyyy_MM_dd");
		requestLog.setRetainDays(90);
		requestLog.setAppend(true);
		requestLog.setExtended(true);
		requestLog.setLogCookies(false);
		requestLog.setLogTimeZone("GMT");
		final RequestLogHandler logHandler = new RequestLogHandler();
		logHandler.setRequestLog(requestLog);
		applicationHandlers.addHandler(logHandler);

		return applicationHandlers;
	}

	/**
	 * Returns the {@link ApplicationHandler} from the previously
	 * {@link #customize(ApplicationHandler) customized} handler.
	 * 
	 * @param customizedHandler
	 *            the handler previously customized using
	 *            {@link #customize(ApplicationHandler)}
	 * @return the {@link ApplicationHandler}
	 */
	public ApplicationHandler getApplicationHandler(final Handler customizedHandler) {
		if (customizedHandler instanceof ApplicationHandler) {
			return (ApplicationHandler) customizedHandler;
		}
		if (customizedHandler instanceof HandlerContainer) {
			return (ApplicationHandler) ((HandlerContainer) customizedHandler).getChildHandlerByClass(ApplicationHandler.class);
		}
		throw new IllegalArgumentException("unsupported handler: " + customizedHandler);
	}

	@Override
	public String getName() {
		return "Jetty " + Server.getVersion();
	}

	/**
	 * Returns the server.
	 * 
	 * @return the server
	 */
	public Server getServer() {
		return server;
	}

	@Override
	public IUrlRegistry getUrlRegistry(final ApplicationManager applicationManager) {
		final UrlRegistry registry = urlRegistryByManager.get(applicationManager);
		if (null != registry) {
			return registry;
		}
		urlRegistryByManager.putIfAbsent(applicationManager, new UrlRegistry(this, applicationManager));
		return urlRegistryByManager.get(applicationManager);
	}

	/**
	 * Removes the handler from the underlying Jetty server.
	 * 
	 * @param appHandler
	 * @param force
	 *            <code>true</code> if the handler must be removed,
	 *            <code>false</code> if it should only be removed when it no
	 *            longer has urls
	 * @throws Exception
	 *             if the handler could not be stopped
	 */
	public boolean removeApplicationHandler(final Handler appHandler, final boolean force) throws Exception {
		// remove if forced or no urls
		boolean removed = false;
		if (force || !getApplicationHandler(appHandler).hasUrls()) {
			appHandlerCollection.removeHandler(appHandler);
			removed = true;
		}

		// remap URLs
		appHandlerCollection.mapUrls();

		if (HttpJettyDebug.handlers) {
			LOG.debug("{} URL handler {}", removed ? "Removed" : "Updated", appHandler);
			LOG.debug(server.dump());
		}

		return removed;
	}
}
