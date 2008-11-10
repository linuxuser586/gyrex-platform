/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.cloudfree.http.internal.apps.dummy;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.eclipse.cloudfree.configuration.PlatformConfiguration;
import org.eclipse.cloudfree.http.internal.HttpServiceTracker;
import org.eclipse.core.runtime.IStatus;

/**
 * 
 */
public class DummyRequestHandler extends HttpServlet {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException, ServletException {
		// check if the 'homepage' is requested
		final String pathInfo = req.getPathInfo();
		if ((null != pathInfo) && !pathInfo.equals("/")) {
			// another page is requested, fail with not found
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Page Not Found");
			return;
		}

		// check the platform configuration for errors
		final IStatus status = PlatformConfiguration.getPlatformStatus();

		// send an error if the server configuration contains errors or warnings
		// note, cancellation will be handled as error
		switch (status.getSeverity()) {
			case IStatus.CANCEL:
			case IStatus.ERROR:
			case IStatus.WARNING:
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server Configuration Problem");
				return;

			default:
				break;
		}

		// send a default page
		req.getRequestDispatcher(HttpServiceTracker.ALIAS_RESOURCES.concat("/newsite.html")).forward(req, resp);
	}
}
