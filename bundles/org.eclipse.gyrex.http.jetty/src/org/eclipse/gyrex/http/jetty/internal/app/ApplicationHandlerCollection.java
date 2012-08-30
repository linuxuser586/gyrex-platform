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

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.jetty.internal.JettyDebug;
import org.eclipse.gyrex.http.jetty.internal.JettyEngineApplication;
import org.eclipse.gyrex.monitoring.metrics.ThroughputMetric;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.jetty.continuation.ContinuationThrowable;
import org.eclipse.jetty.http.HttpException;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.http.PathMap.Entry;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.io.RuntimeIOException;
import org.eclipse.jetty.server.AsyncContinuation;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
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
	private final ApplicationHandlerCollectionMetrics metrics;

	/**
	 * Creates a new instance.
	 * 
	 * @param jettyGateway
	 */
	public ApplicationHandlerCollection(final JettyGateway jettyGateway) {
		this.jettyGateway = jettyGateway;
		metrics = new ApplicationHandlerCollectionMetrics();
		JettyEngineApplication.registerMetrics(metrics);
	}

	public boolean addIfAbsent(final Handler handler) {
		if (handlers.addIfAbsent(handler)) {
			handler.setServer(getServer());
			metrics.getApplicationsMetric().channelStarted(0);
			return true;
		}
		return false;
	}

	@Override
	public void destroy() {
		try {
			super.destroy();
		} finally {
			JettyEngineApplication.unregisterMetrics(metrics);
		}
	}

	private void doHandle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException { // check async requests
		final AsyncContinuation async = baseRequest.getAsyncContinuation();
		if (async.isAsync()) {
			final ContextHandler context = async.getContextHandler();
			if (context != null) {
				if (JettyDebug.handlers) {
					LOG.debug("Dispatching asynchronous request {} to context {}", baseRequest, context);
				}
				context.handle(target, baseRequest, request, response);
				return;
			}
		}

		// get map
		final UrlMap map = urlMap.get();
		if (map == null) {
			if (JettyDebug.handlers) {
				LOG.debug("null URL map, no handler matched!");
			}
			return;
		}

		// perform lookup
		final Entry entry = map.getMatch(request.getScheme(), request.getServerName(), request.getServerPort(), target);
		if (entry == null) {
			if (JettyDebug.handlers) {
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
			if (JettyDebug.handlers) {
				LOG.debug("found matching handler for {}: {}", request.getRequestURL(), handler);
				LOG.debug("adjusted context path for {} to {}", request.getRequestURL(), baseRequest.getContextPath());
			}

			// lazy start handler
			if (!handler.isStarted()) {
				if (JettyDebug.handlers) {
					LOG.debug("lazy start of handler {}", handler);
				}
				try {
					handler.start();
				} catch (final Exception e) {
					if (Platform.inDebugMode()) {
						LOG.debug("Exception starting handler {}. {}", new Object[] { handler, ExceptionUtils.getRootCauseMessage(e), e });
						throw new IllegalStateException(String.format("Failed to start registered handler '%s' for mapping '%s'. %s", handler, mapped, ExceptionUtils.getRootCauseMessage(e)), e);
					} else {
						LOG.warn("Exception starting handler {}. {}", new Object[] { handler, ExceptionUtils.getRootCauseMessage(e) });
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

	@Override
	protected void doStart() throws Exception {
		try {
			super.doStart();
			metrics.setStatus("started", "Handler has been started by Jetty");
		} catch (final Exception e) {
			// TODO: handle exception
		}
	}

	@Override
	protected void doStop() throws Exception {
		try {
			super.doStop();
			metrics.setStatus("stopped", "Handler has been stopped by Jetty");
		} catch (final Exception e) {
			metrics.error("doStop: " + e.getMessage(), e);
			throw e;
		}
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

		final ThroughputMetric requestsMetric = metrics.getRequestsMetric();
		final long requestStart = requestsMetric.requestStarted();
		try {
			doHandle(target, baseRequest, request, response);
			if (response instanceof Response) {
				final int status = ((Response) response).getStatus();
				if (HttpStatus.isServerError(status)) {
					metrics.getRequestsMetric().requestFailed();
					metrics.error(status, ((Response) response).getReason());
				} else {
					metrics.getRequestsMetric().requestFinished(((Response) response).getContentCount(), System.currentTimeMillis() - requestStart);
				}
			} else {
				metrics.getRequestsMetric().requestFinished(0, System.currentTimeMillis() - requestStart);
			}
		} catch (final EofException e) {
			metrics.getRequestsMetric().requestFailed();
			throw e;
		} catch (final RuntimeIOException e) {
			metrics.getRequestsMetric().requestFailed();
			throw e;
		} catch (final ContinuationThrowable e) {
			metrics.getRequestsMetric().requestFailed();
			throw e;
		} catch (final Exception e) {
			metrics.getRequestsMetric().requestFailed();

			final DispatcherType type = baseRequest.getDispatcherType();
			if (!(DispatcherType.REQUEST.equals(type) || DispatcherType.ASYNC.equals(type))) {
				if (e instanceof IOException) {
					throw (IOException) e;
				}
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				if (e instanceof ServletException) {
					throw (ServletException) e;
				}
			}

			// handle or log exception
			if (e instanceof HttpException) {
				throw (HttpException) e;
			} else if (e instanceof RuntimeIOException) {
				throw (RuntimeIOException) e;
			} else if (e instanceof EofException) {
				throw (EofException) e;
			} else if ((e instanceof IOException) || (e instanceof UnavailableException) || (e instanceof IllegalStateException)) {
				if (Platform.inDebugMode()) {
					LOG.debug("Exception processing request {}: {}", request.getRequestURI(), ExceptionUtils.getMessage(e));
					LOG.debug(request.toString());
				}
			} else {
				LOG.error("Exception processing request {}: {}", new Object[] { request.getRequestURI(), ExceptionUtils.getRootCauseMessage(e), e });
				if (Platform.inDebugMode()) {
					LOG.debug(request.toString());
				}
			}

			// send error response if possible
			if (!response.isCommitted()) {
				request.setAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE, e.getClass());
				request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, e);
				if (e instanceof UnavailableException) {
					final UnavailableException ue = (UnavailableException) e;
					if (ue.isPermanent()) {
						response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
					} else {
						response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage());
					}
				} else if (e instanceof IllegalStateException) {
					response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getMessage());
				} else {
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
				}
			} else {
				// give up
				if (JettyDebug.debug) {
					LOG.debug("Response already committed for handling {}", ExceptionUtils.getMessage(e));
				}
			}
		} catch (final Error e) {
			metrics.getRequestsMetric().requestFailed();

			// only handle some errors
			if (!((e instanceof LinkageError) || (e instanceof AssertionError))) {
				throw e;
			}

			final DispatcherType type = baseRequest.getDispatcherType();
			if (!(DispatcherType.REQUEST.equals(type) || DispatcherType.ASYNC.equals(type))) {
				throw e;
			}

			LOG.error("Error processing request {}: {}", new Object[] { request.getRequestURI(), ExceptionUtils.getRootCauseMessage(e), e });
			if (JettyDebug.debug) {
				LOG.debug(request.toString());
			}

			// TODO httpResponse.getHttpConnection().forceClose();
			if (!response.isCommitted()) {
				request.setAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE, e.getClass());
				request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, e);
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			} else {
				if (JettyDebug.debug) {
					LOG.debug("Response already committed for handling {}", ExceptionUtils.getMessage(e));
				}
			}
		}
	}

	/**
	 * Remap the URL.
	 */
	public void mapUrls() {
		if (JettyDebug.handlers) {
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
				if (JettyDebug.handlers) {
					LOG.debug("mapped url {} --> {}", url, handlers[i]);
				}
			}
		}

		this.urlMap.set(urlMap);
	}

	public boolean removeHandler(final Handler handler) throws Exception {
		if (handlers.remove(handler)) {
			metrics.getApplicationsMetric().channelFinished();
			if (handler.isStarted()) {
				handler.stop();
			}
			return true;
		}
		return false;
	}

}
