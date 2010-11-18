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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.context.IApplicationContext;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationInstance;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationRegistration;
import org.eclipse.gyrex.http.jetty.internal.HttpJettyDebug;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jetty.http.PathMap;
import org.eclipse.jetty.http.PathMap.Entry;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.DispatcherType;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.resource.Resource;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Application} handler.
 */
public class ApplicationHandler extends ServletContextHandler {

	/**
	 * {@link ServletContext} implementation used by {@link ApplicationHandler}.
	 */
	public class ApplicationServletContext extends Context {

		@Override
		public ServletContext getContext(final String uripath) {
			// cross-application context access not supported
			if (Platform.inDebugMode()) {
				LOG.warn("Illegal cross-application access attempt from {} to {}.", getApplicationId(), uripath);
			}
			return null;
		}

		@Override
		public String getContextPath() {
			final String contextPath = getCurrentContextPath();
			if (URIUtil.SLASH.equals(contextPath)) {
				return EMPTY_STRING;
			}
			return contextPath;
		}

		@Override
		public RequestDispatcher getNamedDispatcher(final String name) {
			// not supported
			return null;
		}

		@Override
		public String getRealPath(final String path) {
			// not supported
			return null;
		}

		@Override
		public Servlet getServlet(final String name) throws ServletException {
			// not supported
			return null;
		}
	}

	public static final String ATTRIBUTE_DEBUG_INFO = ApplicationHandler.class.getName().concat(".debugInfo");

	static final String EMPTY_STRING = "";

	private static final Logger LOG = LoggerFactory.getLogger(ApplicationHandler.class);

	private final ThreadLocal<String> currentContextPath = new ThreadLocal<String>();
	private final ApplicationRegistration applicationRegistration;
	private final CopyOnWriteArraySet<String> urls = new CopyOnWriteArraySet<String>();
	private final PathMap resourcesMap = new PathMap();
	private final boolean showDebugInfo = Platform.inDebugMode() || Platform.inDevelopmentMode();

	private ApplicationContext applicationContext;
	private Application application;
	private ApplicationDelegateHandler applicationDelegateHandler;
	private SessionHandler sessionHandler;

	/**
	 * Creates a new instance.
	 * 
	 * @param applicationId
	 */
	public ApplicationHandler(final ApplicationRegistration applicationRegistration) {
		super();

		// use "slash" as default context path
		setContextPath(URIUtil.SLASH);

		// initialize context
		_scontext = new ApplicationServletContext();

		// initialize application registration
		this.applicationRegistration = applicationRegistration;
		getInitParams().putAll(applicationRegistration.getInitProperties());

		// set display name
		setDisplayName(applicationRegistration.getProviderId() + "@" + applicationRegistration.getApplicationId() + "@" + applicationRegistration.getContext().getContextPath());
	}

	/**
	 * Adds a resource to the application.
	 * 
	 * @param pathSpec
	 * @param resourceProviderHolder
	 */
	public void addResource(final String pathSpec, final ResourceProviderHolder resourceProviderHolder) {
		resourcesMap.put(pathSpec, resourceProviderHolder);
	}

	public void addUrl(final String url) {
		urls.add(url);
	}

