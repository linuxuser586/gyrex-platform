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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.ApplicationException;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationInstance;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationManager;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationRegistration;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jetty.http.PathMap;
import org.eclipse.jetty.http.PathMap.Entry;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.Resource;

import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The real {@link Application} context.
 */
public class ApplicationContextHandler extends ServletContextHandler {

	public class Context extends ServletContextHandler.Context {
		@Override
		public ServletContext getContext(final String uripath) {
			// cross-application dispatching is not supported
			return null;
		}

		@Override
		public String getContextPath() {
			// TODO need to use a "default" url mount point
			throw new NotImplementedException();
		}

		@Override
		public String getMimeType(final String file) {
			// delegate to the application
			// the application may delegate back to ApplicationContextHandler via our JettyApplicationContext
			return application.getMimeType(file);
		}

		@Override
		public String getRealPath(final String path) {
			// not supported
			return null;
		}

		@Override
		public URL getResource(final String path) throws MalformedURLException {
			// delegate to the application
			// the application may delegate back to ApplicationContextHandler via our JettyApplicationContext
			return application.getResource(path);
		}

		@Override
		public InputStream getResourceAsStream(final String path) {
			// implementation inline to be independent from super
			try {
				// use #getResource(String) to obtain an URL to the resource
				final URL url = getResource(path);
				if (url == null) {
					return null;
				}
				// call URL#openStream() to open the stream
				return url.openStream();
			} catch (final Exception e) {
				Log.ignore(e);
				return null;
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jetty.server.handler.ContextHandler.Context#getResourcePaths(java.lang.String)
		 */
		@Override
		public Set getResourcePaths(final String path) {
			// delegate to the application
			// the application may delegate back to ApplicationContextHandler via our JettyApplicationContext
			return application.getResourcePaths(path);
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(ApplicationContextHandler.class);

	final String applicationId;
	final ApplicationManager applicationManager;
	final ApplicationServletHandler applicationServletHandler;
	final PathMap resourcesMap = new PathMap();

	Application application;
	ApplicationContext applicationContext;

	/**
	 * Creates a new instance.
	 * 
	 * @param applicationId
	 */
	public ApplicationContextHandler(final String applicationId, final ApplicationManager applicationManager) {
		this.applicationId = applicationId;
		this.applicationManager = applicationManager;
		applicationServletHandler = new ApplicationServletHandler(this);
	}

	/**
	 * Returns the applicationId.
	 * 
	 * @return the applicationId
	 */
	public String getApplicationId() {
		return applicationId;
	}

	@Override
	public Resource getResource(String path) throws MalformedURLException {
		// data structure which maps a request to a resource provider; first-best match wins
		// { path =>  resource provider holder }
		if (resourcesMap.isEmpty() || (null == path) || !path.startsWith(URIUtil.SLASH)) {
			return null;
		}

		path = URIUtil.canonicalPath(path);
		final Entry entry = resourcesMap.getMatch(path);
		if (null == entry) {
			return null;
		}
		final ResourceProviderHolder provider = (ResourceProviderHolder) entry.getValue();
		if (null == provider) {
			return null;
		}

		final String pathSpec = (String) entry.getKey();
		final String pathInfo = PathMap.pathInfo(pathSpec, path);
		final URL resourceUrl = provider.getResource(pathInfo);
		if (null == resourceUrl) {
			return null;
		}
		try {
			if (resourceUrl.toExternalForm().startsWith("bundleentry:")) {
				final URL fileURL = FileLocator.toFileURL(resourceUrl);
				if (null != fileURL) {
					return newResource(fileURL);
				}
			}
			return newResource(resourceUrl);
		} catch (final IOException e) {
			Log.ignore(e);
			return null;
		}
	}

	@Override
	protected boolean isProtectedTarget(String target) {
		while (target.startsWith("//")) {
			target = URIUtil.compactPath(target);
		}
		return StringUtil.startsWithIgnoreCase(target, "/web-inf") || StringUtil.startsWithIgnoreCase(target, "/meta-inf");
	}

	@Override
	protected ServletHandler newServletHandler() {
		throw new IllegalStateException("not allowed");
	}

	@Override
	protected void startContext() throws Exception {
		// create the application context
		applicationContext = new ApplicationContext(this);

		// get the application registration
		final ApplicationRegistration applicationRegistration = applicationManager.getApplicationRegistration(applicationId);
		if (null == applicationRegistration) {
			if (LOG.isErrorEnabled()) {
				LOG.error("Application \"{}\" could not be retreived from the registry!", applicationId);
			}
			throw new ApplicationException(HttpServletResponse.SC_NOT_FOUND, "No Application Found");
		}

		// get the application instance
		ApplicationInstance applicationInstance;
		try {
			applicationInstance = applicationRegistration.getApplication(applicationContext);
		} catch (final CoreException e) {
			// TODO: how do we need to log this?
			LOG.error("Error retreiving application \"{}\". {}", applicationId, e.getMessage());
			throw new ApplicationException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Error Retreiving Application");
		}

		// get the application object
		application = null != applicationInstance ? applicationInstance.getApplication() : null;
		if (null == application) {
			throw new ApplicationException(HttpServletResponse.SC_NOT_FOUND, "No Application Found");
		}

		// set our delegating servlet handler
		setServletHandler(applicationServletHandler);

		// support welcome files
		setWelcomeFiles(new String[] { "index.html" });

		// register the default servlet at / to handle resources last
		// note, in Jetty (JavaEE?) "/*" is matched before "/"; "/" really means the last match
//		final ServletHolder defaultServlet = new ServletHolder(new ApplicationResourceServlet(this));
//		defaultServlet.setInitParameter("dirAllowed", String.valueOf(PlatformConfiguration.isOperatingInDevelopmentMode()));
//		defaultServlet.setInitParameter("maxCacheSize", "2000000");
//		defaultServlet.setInitParameter("maxCachedFileSize", "254000");
//		defaultServlet.setInitParameter("maxCachedFiles", "1000");
//		defaultServlet.setInitParameter("useFileMappedBuffer", "true");
//		applicationServletHandler.addServletWithMapping(defaultServlet, "/");

		// initialize handlers
		super.startContext();
	}
}
