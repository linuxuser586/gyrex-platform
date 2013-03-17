/*******************************************************************************
 * Copyright (c) 2010, 2013 AGETO Service GmbH and others.
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

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.ApplicationException;
import org.eclipse.gyrex.http.application.context.IApplicationContext;
import org.eclipse.gyrex.http.jetty.internal.JettyDebug;
import org.eclipse.gyrex.monitoring.metrics.ThroughputMetric;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.ScopedHandler;
import org.eclipse.jetty.util.URIUtil;

import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * This handler delegates all requests to the application and handles requests
 * routed back from the application via
 * {@link IApplicationContext#handleRequest(HttpServletRequest, HttpServletResponse)}
 * .
 */
public class ApplicationDelegateHandler extends ScopedHandler {

	private static final String MDC_KEY_CONTEXT_PATH = "gyrex.contextPath";
	private static final String MDC_KEY_APPLICATION_ID = "gyrex.applicationId";

	private static final String MDC_KEY_REQUEST_REMOTE_HOST = "req.remoteHost"; // same as Logback
	private static final String MDC_KEY_REQUEST_USER_AGENT_MDC_KEY = "req.userAgent"; // same as Logback
	private static final String MDC_KEY_REQUEST_REQUEST_URI = "req.requestURI"; // same as Logback
	private static final String MDC_KEY_REQUEST_REQUEST_URL = "req.requestURL"; // same as Logback
	private static final String MDC_KEY_REQUEST_QUERY_STRING = "req.queryString"; // same as Logback
	private static final String MDC_KEY_REQUEST_X_FORWARDED_FOR = "req.xForwardedFor"; // same as Logback

	private static final Logger LOG = LoggerFactory.getLogger(ApplicationDelegateHandler.class);

	private final ApplicationHandler applicationHandler;
	private final ApplicationHandlerMetrics metrics;

	/**
	 * Creates a new instance.
	 */
	public ApplicationDelegateHandler(final ApplicationHandler applicationHandler) {
		this.applicationHandler = applicationHandler;
		metrics = applicationHandler.getMetrics();
	}

	private void clearMdc() {
		// clear application specific information
		MDC.remove(MDC_KEY_APPLICATION_ID);
		MDC.remove(MDC_KEY_CONTEXT_PATH);

		// clear general request information
		MDC.remove(MDC_KEY_REQUEST_REMOTE_HOST);
		MDC.remove(MDC_KEY_REQUEST_REQUEST_URI);
		MDC.remove(MDC_KEY_REQUEST_QUERY_STRING);
		MDC.remove(MDC_KEY_REQUEST_REQUEST_URL);
		MDC.remove(MDC_KEY_REQUEST_USER_AGENT_MDC_KEY);
		MDC.remove(MDC_KEY_REQUEST_X_FORWARDED_FOR);
	}

