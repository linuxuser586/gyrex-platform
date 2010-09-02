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

import org.eclipse.gyrex.configuration.PlatformConfiguration;
import org.eclipse.gyrex.http.application.ApplicationException;
import org.eclipse.gyrex.http.application.context.IApplicationContext;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.LazyList;
import org.eclipse.jetty.util.log.Log;

public class ApplicationServletHandler extends ServletHandler {
	/** applicationContextHandler */
	final ApplicationContextHandler applicationContextHandler;
	final ThreadLocal<String> currentTarget = new ThreadLocal<String>();

	/**
	 * Creates a new instance.
	 * 
	 * @param applicationContextHandler
	 */
	public ApplicationServletHandler(final ApplicationContextHandler applicationContextHandler) {
		this.applicationContextHandler = applicationContextHandler;
	}

	@Override
	public void doScope(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
		// delegated to the application
		// the application may delegate back to us via ApplicationContext
		currentTarget.set(target);
		try {
			applicationContextHandler.application.handleRequest(request, response);
		} catch (final IllegalStateException e) {
			// IllegalStateException are typically used in Gyrex to indicate that something isn't ready
			// we convert it into UnavailableException to allow recovering on a dynamic platform
			if (PlatformConfiguration.isOperatingInDevelopmentMode()) {
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
	 * Called by the application logic to handle a request using registered
	 * servlets, resources, etc.
	 * <p>
	 * Implements
	 * {@link IApplicationContext#handleRequest(HttpServletRequest, HttpServletResponse)}
	 * </p>
	 */
	public boolean handleDelegatedRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ApplicationException {
		final Request baseRequest = Request.getRequest(request);
		try {
			final String target = currentTarget.get();
			super.doScope(null != target ? target : baseRequest.getPathInfo(), baseRequest, request, response);
		} catch (final ServletException e) {
			throw new ApplicationException(e);
		}
		return baseRequest.isHandled();
	}

	@Override
	public ServletHolder newServletHolder() {
		return new ApplicationRegisteredServletHolder();
	}

	@Override
	public ServletHolder newServletHolder(final Class servlet) {
		return new ApplicationRegisteredServletHolder(servlet);
	}

	public void removeServlet(final ServletHolder holder) {
		setServlets((ServletHolder[]) LazyList.removeFromArray(getServlets(), holder));
	}
}