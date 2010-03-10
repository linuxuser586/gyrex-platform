/**
 * Copyright (c) 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.log.http.wildfire;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.gyrex.log.http.internal.wildfire.FirePHPLogger;

/**
 * A servlet filter which captures log messages and writes them in the HTTP
 * response using the Wildfire Protocol.
 */
public class WidlfireLogWriterFilter implements Filter {

	public static final String ATTR_NAME_LOGGING_ENABLED = "org.eclipse.gyrex.log.http.wildfire.enabled";

	/**
	 * Disables logging for the specified HttpSession.
	 * 
	 * @param session
	 *            the session to disable logging for
	 */
	public static void disableLogging(final HttpSession session) {
		session.removeAttribute(ATTR_NAME_LOGGING_ENABLED);
	}

	/**
	 * Enables logging for the specified HttpSession.
	 * 
	 * @param session
	 *            the session to enable logging for
	 */
	public static void enableLogging(final HttpSession session) {
		session.setAttribute(ATTR_NAME_LOGGING_ENABLED, Boolean.TRUE);
	}

	private final AtomicBoolean sessionSpecificLogging = new AtomicBoolean();

	@Override
	public void destroy() {
		// empty
	}

	public void disableSessionSpecificLogging() {
		sessionSpecificLogging.set(false);
	}

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
		try {
			// note, we just expect a HTTP environment; no instance-of checks will be performed
			final HttpServletRequest httpServletRequest = (HttpServletRequest) request;

			// hook with the logging system if active
			final HttpSession session = httpServletRequest.getSession(false);
			if (!sessionSpecificLogging.get() || ((null != session) && Boolean.TRUE.equals(session.getAttribute(ATTR_NAME_LOGGING_ENABLED)))) {
				FirePHPLogger.setResponse((HttpServletResponse) response);
			}

			// handle the request
			chain.doFilter(request, response);
		} finally {
			// remove from logging
			FirePHPLogger.setResponse(null);
		}
	}

	public void enableSessionSpecificLogging() {
		sessionSpecificLogging.set(true);
	}

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
		// empty
	}
}
