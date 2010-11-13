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
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.util.log.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This handler delegates all requests to the application and handles requests
 * routed back from the application via
 * {@link IApplicationContext#handleRequest(HttpServletRequest, HttpServletResponse)}
 * .
 */
public class ApplicationDelegateHandler extends HandlerWrapper {

	private static final Logger LOG = LoggerFactory.getLogger(ApplicationDelegateHandler.class);

	private final Application application;
	private final ThreadLocal<String> currentTarget = new ThreadLocal<String>();

	/**
	 * Creates a new instance.
	 */
	public ApplicationDelegateHandler(final Application application) {
		this.application = application;
	}

	@Override
	public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
		// delegated to the application
		// the application may delegate back to us via ApplicationContext
		currentTarget.set(target);
		try {
			if (HttpJettyDebug.handlers) {
				LOG.debug("routing request to application {}", application);
			}
			application.handleRequest(request, response);
		} catch (final ApplicationException e) {
			// handle ApplicationException
			if (e.getStatus() == HttpStatus.SERVICE_UNAVAILABLE_503) {
				// we convert it into UnavailableException
				if (Platform.inDebugMode()) {
					Log.warn("Caught ApplicationException while processing request '" + request.toString() + ": " + e.getMessage(), e);
					throw new UnavailableException(e.getMessage(), 5);
				} else {
					throw new UnavailableException(e.getMessage(), 60); // TODO make configurable
				}
			} else {
				if (Platform.inDebugMode()) {
					Log.warn("Caught ApplicationException while processing request '" + request.toString() + ": " + e.getMessage(), e);
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
		} finally {
			currentTarget.set(null);
		}

		// in any case, mark the request handled
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
		try {
			final String target = currentTarget.get();
			final Handler handler = getHandler();
			if (HttpJettyDebug.handlers) {
				LOG.debug("got request back from application {}, routing now to {}", application, handler);
			}
			handler.handle(null != target ? target : baseRequest.getPathInfo(), baseRequest, request, response);
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
