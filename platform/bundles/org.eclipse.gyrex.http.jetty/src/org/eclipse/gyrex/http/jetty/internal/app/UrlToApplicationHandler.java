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

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.gyrex.http.jetty.internal.HttpJettyDebug;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HotSwapHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A handler implementation which delegates to a lazy initialized
 * {@link ApplicationContextHandler}.
 */
public class UrlToApplicationHandler extends ContextHandler {

	private class ApplicationDelegator extends HotSwapHandler {
		private final Handler NOT_INITIALIZED = new AbstractHandler() {
			@Override
			public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
				throw new IllegalStateException("inactive");
			}

			@Override
			public String toString() {
				return "NOT_INITIALIZED_HOT_SWAP_HANDLER (workaround for Jetty bug 322575)";
			};
		};

		private final Lock handlerLock = new ReentrantLock();

		@Override
		protected void doStart() throws Exception {
			// can be removed once bug 322575 is fixed
			setHandler(NOT_INITIALIZED);
			super.doStart();
		}

		@Override
		public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
			// ignore requests received on different port
			if ((port > 0) && (request.getServerPort() != port)) {
				return;
			}

			// ignore requests received on different protocol
			if ((null != protocol) && !protocol.equals(request.getScheme())) {
				return;
			}

			// initialize application context if necessary
			if (!isInitialized()) {
				handlerLock.lock();
				boolean initialized = false;
				try {
					if (!isInitialized()) {
						setHandler(urlRegistry.getHandler(getApplicationId()));
						if ((getServer() != null) && (getServer().isStarting() || getServer().isStarted())) {
							final Handler[] contextCollections = getServer().getChildHandlersByClass(ContextHandlerCollection.class);
							for (int h = 0; (contextCollections != null) && (h < contextCollections.length); h++) {
								((ContextHandlerCollection) contextCollections[h]).mapContexts();
							}
						}
						initialized = true;
					}
				} finally {
					handlerLock.unlock();
				}
				if (HttpJettyDebug.handlers && initialized) {
					LOG.debug("Initialized application handler {}", getApplicationId());
					final Server server = getServer();
					if (null != server) {
						LOG.debug(server.dump());
					}
				}
			}

			// handle
			super.handle(target, baseRequest, request, response);

			// sanity check
			if (!baseRequest.isHandled() && !response.isCommitted()) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Handled");
				baseRequest.setHandled(true);
			}
		}

		private boolean isInitialized() {
			return (getHandler() != null) && (getHandler() != NOT_INITIALIZED);
		}

		@Override
		public String toString() {
			final StringBuilder b = new StringBuilder();
			b.append("ApplicationDelegator [");
			if (isInitialized()) {
				b.append("ACTIVE");
			} else {
				b.append("INACTIVE");
			}
			b.append("]");
			return b.toString();
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(UrlToApplicationHandler.class);

	final String applicationId;

	final URL url;
	String protocol;

	int port;
	ApplicationDelegator delegator;
	UrlRegistry urlRegistry;

	/**
	 * Creates a new instance.
	 * 
	 * @param applicationId
	 * @param url
	 */
	public UrlToApplicationHandler(final String applicationId, final URL url) {
		this.applicationId = applicationId;
		this.url = url;

		setDisplayName(url.toExternalForm() + " --> " + applicationId);
	}

	/**
	 * Configures the handler.
	 */
	public void configure(final UrlRegistry urlRegistry) {
		// remember context registry
		this.urlRegistry = urlRegistry;

		// virtual hosts
		final String domain = UrlUtil.getDomain(url);
		if ((null != domain) && (domain.length() > 0)) {
			if (isEligibleForWildcardSubDomains(domain)) {
				// register domain as well as wild-card sub domains
				setVirtualHosts(new String[] { domain, "*.".concat(domain) });
			} else {
				// register just domain
				setVirtualHosts(new String[] { domain });
			}
		}

		// remember protocol & port
		protocol = UrlUtil.getProtocol(url);
		port = UrlUtil.getPort(url);

		// context path
		final String path = UrlUtil.getPath(url);
		setContextPath(path.length() == 0 ? "/" : path);

		// redirect "/context" to "/context/"
		setAllowNullPathInfo(false);

		// set delegating handler
		delegator = new ApplicationDelegator();
		setHandler(delegator);
	}

	/**
	 * Returns the application id
	 * 
	 * @return the application id
	 */
	public String getApplicationId() {
		return applicationId;
	}

	private boolean isEligibleForWildcardSubDomains(final String domain) {
		return !domain.startsWith("*.");
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("UrlToApplicationHandler [applicationId=").append(applicationId).append(", url=").append(url).append("]");
		return builder.toString();
	}

}
