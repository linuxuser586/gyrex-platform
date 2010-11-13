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
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.jetty.internal.HttpJettyDebug;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.jetty.http.PathMap.Entry;
import org.eclipse.jetty.server.AsyncContinuation;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandlerContainer;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.URIUtil;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optimized handler for mapping requests to {@link Application applications}.
 * <p>
 * The handling is optimized for the applications concept. This requires a
 * specific handler hierarchy. Thus, a {@link ApplicationHandlerCollection} only
 * supports {@link ApplicationHandler} as direct children.
 * </p>
 */
public class ApplicationHandlerCollection extends AbstractHandlerContainer {

	private static final Logger LOG = LoggerFactory.getLogger(ApplicationHandlerCollection.class);

	private final AtomicReference<UrlMap> urlMap = new AtomicReference<UrlMap>();

	/** all registered handlers */
	private final CopyOnWriteArrayList<Handler> handlers = new CopyOnWriteArrayList<Handler>();

	private final JettyGateway jettyGateway;

	/**
	 * Creates a new instance.
	 * 
	 * @param jettyGateway
	 */
	public ApplicationHandlerCollection(final JettyGateway jettyGateway) {
		this.jettyGateway = jettyGateway;
	}

	public boolean addIfAbsent(final Handler handler) {
		if (handlers.addIfAbsent(handler)) {
			handler.setServer(getServer());
			return true;
		}
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Object expandChildren(Object list, final Class<?> byClass) {
		final Handler[] handlers = getHandlers();
		for (int i = 0; (handlers != null) && (i < handlers.length); i++) {
			list = expandHandler(handlers[i], list, (Class<Handler>) byClass);
		}
		return list;
	}

	@Override
	public Handler[] getHandlers() {
		return handlers.toArray(new Handler[handlers.size()]);
	}

	@Override
	public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
		// don't do anything if already processed
		if (response.isCommitted() || baseRequest.isHandled()) {
			return;
		}

		final Iterator<Handler> handlers = this.handlers.iterator();
		if (!handlers.hasNext()) {
			return;
		}

		// check async requests
		final AsyncContinuation async = baseRequest.getAsyncContinuation();
		if (async.isAsync()) {
			final ContextHandler context = async.getContextHandler();
			if (context != null) {
				if (HttpJettyDebug.handlers) {
					LOG.debug("Dispatching asynchronous request {} to context {}", baseRequest, context);
				}
				context.handle(target, baseRequest, request, response);
				return;
			}
		}

		// get map
		final UrlMap map = urlMap.get();
		if (map == null) {
			if (HttpJettyDebug.handlers) {
				LOG.debug("null URL map, no handler matched!");
			}
			return;
		}

		// perform lookup
		final Entry entry = map.getMatch(request.getScheme(), request.getServerName(), request.getServerPort(), target);
		if (entry == null) {
			if (HttpJettyDebug.handlers) {
				LOG.debug("no matching handler for {}", request.getRequestURL());
			}
			return;
		}

		final String oldContextPath = baseRequest.getContextPath();
		try {
			// adjust context path and execute
			final String mapped = entry.getMapped();
			baseRequest.setContextPath((mapped != null) && (mapped.length() > 0) ? mapped : URIUtil.SLASH);

			// get handler
			final Handler handler = (Handler) entry.getValue();
			if (HttpJettyDebug.handlers) {
				LOG.debug("found matching handler for {}: {}", request.getRequestURL(), handler);
				LOG.debug("adjusted context path for {} to {}", request.getRequestURL(), baseRequest.getContextPath());
			}

			// lazy start handler
			if (!handler.isStarted()) {
				if (HttpJettyDebug.handlers) {
					LOG.debug("lazy start of handler {}", handler);
				}
				try {
					handler.start();
				} catch (final Exception e) {
					LOG.warn("Exception starting handler {}. {}", new Object[] { handler, ExceptionUtils.getMessage(e), e });
					if (Platform.inDebugMode()) {
						throw new IllegalStateException("Application Not Available: " + ExceptionUtils.getMessage(e), e);
					} else {
						throw new IllegalStateException("Application Not Available");
					}
				}
			}

			// handle
			handler.handle(target, baseRequest, request, response);
		} finally {
			baseRequest.setContextPath(oldContextPath);
		}
	}

	/**
	 * Remap the URL.
	 */
	public void mapUrls() {
		if (HttpJettyDebug.handlers) {
			LOG.debug("remapping urls {}", this);
		}
		final UrlMap urlMap = new UrlMap();
		final Handler[] handlers = getHandlers();
		for (int i = 0; (handlers != null) && (i < handlers.length); i++) {
			final ApplicationHandler handler = jettyGateway.getApplicationHandler(handlers[i]);
			final String[] urls = handler.getUrls();
			for (final String url : urls) {
				// note, we must but the customized handler for the url
				if (!urlMap.put(url, handlers[i])) {
					throw new IllegalStateException("conflict detected for url: " + url);
				}
				if (HttpJettyDebug.handlers) {
					LOG.debug("mapped url {} --> {}", url, handlers[i]);
				}
			}
		}

		this.urlMap.set(urlMap);
	}

	public boolean removeHandler(final Handler handler) throws Exception {
		if (handlers.remove(handler)) {
			if (handler.isStarted()) {
				handler.stop();
			}
			return true;
		}
		return false;
	}

}
