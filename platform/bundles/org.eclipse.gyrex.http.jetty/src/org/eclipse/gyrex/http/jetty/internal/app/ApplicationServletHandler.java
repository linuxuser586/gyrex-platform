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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.gyrex.http.application.ApplicationException;
import org.eclipse.gyrex.http.application.context.IApplicationContext;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.LazyList;

public class ApplicationServletHandler extends ServletHandler {
	/** applicationContextHandler */
	final ApplicationContextHandler applicationContextHandler;

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
		request.setAttribute("__target", target);
		applicationContextHandler.application.handleRequest(request, response);

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
			final String target = (String) request.getAttribute("__target");
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