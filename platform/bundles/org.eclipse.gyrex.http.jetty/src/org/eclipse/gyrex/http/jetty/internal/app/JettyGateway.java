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

import org.eclipse.gyrex.configuration.PlatformConfiguration;
import org.eclipse.gyrex.http.internal.application.gateway.IHttpGateway;
import org.eclipse.gyrex.http.internal.application.gateway.IUrlRegistry;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationManager;
import org.eclipse.gyrex.http.jetty.internal.HttpJettyDebug;
import org.eclipse.gyrex.http.jetty.internal.handlers.DefaultErrorHandler;
import org.eclipse.gyrex.http.jetty.internal.handlers.DefaultErrorHandlerResourcesHandler;
import org.eclipse.gyrex.http.jetty.internal.handlers.DefaultFaviconHandler;

import org.eclipse.core.runtime.Path;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
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
	private final ContextHandlerCollection urlToApplicationHandlerCollection;
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

		// primary handler for UrlToApplicationHandlers
		urlToApplicationHandlerCollection = new ContextHandlerCollection();
		urlToApplicationHandlerCollection.setContextClass(UrlToApplicationHandler.class);
		serverHandlers.addHandler(urlToApplicationHandlerCollection);

		// default favicon handler
		serverHandlers.addHandler(new DefaultFaviconHandler());

		// default handler for all other requests
		final DefaultHandler defaultHandler = new DefaultHandler();
		defaultHandler.setShowContexts(PlatformConfiguration.isOperatingInDevelopmentMode());
		defaultHandler.setServeIcon(false);
		serverHandlers.addHandler(defaultHandler);

		server.setHandler(serverHandlers);
		server.setSendServerVersion(true);
		server.setSendDateHeader(true);
		server.setGracefulShutdown(1000);
	}

	/**
	 * Adds an URL handler to the underlying Jetty server.
	 * 
	 * @param urlToApplicationHandler
	 * @throws Exception
	 *             if the handler could not be started
	 */
	public void addUrlHandler(final UrlToApplicationHandler urlToApplicationHandler) throws Exception {
		urlToApplicationHandlerCollection.addHandler(urlToApplicationHandler);
		if (urlToApplicationHandlerCollection.isStarted() && !urlToApplicationHandler.isStarted() && !urlToApplicationHandler.isStarting()) {
			urlToApplicationHandler.start();
		}
		if (HttpJettyDebug.handlers) {
			LOG.debug("Added URL handler {}", urlToApplicationHandler.getDisplayName());
			LOG.debug(server.dump());
		}
	}

	/**
	 * Closes the gateway.
	 */
	public void close() {
		urlRegistryByManager.clear();
	}

	public Handler customize(final ApplicationContextHandler applicationContextHandler) {

		final HandlerCollection applicationHandlers = new HandlerCollection();

		// primary handler
		applicationHandlers.addHandler(applicationContextHandler);

		// request logging
		final File appLogDir = new File(logsBaseDirectory, applicationContextHandler.getApplicationId());
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
	 * Removes n URL handler from the underlying Jetty server.
	 * 
	 * @param urlToApplicationHandler
	 * @throws Exception
	 *             if the handler could not be stopped
	 */
	public void removeUrlHandler(final UrlToApplicationHandler urlToApplicationHandler) throws Exception {
		urlToApplicationHandlerCollection.removeHandler(urlToApplicationHandler);
		if (urlToApplicationHandlerCollection.isStarted() && !urlToApplicationHandler.isStopped() && !urlToApplicationHandler.isStopping()) {
			urlToApplicationHandler.stop();
		}
		if (HttpJettyDebug.handlers) {
			LOG.debug("Removed URL handler {}", urlToApplicationHandler.getDisplayName());
			LOG.debug(server.dump());
		}
	}

}