	@Override
	public void doScope(String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {

		/*
		 * IMPLEMENTATION NOTE
		 *
		 * This method is overridden to customize the path handling.
		 *
		 * Normally, a ContextHandler has a context path. However, the ApplicationContextHandler
		 * must inherit the context path from the calling ApplicationHandlerCollection because
		 * it changes depending on the incoming url.
		 *
		 * Additionally, class loader handling has been disabled in order to rely on the Equinox default.
		 */

		// note, we do not support requests going through different contexts
		final ContextHandler.Context origContext = baseRequest.getContext();
		if ((origContext != null) && (origContext != _scontext)) {
			throw new IllegalStateException("origContext != this context, nesting/cross-application routing not supported!");
		}

		// backup paths
		final String origContextPath = baseRequest.getContextPath();
		final String origServletPath = baseRequest.getServletPath();
		final String origPathInfo = baseRequest.getPathInfo();

		// if the current context is not set, we need to set and restore
		final boolean newContext = (origContext == null) || (currentContextPath.get() == null);

		// set 'current' context path if not already set
		if (newContext) {
			// note, we rely on ApplicationHandlerCollection to pre-set the
			// correct context path on the request
			currentContextPath.set(baseRequest.getContextPath());
		}
		try {
			final String contextPath = currentContextPath.get();
			String pathInfo = null;

			final DispatcherType dispatch = baseRequest.getDispatcherType();

			// Are we already in this context?
			if (newContext) {
				// check the target
				if (DispatcherType.REQUEST.equals(dispatch) || DispatcherType.ASYNC.equals(dispatch)) {

					// perform checkContext to support unavailable status
					if (!checkContext(target, baseRequest, response)) {
						return;
					}

					// only accept requests coming through ApplicationHandlerCollection
					if (contextPath == null) {
						return;
					}

					// calculate paths
					if (target.length() > contextPath.length()) {
						if (contextPath.length() > 1) {
							target = target.substring(contextPath.length());
						}
						pathInfo = target;
					} else if (contextPath.length() == 1) {
						target = URIUtil.SLASH;
						pathInfo = URIUtil.SLASH;
					} else {
						// redirect null path infos in order to have context request end with /
						baseRequest.setHandled(true);
						if (baseRequest.getQueryString() != null) {
							response.sendRedirect(URIUtil.addPaths(baseRequest.getRequestURI(), URIUtil.SLASH) + "?" + baseRequest.getQueryString());
						} else {
							response.sendRedirect(URIUtil.addPaths(baseRequest.getRequestURI(), URIUtil.SLASH));
						}
						return;
					}
				}
			}

			// update the paths
			baseRequest.setContext(_scontext);
			if (!DispatcherType.INCLUDE.equals(dispatch) && target.startsWith(URIUtil.SLASH)) {
				if (contextPath.length() == 1) {
					baseRequest.setContextPath(EMPTY_STRING);
				} else {
					baseRequest.setContextPath(contextPath);
				}
				baseRequest.setServletPath(null);
				baseRequest.setPathInfo(pathInfo);
			}

			// set application handler debug info reference
			if (showDebugInfo) {
				final StringBuilder dump = new StringBuilder();
				dump(dump);
				baseRequest.setAttribute(ATTRIBUTE_DEBUG_INFO, dump.toString());
			}

			// next scope
			// note, we don't inline here in order not to break when Jetty changes something
			nextScope(target, baseRequest, request, response);
		} finally {
			if (newContext) {
				// reset the context and servlet path
				baseRequest.setContext(origContext);
				baseRequest.setContextPath(origContextPath);
				baseRequest.setServletPath(origServletPath);
				baseRequest.setPathInfo(origPathInfo);
				currentContextPath.set(null);
			}
		}

	}

	@Override
	protected void dump(final Appendable out, final String indent) throws IOException {
		out.append(toString()).append(isStarted() ? " started" : " STOPPED").append('\n');
		for (final String url : urls) {
			out.append(indent).append(" +-").append(url).append('\n');
		}
		dumpHandlers(out, indent);
	}

	/**
	 * Returns the application
	 * 
	 * @return the application
	 */
	public Application getApplication() {
		final Application app = application;
		if (app == null) {
			throw new IllegalStateException("inactive");
		}
		return app;
	}

	/**
	 * Returns the application id.
	 * 
	 * @return the application id
	 */
	public String getApplicationId() {
		return applicationRegistration.getApplicationId();
	}

	/**
	 * Returns the application registration.
	 * 
	 * @return the application registration
	 */
	public ApplicationRegistration getApplicationRegistration() {
		return applicationRegistration;
	}

	@Override
	public String getContextPath() {
		return getCurrentContextPath();
	}

	/**
	 * Returns the current context path.
	 * <p>
	 * The path returned may vary depending on an active request. If no request
	 * is active, i.e. if called outside a request scope, <code>null</code> will
	 * be returned.
	 * </p>
	 * 
	 * @return the context path (maybe <code>null</code> if unable to determine)
	 */
	public String getCurrentContextPath() {
		return currentContextPath.get();
	}

	/**
	 * Returns the handler that is responsible for routing requests to
	 * {@link Application#handleRequest(HttpServletRequest, HttpServletResponse)}
	 * .
	 * 
	 * @return the application delegate handler
	 */
	public ApplicationDelegateHandler getDelegateHandler() {
		return applicationDelegateHandler;
	}

	/**
	 * Looks up and returns a resource for the specified path.
	 * 
	 * @param path
	 * @return the found resource (maybe <code>null</code> if non is registered
	 *         for the specified path)
	 * @throws MalformedURLException
	 */
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
			// resolve bundle/Eclipse URLs
			final URL fileURL = FileLocator.toFileURL(resourceUrl);
			return Resource.newResource(fileURL);
		} catch (final IOException e) {
			LOG.warn("Error resolving url {} to file based resource. {}", resourceUrl.toExternalForm(), ExceptionUtils.getMessage(e));
			return null;
		}
	}

	@Override
	public SecurityHandler getSecurityHandler() {
		// no security handler
		return null;
	}

	/**
	 * Returns the servlet handler.
	 * 
	 * @return the servlet handler
	 */
	@Override
	public ApplicationServletHandler getServletHandler() {
		// out servlet handler
		return (ApplicationServletHandler) _servletHandler;
	}

	@Override
	public SessionHandler getSessionHandler() {
		// our session handler
		return sessionHandler;
	}

	/**
	 * Returns all mount urls.
	 * 
	 * @return the urls
	 */
	public String[] getUrls() {
		return urls.toArray(new String[urls.size()]);
	}

	public boolean hasUrls() {
		return urls.size() > 0;
	}

	@Override
	protected boolean isProtectedTarget(String target) {
		while (target.startsWith("//")) {
			target = URIUtil.compactPath(target);
		}
		return StringUtil.startsWithIgnoreCase(target, "/web-inf") || StringUtil.startsWithIgnoreCase(target, "/meta-inf") || StringUtil.startsWithIgnoreCase(target, "/osgi-inf") || StringUtil.startsWithIgnoreCase(target, "/osgi-opt");
	}

	/**
	 * Removes a resource from the application.
	 * 
	 * @param pathSpec
	 */
	public void removeResource(final String pathSpec) {
		resourcesMap.remove(pathSpec);
	}

	public void removeUrl(final String url) {
		urls.remove(url);
	}

	@Override
	protected void startContext() throws Exception {
		if (HttpJettyDebug.handlers) {
			LOG.debug("Starting {}", this);
		}

		// create the application context
		applicationContext = new ApplicationContext(this);

		// create servlet handler early
		_servletHandler = new ApplicationServletHandler(this);

		// initialize the application instance now (after servlet handler is available)
		try {
			final ApplicationInstance applicationInstance = applicationRegistration.getApplication(applicationContext);
			if (applicationInstance == null) {
				throw new IllegalStateException("no application instance for " + applicationRegistration);
			}
			application = applicationInstance.getApplication();
			if (application == null) {
				throw new IllegalStateException("no application object returned from instance for " + applicationRegistration);
			}
			if (HttpJettyDebug.handlers) {
				LOG.debug("Application {} initialized", application);
			}
		} catch (final Exception e) {
			LOG.error("Error creating application \"{}\". {}", new Object[] { getApplicationId(), e.getMessage(), e });
			if (Platform.inDebugMode()) {
				throw new IllegalStateException("Application Not Available: " + e.getMessage());
			} else {
				throw new IllegalStateException("Application Not Available");
			}
		}

		// create remaining the handlers
		applicationDelegateHandler = new ApplicationDelegateHandler(application);
		sessionHandler = new SessionHandler(new HashSessionManager());

		// setup the handler chain after super initialization
		// session -> application -> registered servlets/resources
		setHandler(sessionHandler);
		sessionHandler.setHandler(applicationDelegateHandler);
		applicationDelegateHandler.setHandler(_servletHandler);

		// set important attributes
		setAttribute(IApplicationContext.SERVLET_CONTEXT_ATTRIBUTE_APPLICATION, application);
		setAttribute(IApplicationContext.SERVLET_CONTEXT_ATTRIBUTE_CONTEXT, application.getContext());

		// perform super initialization
		super.startContext();

		// support welcome files
		setWelcomeFiles(new String[] { "index.jsp", "index.html", "index.htm" });
	}

	@Override
	public String toString() {
		final StringBuilder toString = new StringBuilder();
		toString.append("ApplicationHandler@").append(Integer.toHexString(hashCode()));
		toString.append('(').append(applicationRegistration.getApplicationId()).append('[').append(applicationRegistration.getProviderId()).append('@').append(applicationRegistration.getContext().getContextPath()).append("])");
		return toString.toString();
	}
}