	@Override
	public void doHandle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
		// FIXME: wird zweimal aufgerufen...
		final ThroughputMetric requestsMetric = metrics.getRequestsMetric();
		final long requestStart = requestsMetric.requestStarted();
		try {
			nextHandle(target, baseRequest, request, response);
			if (response instanceof Response) {
				final int status = ((Response) response).getStatus();
				if (HttpStatus.isServerError(status)) {
					metrics.getRequestsMetric().requestFailed();
					metrics.error(status, ((Response) response).getReason());
				} else {
					metrics.getRequestsMetric().requestFinished(((Response) response).getContentCount(), System.nanoTime() - requestStart);
				}
			} else {
				metrics.getRequestsMetric().requestFinished(0, System.nanoTime() - requestStart);
			}
		} catch (final IOException e) {
			metrics.getRequestsMetric().requestFailed();
			throw e;
		} catch (final ServletException e) {
			metrics.getRequestsMetric().requestFailed();
			throw e;
		} catch (final RuntimeException e) {
			metrics.getRequestsMetric().requestFailed();
			throw e;
		} catch (final Error e) {
			metrics.getRequestsMetric().requestFailed();
			throw e;
		}
	}

	@Override
	public void doScope(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {

		/*
		 * This scope implementation is different. We delegate to
		 * the application immediately. The assumption is, the context,
		 *  path info and session has been properly set up by scoping
		 * from previous handlers (scopes).
		 *
		 * The next scope would be the servlet handler which handles
		 * servlets registered via IApplicationContext. However, the
		 * ServletHandler already modifies the scope by setting
		 * a servlet path and path info as of the found servlet.
		 * This is wrong when calling Application#handleRequest.
		 *
		 * It's completely up to the Application if the ServletHandler
		 * should be called or not. It does so via calling the
		 * IApplicationContext method. This is implemented in this class
		 * as well (handleApplicationRequest) and continues with the next
		 * scope.
		 */

		// delegated to the application
		// the application may delegate back to us via ApplicationContext
		try {
			final Application application = applicationHandler.getApplication();

			// setup MDC
			setupMdc(application, request);

			// check application status
			final IStatus status = application.getStatus();
			if (!status.isOK()) {
				// abort request processing
				final String message = StringUtils.defaultIfEmpty(status.getMessage(), "Application not ready.");
				// we convert it into UnavailableException
				if (Platform.inDebugMode()) {
					LOG.warn("Application '{}' returned a not-ok status: {}", new Object[] { application.getId(), status });
					throw new UnavailableException(message, 5);
				} else
					throw new UnavailableException(message, 30); // TODO make configurable
			}

			// route to application
			if (JettyDebug.handlers) {
				LOG.debug("routing request to application {}", application);
			}
			application.handleRequest(request, response);
		} catch (final IOException e) {
			if (Platform.inDebugMode()) {
				LOG.warn("Caught IOException while processing request '{}': {}", new Object[] { request, e.getMessage(), e });
			}
			throw e;
		} catch (final ApplicationException e) {
			// handle ApplicationException
			if (e.getStatus() == HttpStatus.SERVICE_UNAVAILABLE_503) {
				// we convert it into UnavailableException
				if (Platform.inDebugMode()) {
					LOG.warn("Caught ApplicationException while processing request '{}': {}", new Object[] { request, e.getMessage(), e });
					throw new UnavailableException(e.getMessage(), 5);
				} else
					throw new UnavailableException(e.getMessage(), 30); // TODO make configurable
			} else {
				if (Platform.inDebugMode()) {
					LOG.warn("Caught ApplicationException while processing request '{}': {}", new Object[] { request, e.getMessage(), e });
					response.sendError(e.getStatus(), e.getMessage());
				} else {
					response.sendError(e.getStatus());
				}
			}
		} catch (final IllegalStateException e) {
			// IllegalStateException are typically used in Gyrex to indicate that something isn't ready
			// we convert it into UnavailableException to allow recovering on a dynamic platform
			if (Platform.inDebugMode()) {
				LOG.warn("Caught IllegalStateException while processing request '{}': {}", new Object[] { request, e.getMessage(), e });
				throw new UnavailableException(e.getMessage(), 5);
			} else
				throw new UnavailableException(e.getMessage(), 30); // TODO make configurable
		} catch (final RuntimeException e) {
			if (Platform.inDebugMode()) {
				LOG.warn("Caught RuntimeException while processing request '{}': {}", new Object[] { request, e.getMessage(), e });
			}
			throw e;
		} finally {
			// clear the MDC
			clearMdc();
		}

		// mark the request handled (if this point is reached)
		baseRequest.setHandled(true);
	}

	/**
	 * Handles a request from the {@link Application} via
	 * {@link IApplicationContext#handleRequest(HttpServletRequest, HttpServletResponse)}
	 * .
	 * <p>
	 * This passes the request back into the handler chain.
	 * </p>
	 * 
	 * @param request
	 * @param response
	 * @return <code>true</code> if the request was handled, <code>false</code>
	 *         otherwise
	 * @throws IOException
	 */
	public boolean handleApplicationRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		if (!(request instanceof Request))
			throw new IllegalArgumentException("Please ensure that this method is called within the request thread with the original Jetty request and response objects!");
		final Request baseRequest = (Request) request;

		try {
			// calculate target based on current path info
			final String target = baseRequest.getPathInfo();
			if (JettyDebug.handlers) {
				LOG.debug("got request back from application {}, continue processing with Jetty handler chain (using target '{}')", applicationHandler.getApplication(), target);
			}
			// also make sure the path absolute is absolute (required by ServletHandler down the road)
			if ((null == target) || !target.startsWith(URIUtil.SLASH))
				// if not it might indicate a problem higher up the stack, thus, make sure to fail
				// otherwise we might unveil unwanted resources (eg. display directory for wrong folder)
				throw new ApplicationException(String.format("Unable to handle request. It seems the specified request is invalid (path info '%s'). At least an absolute path info is necessary in order to determine the request target within the registered application servlets and resources.", StringUtils.trimToEmpty(target)));
			nextScope(target, baseRequest, baseRequest, response);
		} catch (final ServletException e) {
			throw new ApplicationException(e);
		}
		return baseRequest.isHandled();
	}

	private void setupMdc(final Application application, final HttpServletRequest request) {
		// application specific information
		MDC.put(MDC_KEY_APPLICATION_ID, application.getId());
		MDC.put(MDC_KEY_CONTEXT_PATH, application.getContext().getContextPath().toString());

		// general request information
		MDC.put(MDC_KEY_REQUEST_REMOTE_HOST, request.getRemoteHost());
		MDC.put(MDC_KEY_REQUEST_REQUEST_URI, request.getRequestURI());
		final StringBuffer requestURL = request.getRequestURL();
		if (requestURL != null) {
			MDC.put(MDC_KEY_REQUEST_REQUEST_URL, requestURL.toString());
		}
		MDC.put(MDC_KEY_REQUEST_QUERY_STRING, request.getQueryString());
		MDC.put(MDC_KEY_REQUEST_USER_AGENT_MDC_KEY, request.getHeader("User-Agent"));
		MDC.put(MDC_KEY_REQUEST_X_FORWARDED_FOR, request.getHeader("X-Forwarded-For"));
	}

	@Override
	public String toString() {
		final StringBuilder string = new StringBuilder();
		string.append(getClass().getSimpleName());
		string.append("[");
		try {
			string.append(applicationHandler.getApplication().getId());
		} catch (final Exception e) {
			string.append(e.toString());
		}
		string.append("]");
		return string.toString();
	}
}
