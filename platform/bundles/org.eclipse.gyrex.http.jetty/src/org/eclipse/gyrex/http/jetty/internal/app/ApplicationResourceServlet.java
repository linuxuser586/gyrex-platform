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

import java.net.MalformedURLException;

import org.eclipse.gyrex.server.Platform;

import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.Resource;

public class ApplicationResourceServlet extends DefaultServlet {

	static ServletHolder newHolder(final ApplicationContextHandler applicationContextHandler) {
		final ServletHolder defaultServlet = new ServletHolder(new ApplicationResourceServlet(applicationContextHandler));
		defaultServlet.setInitParameter("dirAllowed", String.valueOf(Platform.inDevelopmentMode()));
		defaultServlet.setInitParameter("maxCacheSize", "2000000");
		defaultServlet.setInitParameter("maxCachedFileSize", "254000");
		defaultServlet.setInitParameter("maxCachedFiles", "1000");
		defaultServlet.setInitParameter("useFileMappedBuffer", "true");
		return defaultServlet;
	}

	final ApplicationContextHandler applicationContextHandler;

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance.
	 */
	public ApplicationResourceServlet(final ApplicationContextHandler applicationContextHandler) {
		this.applicationContextHandler = applicationContextHandler;
	}

	@Override
	public Resource getResource(final String pathInContext) {
		try {
			return applicationContextHandler.getResource(pathInContext);
		} catch (final MalformedURLException e) {
			Log.ignore(e);
			return null;
		}
	}

}
