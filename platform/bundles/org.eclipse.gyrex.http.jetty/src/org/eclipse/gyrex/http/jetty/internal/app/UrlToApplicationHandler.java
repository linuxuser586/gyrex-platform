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

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HotSwapHandler;

/**
 * A handler implementation which delegates to a lazy initialized
 * {@link ApplicationContextHandler}.
 */
public class UrlToApplicationHandler extends ContextHandler {

	private class ApplicationDelegator extends HotSwapHandler {
		/**
		 *
		 */
		private final class DummyHandler extends AbstractHandler {
			@Override
			public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
				throw new IllegalStateException("inactive");
			}
		}

		private final Lock handlerLock = new ReentrantLock();

		@Override
		protected void doStart() throws Exception {
			// can be removed once bug 322575 is fixed
			setHandler(new DummyHandler());
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
			if ((null == getHandler()) || (getHandler() instanceof DummyHandler)) {
				handlerLock.lock();
				try {
					if ((null == getHandler()) || (getHandler() instanceof DummyHandler)) {
						setHandler(urlRegistry.getHandler(getApplicationId()));
						if ((getServer() != null) && (getServer().isStarting() || getServer().isStarted())) {
							final Handler[] contextCollections = getServer().getChildHandlersByClass(ContextHandlerCollection.class);
							for (int h = 0; (contextCollections != null) && (h < contextCollections.length); h++) {
								((ContextHandlerCollection) contextCollections[h]).mapContexts();
							}
						}
					}
				} finally {
					handlerLock.unlock();
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
	}

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
