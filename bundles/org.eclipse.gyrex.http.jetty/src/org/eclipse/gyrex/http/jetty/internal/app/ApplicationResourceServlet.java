/*******************************************************************************
 * Copyright (c) 2010, 2012 AGETO Service GmbH and others.
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

import java.net.MalformedURLException;

import javax.servlet.ServletContext;

import org.eclipse.gyrex.server.Platform;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

/**
 * A servlet which serves {@link ApplicationHandler#getResource(String)
 * registered application resources}.
 */
public class ApplicationResourceServlet extends DefaultServlet {

	static ServletHolder newHolder(final ApplicationHandler applicationHandler) {
		final ServletHolder defaultServlet = new ServletHolder(new ApplicationResourceServlet(applicationHandler));
		if (Platform.inDevelopmentMode()) {
			defaultServlet.setInitParameter("dirAllowed", "true");
			defaultServlet.setInitParameter("useFileMappedBuffer", "false");
			defaultServlet.setInitParameter("maxCachedFiles", "0");
		} else {
			defaultServlet.setInitParameter("dirAllowed", "false");
			defaultServlet.setInitParameter("maxCacheSize", "2000000");
			defaultServlet.setInitParameter("maxCachedFileSize", "254000");
			defaultServlet.setInitParameter("maxCachedFiles", "1000");
			defaultServlet.setInitParameter("useFileMappedBuffer", "true");
		}
		return defaultServlet;
	}

	final ApplicationHandler applicationHandler;

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance.
	 */
	public ApplicationResourceServlet(final ApplicationHandler applicationHandler) {
		this.applicationHandler = applicationHandler;
	}

	@Override
	public Resource getResource(final String pathInContext) {
		try {
			return applicationHandler.getResource(pathInContext);
		} catch (final MalformedURLException e) {
			return null;
		}
	}

	@Override
	protected ContextHandler initContextHandler(final ServletContext servletContext) {
		return applicationHandler;
	}

}
