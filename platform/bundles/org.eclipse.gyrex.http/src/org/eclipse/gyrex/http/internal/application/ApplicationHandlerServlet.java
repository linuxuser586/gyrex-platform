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
package org.eclipse.gyrex.http.internal.application;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.gyrex.common.logging.LogAudience;
import org.eclipse.gyrex.common.logging.LogSource;
import org.eclipse.gyrex.configuration.PlatformConfiguration;
import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.ApplicationException;
import org.eclipse.gyrex.http.internal.HttpActivator;
import org.eclipse.gyrex.http.internal.HttpDebug;
import org.eclipse.gyrex.http.internal.application.helpers.ApplicationRequestAdapter;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationInstance;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationManager;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationMount;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main entry into the world of applications.
 * <p>
 * The application servlet handles all HTTP requests and calls the registered
 * applications. Typically, an instance of this class is registered with the
 * OSGi Http Service. It then takes control over the URI namespace below its
 * registration.
 * </p>
 * <p>
 * Under the covers, the application handler servlet maintains a registry of
 * application
 * </p>
 */
public class ApplicationHandlerServlet extends HttpServlet implements IApplicationServletConstants {

	private static final Logger LOG = LoggerFactory.getLogger(ApplicationHandlerServlet.class);

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	private final ApplicationManager applicationManager;

	/**
	 * Creates a new instance.
	 * 
	 * @param applicationManager
	 */
	public ApplicationHandlerServlet(final ApplicationManager applicationManager) {
		this.applicationManager = applicationManager;
	}

	/**
	 * Called by {@link #service(ServletRequest, ServletResponse)} to handle a
	 * HTTP request.
	 * 
	 * @param req
	 *            the HTTP servlet request
	 * @param res
	 *            the HTTP servlet response
	 * @exception IOException
	 *                if an input or output error occurs while the servlet is
	 *                handling the HTTP request
	 * @exception ServletException
	 *                if the HTTP request cannot be handled
	 * @see javax.servlet.Servlet#service(ServletRequest, ServletResponse)
	 */
	private void doService(final HttpServletRequest req, final HttpServletResponse res) throws IOException, ApplicationException {
		// get the requested URL
		final URL url = getRequestUrl(req);

		// get the requesting client address
		final String clientAddress = getClientAddress(req);
		final int clientPort = getClientPort(req);

		// determine if there is an application mounted for the url
		final ApplicationMount applicationMount = getApplicationMount(req, url);
		if (null == applicationMount) {
			if (LOG.isWarnEnabled()) {
				LOG.warn("No application mounted on URL \"{}\".", url);
			}
			throw new ApplicationException(HttpServletResponse.SC_NOT_FOUND, "No Mount Found");
		}

		// get the application registration
		final ApplicationRegistration applicationRegistration = getApplicationRegistration(req, applicationMount);
		if (null == applicationRegistration) {
			if (LOG.isErrorEnabled()) {
				LOG.error("Application mounted on URL \"{}\" could not be retreived from the registry!", url);
			}
			throw new ApplicationException(HttpServletResponse.SC_NOT_FOUND, "No Application Found");
		}

		// get the application instance
		ApplicationInstance applicationInstance;
		try {
			applicationInstance = applicationRegistration.getApplication(this);
		} catch (final CoreException e) {
			// TODO: how do we need to log this?
			HttpActivator.getInstance().getLog().log("error retreiving application for url" + req, e, (Object) null, LogAudience.ADMIN, LogSource.PLATFORM);
			throw new ApplicationException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Error Retreiving Application");
		}

		// get the application object
		final Application application = null != applicationInstance ? applicationInstance.getApplication() : null;
		if (null == application) {
			throw new ApplicationException(HttpServletResponse.SC_NOT_FOUND, "No Application Found");
		}

		// get the adapted servlet context
		final ServletContext adaptedServletContext = applicationInstance.getAdaptedServletContext();
		if (null == adaptedServletContext) {
			// application has been destroyed
			throw new ApplicationException(HttpServletResponse.SC_NOT_FOUND, "No Application Found");
		}

		// adapt the request
		final HttpServletRequest adaptedRequest = new ApplicationRequestAdapter(req, url, applicationRegistration, applicationMount, adaptedServletContext, clientAddress, clientPort);

		// check if the application can be called
		final IStatus appStatus = application.getStatus();
		if (!appStatus.isOK()) {
			throw new ApplicationException(HttpServletResponse.SC_SERVICE_UNAVAILABLE, appStatus.getMessage());
		}

		// call the application
		if (HttpDebug.handlerServlet) {
			LOG.debug(MessageFormat.format("[{0}] {1} -> {2} {3} {4}", adaptedRequest.getMethod(), url, application, adaptedRequest.getMethod(), null != adaptedRequest.getPathInfo() ? adaptedRequest.getPathInfo() : ""));
		}
		application.handleRequest(adaptedRequest, res);
	}

	/**
	 * Returns the applicationManager.
	 * 
	 * @return the applicationManager
	 */
	private ApplicationManager getApplicationManager() {
		return applicationManager;
	}

