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

import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.ApplicationException;
import org.eclipse.gyrex.http.application.context.IApplicationContext;
import org.eclipse.gyrex.http.jetty.internal.HttpJettyDebug;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ScopedHandler;
import org.eclipse.jetty.util.log.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This handler delegates all requests to the application and handles requests
 * routed back from the application via
 * {@link IApplicationContext#handleRequest(HttpServletRequest, HttpServletResponse)}
 * .
 */
public class ApplicationDelegateHandler extends ScopedHandler {

	private static final Logger LOG = LoggerFactory.getLogger(ApplicationDelegateHandler.class);

	private final Application application;

	/**
	 * Creates a new instance.
	 */
	public ApplicationDelegateHandler(final Application application) {
		this.application = application;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jetty.server.handler.ScopedHandler#doHandle(java.lang.String, org.eclipse.jetty.server.Request, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public void doHandle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
		nextHandle(target, baseRequest, request, response);
	}

	@Override
	public void doScope(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {

		/*
		 * This scope implementation is different. The we delegate to
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
			if (HttpJettyDebug.handlers) {
				LOG.debug("routing request to application {}", application);
			}
			application.handleRequest(request, response);
		} catch (final IOException e) {
			if (Platform.inDebugMode()) {
				Log.warn("Caught IOException while processing request '{}': {}", new Object[] { request, e.getMessage(), e });
			}
			throw e;
		} catch (final ApplicationException e) {
			// handle ApplicationException
			if (e.getStatus() == HttpStatus.SERVICE_UNAVAILABLE_503) {
				// we convert it into UnavailableException
				if (Platform.inDebugMode()) {
					Log.warn("Caught ApplicationException while processing request '{}': {}", new Object[] { request, e.getMessage(), e });
					throw new UnavailableException(e.getMessage(), 5);
				} else {
					throw new UnavailableException(e.getMessage(), 60); // TODO make configurable
				}
			} else {
				if (Platform.inDebugMode()) {
					Log.warn("Caught ApplicationException while processing request '{}': {}", new Object[] { request, e.getMessage(), e });
					response.sendError(e.getStatus(), e.getMessage());
				} else {
					response.sendError(e.getStatus());
				}
			}
		} catch (final IllegalStateException e) {
			// IllegalStateException are typically used in Gyrex to indicate that something isn't ready
			// we convert it into UnavailableException to allow recovering on a dynamic platform
			if (Platform.inDebugMode()) {
				Log.warn("Caught IllegalStateException while processing request '" + request.toString() + ": " + e.getMessage(), e);
				throw new UnavailableException(e.getMessage(), 5);
			} else {
				throw new UnavailableException(e.getMessage(), 60); // TODO make configurable
			}
		} catch (final RuntimeException e) {
			if (Platform.inDebugMode()) {
				Log.warn("Caught RuntimeException while processing request '{}': {}", new Object[] { request, e.getMessage(), e });
			}
			throw e;
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
		final Request baseRequest = Request.getRequest(request);
		if (baseRequest == null) {
			throw new IllegalStateException("Please ensure that this method is called within the request thread!");
		}

		try {
			// calculate target based on current path info
			final String target = baseRequest.getPathInfo();
			if (HttpJettyDebug.handlers) {
				LOG.debug("got request back from application {}, continue processing with Jetty handler chain (using target {})", application, target);
			}
			nextScope(target, baseRequest, baseRequest, response);
		} catch (final ServletException e) {
			throw new ApplicationException(e);
		}
		return baseRequest.isHandled();
	}

	@Override
	public String toString() {
		final StringBuilder string = new StringBuilder();
		string.append(getClass().getSimpleName());
		string.append("[");
		string.append(application.getId());
		string.append("]");
		return string.toString();
	}
}
