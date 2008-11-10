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
package org.eclipse.cloudfree.http.internal.application.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;


import org.eclipse.cloudfree.http.application.Application;
import org.eclipse.cloudfree.http.internal.application.manager.ApplicationConfiguration;

/**
 * A servlet context for a particular {@link Application}
 */
public class ApplicationServletContextAdapter implements ServletContext {

	private final ServletContext servletContext;
	private final Application application;
	private String contextPath;
	private final ApplicationConfiguration applicationConfiguration;

	/**
	 * Creates a new instance.
	 * 
	 * @param applicationHandlerServlet
	 *            .getServletContext()
	 * @param application
	 */
	public ApplicationServletContextAdapter(final Application application, final ApplicationConfiguration applicationConfiguration, final ServletContext servletContext) {
		this.applicationConfiguration = applicationConfiguration;
		this.servletContext = servletContext;
		this.application = application;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#getAttribute(java.lang.String)
	 */
	@Override
	public Object getAttribute(final String name) {
		return applicationConfiguration.getAttribute(name);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#getAttributeNames()
	 */
	@Override
	public Enumeration getAttributeNames() {
		return applicationConfiguration.getAttributeNames();
	}

	/**
	 * Returns <code>null</code> because cross-application dispatching is not
	 * supported.
	 * 
	 * @see javax.servlet.ServletContext#getContext(java.lang.String)
	 */
	@Override
	public ServletContext getContext(final String uripath) {
		// cross-application dispatching is not supported
		return null;
	}

	/**
	 * Returns the default application mount point.
	 * <p>
	 * The path starts with a <code>"/"</code> character but does not end with a
	 * <code>"/"</code> character. If the application is mounted to the root,
	 * this method returns <code>""</code>.
	 * </p>
	 * 
	 * @see javax.servlet.ServletContext#getContextPath()
	 */
	@Override
	public String getContextPath() {
		// our context path is always the mount point of an application
		if (null == contextPath) {
			// use the default mount point
			contextPath = applicationConfiguration.getDefaultMountPoint().getPath();

			// make sure the path is absolute
			if ((contextPath.length() > 0) && (contextPath.charAt(0) != '/')) {
				contextPath = "/" + contextPath;
			}

			// make sure the path doesn't end with a slash
			if ((contextPath.length() > 1) && (contextPath.charAt(contextPath.length() - 1) == '/')) {
				contextPath = contextPath.substring(0, contextPath.length() - 1);
			}

			// use an empty string for the root path
			if (contextPath.length() == 1) {
				contextPath = "";
			}

		}
		return contextPath;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#getInitParameter(java.lang.String)
	 */
	@Override
	public String getInitParameter(final String name) {
		return applicationConfiguration.getInitParameter(name);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#getInitParameterNames()
	 */
	@Override
	public Enumeration getInitParameterNames() {
		return applicationConfiguration.getInitParameterNames();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#getMajorVersion()
	 */
	@Override
	public int getMajorVersion() {
		return servletContext.getMajorVersion();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#getMimeType(java.lang.String)
	 */
	@Override
	public String getMimeType(final String file) {
		final String mimeType = application.getMimeType(file);
		return (mimeType != null) ? mimeType : servletContext.getMimeType(file);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#getMinorVersion()
	 */
	@Override
	public int getMinorVersion() {
		return servletContext.getMinorVersion();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#getNamedDispatcher(java.lang.String)
	 */
	@Override
	public RequestDispatcher getNamedDispatcher(final String name) {
		// TODO we might want to support this
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#getRealPath(java.lang.String)
	 */
	@Override
	public String getRealPath(final String path) {
		// not supported
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#getRequestDispatcher(java.lang.String)
	 */
	@Override
	public RequestDispatcher getRequestDispatcher(String uri) {
		if ((null == uri) || (uri.length() == 0)) {
			return null;
		}

		if (uri.charAt(0) != '/') {
			return null;
		}

		// extract query string from uri
		String queryString = null;
		int qPos = -1;
		if ((qPos = uri.indexOf('?')) > -1) {
			queryString = uri.substring(qPos + 1);
			uri = uri.substring(0, qPos);
		}

		// remove ";jsessionid"
		if ((qPos = uri.indexOf(';')) > 0) {
			uri = uri.substring(0, qPos);
		}

		// ask for the containers request dispatcher to the application servlet
		final RequestDispatcher dispatcher = servletContext.getRequestDispatcher(null == queryString ? "/" : "/?".concat(queryString));
		if (null == dispatcher) {
			return null;
		}

		// return adapted dispatcher
		return new ApplicationRequestDispatcherAdapter(dispatcher, uri, queryString);
	}

	/**
	 * Calls {@link Application#getResource(String)}
	 * 
	 * @see javax.servlet.ServletContext#getResource(java.lang.String)
	 */
	@Override
	public URL getResource(final String path) throws MalformedURLException {
		return application.getResource(path);
	}

	/**
	 * Uses {@link #getResource(String)} to obtain an URL to the resource and
	 * calls {@link URL#openStream()}.
	 * 
	 * @see javax.servlet.ServletContext#getResourceAsStream(java.lang.String)
	 */
	@Override
	public InputStream getResourceAsStream(final String path) {
		try {
			final URL url = getResource(path);
			if (url != null) {
				return url.openStream();
			}
		} catch (final IOException e) {
			log("Error opening stream for resource '" + path + "'", e);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#getResourcePaths(java.lang.String)
	 */
	@Override
	public Set getResourcePaths(final String path) {
		return application.getResourcePaths(path);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#getServerInfo()
	 */
	@Override
	public String getServerInfo() {
		return "CloudFree";
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#getServlet(java.lang.String)
	 */
	@Override
	@Deprecated
	public Servlet getServlet(final String name) throws ServletException {
		// deprecated, always null
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#getServletContextName()
	 */
	@Override
	public String getServletContextName() {
		// we don't have a display-name element
		// TODO we might be able to generate a name based on the application, provider + context
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#getServletNames()
	 */
	@Override
	@Deprecated
	public Enumeration getServletNames() {
		// deprecated, always empty
		return Collections.enumeration(Collections.emptyList());
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#getServlets()
	 */
	@Override
	@Deprecated
	public Enumeration getServlets() {
		// deprecated, always empty
		return Collections.enumeration(Collections.emptyList());
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#log(java.lang.Exception, java.lang.String)
	 */
	@Override
	@Deprecated
	public void log(final Exception exception, final String msg) {
		log(msg, exception);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#log(java.lang.String)
	 */
	@Override
	public void log(final String msg) {
		log(msg, null);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#log(java.lang.String, java.lang.Throwable)
	 */
	@Override
	public void log(final String message, final Throwable throwable) {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#removeAttribute(java.lang.String)
	 */
	@Override
	public void removeAttribute(final String name) {
		applicationConfiguration.removeAttribte(name);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#setAttribute(java.lang.String, java.lang.Object)
	 */
	@Override
	public void setAttribute(final String name, final Object object) {
		applicationConfiguration.setAttribute(name, object);
	}

}