	private ApplicationMount getApplicationMount(final HttpServletRequest req, final URL url) throws ApplicationException {
		// use existing mount
		final ApplicationMount applicationMount = (ApplicationMount) req.getAttribute(INTERNAL_ATTR_APP_MOUNT);
		if (null != applicationMount) {
			return applicationMount;
		}

		// lookup a mount point
		return getApplicationManager().findApplicationMount(url);
	}

	private ApplicationRegistration getApplicationRegistration(final HttpServletRequest req, final ApplicationMount applicationMount) throws ApplicationException {
		// use existing application
		final ApplicationRegistration applicationRegistration = (ApplicationRegistration) req.getAttribute(INTERNAL_ATTR_APP_REGISTRATION);
		if (null != applicationRegistration) {
			return applicationRegistration;
		}

		// get the application registration
		return getApplicationManager().getApplicationRegistration(applicationMount.getApplicationId());
	}

	private String getClientAddress(final HttpServletRequest req) throws ApplicationException {
		// determine
		final String clientIP = req.getHeader(INTERNAL_HEADER_CLIENT_IP);
		if (null != clientIP) {
			// verify key
			if (verifyHeaderKey(req)) {
				return clientIP;
			}
		}

		// fallback to remote address if no header was provided
		return req.getRemoteAddr();
	}

	private int getClientPort(final HttpServletRequest req) throws ApplicationException {
		// determine
		final int clientPort = req.getIntHeader(INTERNAL_HEADER_CLIENT_PORT);
		if (clientPort != -1) {
			// verify key
			if (verifyHeaderKey(req)) {
				return clientPort;
			}
		}

		// fallback to remote address if no header was provided
		return req.getRemotePort();
	}

	private URL getRequestUrl(final HttpServletRequest req) throws ApplicationException {
		try {
			// use given request url if available
			final String requestUrl = (String) req.getAttribute(INTERNAL_ATTR_REQUEST_URL);
			if (null != requestUrl) {
				return new URL(requestUrl);
			}

			// use url provided as HTTP header
			final String originalUrl = req.getHeader(INTERNAL_HEADER_ORIGINAL_URL);
			if (null != originalUrl) {
				// verify key
				if (verifyHeaderKey(req)) {
					return new URL(originalUrl);
				}
			}

			// fallback to request URL from container
			return new URL(req.getRequestURL().toString());
		} catch (final MalformedURLException e) {
			throw new ApplicationException(HttpServletResponse.SC_BAD_REQUEST, MessageFormat.format("Invalid Request URL ({0})", e.getMessage()));
		}
	}

	private void sendError(final HttpServletRequest req, final HttpServletResponse res, final ApplicationException e) throws UnavailableException {
		try {
			// we pass the root cause a long so that an error page might have more context
			final Throwable cause = e.getCause();
			if (null != cause) {
				req.setAttribute("javax.servlet.error.exception", cause);
				//req.setAttribute("javax.servlet.error.exception_type", cause.getClass());
			}
			res.sendError(e.getStatus(), e.getMessage());
			return;
		} catch (final Exception sendErrorException) {
			// TODO: consider logging this, there is nothing much we can do here
			// note, we'll be unavailable for 5 seconds only to allow a fast recover
			// this could be more smart in the future (eg. an increase depending on load)
			final UnavailableException unavailableException = new UnavailableException("Internal Error", 5);
			unavailableException.initCause(sendErrorException);
			throw unavailableException;
		}
	}

	/**
	 * Receives standard HTTP requests from
	 * {@link #service(ServletRequest, ServletResponse)} and dispatches to
	 * {@link #doService(HttpServletRequest, HttpServletResponse)}.
	 * 
	 * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		try {
			// handle the request
			doService(req, resp);
		} catch (final ApplicationException e) {
			if (HttpDebug.handlerServlet) {
				LOG.debug(MessageFormat.format("[ERROR {2}] [{0}] {1}: {3}", req.getMethod(), req.getRequestURL(), e.getStatus(), e.getMessage()), e);
			}
			sendError(req, resp, e);
		} catch (final Throwable t) {
			if (HttpDebug.handlerServlet) {
				LOG.debug(MessageFormat.format("[UNHANDLED EXCEPTION] [{0}] {1}: {2}", req.getMethod(), req.getRequestURL(), t), t);
			}
			// we cache throwable here because we don't want to exceptions to slip through to an end user
			if (PlatformConfiguration.isOperatingInDevelopmentMode()) {
				throw new ServletException("Application Handler Error", t);
			} else {
				// send a 503 to encourage the caller to re-try later
				// TODO: consider logging original exception
				// note, we must not throw javax.servlet.UnavailableException here because
				// this will block the whole server and affects all applications
				// we also don't set a cause to prevent exposing stack straces to users
				sendError(req, resp, new ApplicationException(503, "Application Handler Error"));
			}
		}
	}

	private boolean verifyHeaderKey(final HttpServletRequest req) throws ApplicationException {
		//final String key = req.getHeader("X-Gyrex-Key");
		// TODO implement header key validation
		// TODO this should be configurable
		throw new ApplicationException(HttpServletResponse.SC_FORBIDDEN, "invalid header key submitted");
	}

}
