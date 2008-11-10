/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Cognos Incorporated, IBM Corporation - concept/implementation from 
 *                                            org.eclipse.equinox.http.servlet
 *     Gunnar Wagenknecht - adaption to CloudFree
 *******************************************************************************/
package org.eclipse.cloudfree.http.internal.application.registrations;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.eclipse.cloudfree.http.internal.application.IApplicationServletConstants;

/**
 * Adapts a {@link HttpServletRequest} for a {@link ServletRegistration}.
 * <p>
 * <ul>
 * <li>{@link #getServletPath()} returns the alias</li>
 * <li>{@link #getPathInfo()} returns the path info below the alias</li>
 * <li>{@link #getContextPath()} returns the original context path plus the
 * alias</li>
 * <li>{@link #getAttribute(String)} adapts include request attribute paths but
 * all other attributes are returned as is</li>
 * </ul>
 * </p>
 */
class ServletRegistrationRequestAdaptor extends HttpServletRequestWrapper {
	/**
	 * unwraps the ServletRegistrationRequestAdaptor so it can be processed by
	 * the underlying servlet container
	 */
	static class RequestDispatcherAdaptor implements RequestDispatcher {

		private final RequestDispatcher requestDispatcher;

		public RequestDispatcherAdaptor(final RequestDispatcher requestDispatcher) {
			this.requestDispatcher = requestDispatcher;
		}

		@Override
		public void forward(ServletRequest req, final ServletResponse resp) throws ServletException, IOException {
			if (req instanceof ServletRegistrationRequestAdaptor) {
				req = ((ServletRegistrationRequestAdaptor) req).getRequest();
			}

			requestDispatcher.forward(req, resp);
		}

		@Override
		public void include(ServletRequest req, final ServletResponse resp) throws ServletException, IOException {
			if (req instanceof ServletRegistrationRequestAdaptor) {
				req = ((ServletRegistrationRequestAdaptor) req).getRequest();
			}

			requestDispatcher.include(req, resp);
		}
	}

	private final boolean isIncludedRequest;
	private final String alias;

	/**
	 * Creates a new instance.
	 * 
	 * @param request
	 */
	public ServletRegistrationRequestAdaptor(final HttpServletRequest request, final String alias) {
		super(request);
		this.alias = alias;
		isIncludedRequest = request.getAttribute(IApplicationServletConstants.REQ_ATTR_INCLUDE_REQUEST_URI) != null;
	}

	/**
	 * Adapts include request attribute paths. All other attributes are returned
	 * as is.
	 * 
	 * @see javax.servlet.ServletRequestWrapper#getAttribute(java.lang.String)
	 */
	@Override
	public Object getAttribute(final String name) {

		if (isIncludedRequest) {
			if (name.equals(IApplicationServletConstants.REQ_ATTR_INCLUDE_CONTEXT_PATH)) {
				String contextPath = (String) super.getAttribute(IApplicationServletConstants.REQ_ATTR_INCLUDE_CONTEXT_PATH);
				if ((contextPath == null) || contextPath.equals("/")) {
					contextPath = ""; //$NON-NLS-1$
				}

				String servletPath = (String) super.getAttribute(IApplicationServletConstants.REQ_ATTR_INCLUDE_SERVLET_PATH);
				if ((servletPath == null) || servletPath.equals("/")) {
					servletPath = ""; //$NON-NLS-1$
				}

				return contextPath + servletPath;
			} else if (name.equals(IApplicationServletConstants.REQ_ATTR_INCLUDE_SERVLET_PATH)) {
				if (alias.equals("/")) { //$NON-NLS-1$
					return ""; //$NON-NLS-1$
				}
				return alias;
			} else if (name.equals(IApplicationServletConstants.REQ_ATTR_INCLUDE_PATH_INFO)) {
				String pathInfo = (String) super.getAttribute(IApplicationServletConstants.REQ_ATTR_INCLUDE_PATH_INFO);
				if (alias.equals("/")) { //$NON-NLS-1$
					return pathInfo;
				}

				pathInfo = pathInfo.substring(alias.length());
				if (pathInfo.length() == 0) {
					return null;
				}

				return pathInfo;
			}
		}

		return super.getAttribute(name);
	}

	/**
	 * Returns the original context path plus the alias.
	 * 
	 * @see javax.servlet.http.HttpServletRequestWrapper#getContextPath()
	 */
	@Override
	public String getContextPath() {

		if (isIncludedRequest) {
			return super.getContextPath();
		}

		return super.getContextPath() + super.getServletPath();
	}

	/**
	 * Returns the path info below the alias.
	 * 
	 * @see javax.servlet.http.HttpServletRequestWrapper#getPathInfo()
	 */
	@Override
	public String getPathInfo() {
		if (isIncludedRequest) {
			return super.getPathInfo();
		}

		if ((alias.length() == 1) && (alias.charAt(0) == '/')) {
			return super.getPathInfo();
		}

		final String pathInfo = super.getPathInfo().substring(alias.length());
		if (pathInfo.length() == 0) {
			return null;
		}

		return pathInfo;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequestWrapper#getRequestDispatcher(java.lang.String)
	 */
	@Override
	public RequestDispatcher getRequestDispatcher(final String path) {
		if (null == path) {
			return null;
		}
		return new RequestDispatcherAdaptor(super.getRequestDispatcher(super.getServletPath().concat(path)));
	}

	/**
	 * Returns the alias.
	 * 
	 * @see javax.servlet.http.HttpServletRequestWrapper#getServletPath()
	 */
	@Override
	public String getServletPath() {
		if (isIncludedRequest) {
			return super.getServletPath();
		}

		if ((alias.length() == 1) && (alias.charAt(0) == '/')) {
			return "";
		}
		return alias;
	}
}