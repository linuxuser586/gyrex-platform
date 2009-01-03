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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.eclipse.cloudfree.http.application.Application;
import org.eclipse.cloudfree.http.application.IApplicationConstants;
import org.eclipse.cloudfree.http.internal.application.IApplicationServletConstants;
import org.eclipse.cloudfree.http.internal.application.manager.ApplicationMount;
import org.eclipse.cloudfree.http.internal.application.manager.ApplicationRegistration;
import org.mortbay.util.URIUtil;

/**
 * Adapts {@link HttpServletRequest} for {@link Application}.
 * <p>
 * <ul>
 * <li>{@link #getContextPath()} returns the path of the application mount point
 * the request was received on</li>
 * <li>{@link #getServletPath()} returns an empty path (there is no servlet path
 * for the application servlet)</li>
 * <li>{@link #getPathInfo()} returns the path of the incoming request below the
 * context path/mount point</li>
 * </ul>
 * </p>
 */
public final class ApplicationRequestAdapter implements IApplicationServletConstants, HttpServletRequest {

	private static class FilteringEnumeration implements Enumeration {

		private final Enumeration source;
		private final String filteredPrefix;
		private Object next;

		/**
		 * Creates a new instance.
		 */
		public FilteringEnumeration(final Enumeration source, final String filteredPrefix) {
			this.source = source;
			this.filteredPrefix = filteredPrefix;
		}

		private void determineNext() {
			if (null != next) {
				return;
			}
			do {
				if (source.hasMoreElements()) {
					next = source.nextElement();
					if ((null != next) && !(next instanceof String)) {
						throw new ClassCastException("The source enumeration is expected to return strings but it returned: " + next.getClass().getName());
					}
				} else {
					next = null;
				}
			} while ((next != null) && ((String) next).startsWith(filteredPrefix));
		}

		/* (non-Javadoc)
		 * @see java.util.Enumeration#hasMoreElements()
		 */
		@Override
		public boolean hasMoreElements() {
			determineNext();
			return null != next;
		}

		/* (non-Javadoc)
		 * @see java.util.Enumeration#nextElement()
		 */
		@Override
		public Object nextElement() {
			determineNext();
			final Object next = this.next;
			if (null == next) {
				throw new NoSuchElementException();
			}
			this.next = null;
			return next;
		}

	}

	private final HttpServletRequest request;
	private final ApplicationRegistration applicationRegistration;
	private final ServletContext servletContext;
	private final URL requestUrl;
	private final String clientAddress;
	private final int clientPort;
	private String contextPath;

	private String pathInfo;
	private final ApplicationMount applicationMount;

	public ApplicationRequestAdapter(final HttpServletRequest request, final URL requestUrl, final ApplicationRegistration applicationRegistration, final ApplicationMount applicationMount, final ServletContext servletContext, final String clientAddress, final int clientPort) {
		this.request = request;
		this.requestUrl = requestUrl;
		this.applicationRegistration = applicationRegistration;
		this.applicationMount = applicationMount;
		this.servletContext = servletContext;
		this.clientAddress = clientAddress;
		this.clientPort = clientPort;

		// note, calling non-private members in a constructor is harmful
		// but we are final so it's ok

		// store attributes
		request.setAttribute(INTERNAL_ATTR_APP_REGISTRATION, getApplicationRegistration());
		request.setAttribute(INTERNAL_ATTR_APP_MOUNT, getApplicationMount());
		request.setAttribute(INTERNAL_ATTR_REQUEST_URL, requestUrl.toString());
		request.setAttribute(INTERNAL_ATTR_REQUEST_URI, getRequestURI());
		request.setAttribute(INTERNAL_ATTR_CONTEXT_PATH, getContextPath());
		request.setAttribute(INTERNAL_ATTR_PATH_INFO, getPathInfo());
		request.setAttribute(INTERNAL_ATTR_QUERY_STRING, getQueryString());

		// store public attributes
		request.setAttribute(IApplicationConstants.REQUEST_ATTRIBUTE_MOUNT_POINT, applicationMount.getMountPoint().toExternalForm());
	}

	private ApplicationMount getApplicationMount() {
		return applicationMount;
	}

	private ApplicationRegistration getApplicationRegistration() {
		return applicationRegistration;
	}

	@Override
	public Object getAttribute(final String name) {
		// filter internal attributes
		if (name.startsWith(INTERNAL_ATTR_PREFIX)) {
			return null;
		}

		// special handling for included or forwarded requests because they may be overwritten
		if (name.startsWith("javax.servlet.include.")) {
			if (null == getRequest().getAttribute(INTERNAL_ATTR_INCLUDED)) {
				return null;
			}

			return getRequest().getAttribute(INTERNAL_ATTR_PREFIX.concat(name));
		} else if (name.startsWith("javax.servlet.forward.")) {
			if (null == getRequest().getAttribute(INTERNAL_ATTR_FORWARDED)) {
				return null;
			}

			return getRequest().getAttribute(INTERNAL_ATTR_PREFIX.concat(name));
		}

		// return attribute
		return getRequest().getAttribute(name);
	}

	@Override
	public Enumeration getAttributeNames() {
		// filter internal attributes
		return new FilteringEnumeration(getRequest().getAttributeNames(), INTERNAL_ATTR_PREFIX);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getAuthType()
	 */
	@Override
	public String getAuthType() {
		return getRequest().getAuthType();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getCharacterEncoding()
	 */
	@Override
	public String getCharacterEncoding() {
		return getRequest().getCharacterEncoding();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getContentLength()
	 */
	@Override
	public int getContentLength() {
		return getRequest().getContentLength();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getContentType()
	 */
	@Override
	public String getContentType() {
		return getRequest().getContentType();
	}

	/**
	 * Returns the application mount point the request was received on.
	 * 
	 * @see javax.servlet.http.HttpServletRequestWrapper#getContextPath()
	 */
	@Override
	public String getContextPath() {
		// use attribute if set
		if (null != getRequest().getAttribute(INTERNAL_ATTR_CONTEXT_PATH)) {
			return (String) getRequest().getAttribute(INTERNAL_ATTR_CONTEXT_PATH);
		}
		// our context path is always the mount point of an application
		if (null == contextPath) {
			contextPath = getApplicationMount().getMountPoint().getPath();
			// sanitize the context path
			if ((contextPath.length() > 0)) {
				// must start with a slash
				if ((contextPath.charAt(0) != '/')) {
					contextPath = "/" + contextPath;
				}

				// must not end with a slash
				if ((contextPath.length() > 1) && (contextPath.charAt(contextPath.length() - 1) == '/')) {
					contextPath = contextPath.substring(0, contextPath.length() - 1);
				}

				// check if we reduced to the root path (eg. //)
				if (contextPath.length() == 1) {
					contextPath = "";
				}
			}
		}
		return contextPath;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getCookies()
	 */
	@Override
	public Cookie[] getCookies() {
		return getRequest().getCookies();
	}

	@Override
	public long getDateHeader(final String name) {
		// filter internal attributes
		if (name.startsWith(INTERNAL_HEADER_PREFIX)) {
			return -1;
		}
		return getRequest().getDateHeader(name);
	}

	@Override
	public String getHeader(final String name) {
		// filter internal attributes
		if (name.startsWith(INTERNAL_HEADER_PREFIX)) {
			return null;
		}
		return getRequest().getHeader(name);
	}

	@Override
	public Enumeration getHeaderNames() {
		final Enumeration headerNames = getRequest().getHeaderNames();
		if (headerNames == null) {
			return null;
		}
		// filter internal attributes
		return new FilteringEnumeration(headerNames, INTERNAL_HEADER_PREFIX);
	}

	@Override
	public Enumeration getHeaders(final String name) {
		// filter internal attributes
		if (name.startsWith(INTERNAL_HEADER_PREFIX)) {
			return Collections.enumeration(Collections.emptyList());
		}
		return getRequest().getHeaders(name);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getInputStream()
	 */
	@Override
	public ServletInputStream getInputStream() throws IOException {
		return getRequest().getInputStream();
	}

	@Override
	public int getIntHeader(final String name) {
		// filter internal attributes
		if (name.startsWith(INTERNAL_HEADER_PREFIX)) {
			return -1;
		}
		return getRequest().getIntHeader(name);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getLocalAddr()
	 */
	@Override
	public String getLocalAddr() {
		return getRequest().getLocalAddr();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getLocale()
	 */
	@Override
	public Locale getLocale() {
		return getRequest().getLocale();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getLocales()
	 */
	@Override
	public Enumeration getLocales() {
		return getRequest().getLocales();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getLocalName()
	 */
	@Override
	public String getLocalName() {
		return getRequest().getLocalName();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getLocalPort()
	 */
	@Override
	public int getLocalPort() {
		return getRequest().getLocalPort();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getMethod()
	 */
	@Override
	public String getMethod() {
		return getRequest().getMethod();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getParameter(java.lang.String)
	 */
	@Override
	public String getParameter(final String name) {
		return getRequest().getParameter(name);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getParameterMap()
	 */
	@Override
	public Map getParameterMap() {
		return getRequest().getParameterMap();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getParameterNames()
	 */
	@Override
	public Enumeration getParameterNames() {
		return getRequest().getParameterNames();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getParameterValues(java.lang.String)
	 */
	@Override
	public String[] getParameterValues(final String name) {
		return getRequest().getParameterValues(name);
	}

	@Override
	public String getPathInfo() {
		// use attribute if set
		if (null != getRequest().getAttribute(INTERNAL_ATTR_PATH_INFO)) {
			return (String) getRequest().getAttribute(INTERNAL_ATTR_PATH_INFO);
		}
		// compute the path info based on request URI and context path
		if (null == pathInfo) {
			final String contextPath = getContextPath();
			pathInfo = getRequestURI().substring(contextPath.length());
			// make sure path info starts with a slash
			if ((pathInfo.length() > 0) && (pathInfo.charAt(0) != '/')) {
				pathInfo = '/' + pathInfo;
			}
		}
		return pathInfo.length() > 0 ? pathInfo : null;
	}

	/**
	 * Returns <code>null</code>.
	 * 
	 * @see javax.servlet.http.HttpServletRequest#getPathTranslated()
	 */
	@Override
	public String getPathTranslated() {
		// we don't have a real path
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getProtocol()
	 */
	@Override
	public String getProtocol() {
		return getRequest().getProtocol();
	}

	@Override
	public String getQueryString() {
		// use attribute if set
		if (null != getRequest().getAttribute(INTERNAL_ATTR_QUERY_STRING)) {
			return (String) getRequest().getAttribute(INTERNAL_ATTR_QUERY_STRING);
		}
		// use getRequest()
		return getRequest().getQueryString();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#getReader()
	 */
	@Override
	public BufferedReader getReader() throws IOException {
		return getRequest().getReader();
	}

	/**
	 * Return <code>null</code>.
	 * 
	 * @see javax.servlet.ServletRequest#getRealPath(java.lang.String)
	 */
	@Override
	@Deprecated
	public String getRealPath(final String path) {
		// not supported
		return null;
	}

	@Override
	public String getRemoteAddr() {
		return clientAddress;
	}

	@Override
	public String getRemoteHost() {
		return clientAddress;
	}

	@Override
	public int getRemotePort() {
		return clientPort;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
	 */
	@Override
	public String getRemoteUser() {
		return getRequest().getRemoteUser();
	}

	/**
	 * Returns the underlying request.
	 * 
	 * @return the request
	 */
	/*package */HttpServletRequest getRequest() {
		return request;
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String uri) {
		if ((null == uri) || (uri.length() == 0)) {
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

		// resolve relative paths
		if (uri.charAt(0) != '/') {
			uri = URIUtil.canonicalPath(URIUtil.addPaths(getRequestURI(), uri));
			if (null == uri) {
				return null;
			}
		}

		// ask for the containers request dispatcher to the application servlet
		final RequestDispatcher dispatcher = getRequest().getRequestDispatcher(getRequest().getServletPath().concat(null == queryString ? "" : "?".concat(queryString)));
		if (null == dispatcher) {
			return null;
		}

		// return adapted dispatcher
		return new ApplicationRequestDispatcherAdapter(dispatcher, uri, queryString);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getRequestedSessionId()
	 */
	@Override
	public String getRequestedSessionId() {
		return getRequest().getRequestedSessionId();
	}

	@Override
	public String getRequestURI() {
		// use attribute if set
		if (null != getRequest().getAttribute(INTERNAL_ATTR_REQUEST_URI)) {
			return (String) getRequest().getAttribute(INTERNAL_ATTR_REQUEST_URI);
		}
		// the path of the request url
		return requestUrl.getPath();
	}

	@Override
	public StringBuffer getRequestURL() {
		// the request url
		return new StringBuffer(requestUrl.toString());
	}

	@Override
	public String getScheme() {
		// the request url protocol
		return requestUrl.getProtocol();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequestWrapper#getServerName()
	 */
	@Override
	public String getServerName() {
		// use the request url
		return requestUrl.getHost();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequestWrapper#getServerPort()
	 */
	@Override
	public int getServerPort() {
		// use the request url
		final int port = requestUrl.getPort();
		return port != -1 ? port : requestUrl.getDefaultPort();
	}

	/**
	 * Returns an empty path
	 * 
	 * @see javax.servlet.http.HttpServletRequestWrapper#getServletPath()
	 */
	@Override
	public String getServletPath() {
		// the application servlet is registered at the root
		return "";
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getSession()
	 */
	@Override
	public HttpSession getSession() {
		final HttpSession session = getRequest().getSession();
		if (session != null) {
			return new ApplicationSessionAdapter(session, servletContext);
		}

		return null;

	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getSession(boolean)
	 */
	@Override
	public HttpSession getSession(final boolean create) {
		final HttpSession session = getRequest().getSession(create);
		if (session != null) {
			return new ApplicationSessionAdapter(session, servletContext);
		}

		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
	 */
	@Override
	public Principal getUserPrincipal() {
		return getRequest().getUserPrincipal();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromCookie()
	 */
	@Override
	public boolean isRequestedSessionIdFromCookie() {
		return getRequest().isRequestedSessionIdFromCookie();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromUrl()
	 */
	@Override
	@Deprecated
	public boolean isRequestedSessionIdFromUrl() {
		return getRequest().isRequestedSessionIdFromUrl();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromURL()
	 */
	@Override
	public boolean isRequestedSessionIdFromURL() {
		return getRequest().isRequestedSessionIdFromURL();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdValid()
	 */
	@Override
	public boolean isRequestedSessionIdValid() {
		return getRequest().isRequestedSessionIdValid();
	}

	@Override
	public boolean isSecure() {
		// we decide based on the original request
		return "https".equalsIgnoreCase(requestUrl.getProtocol());
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletRequest#isUserInRole(java.lang.String)
	 */
	@Override
	public boolean isUserInRole(final String role) {
		return getRequest().isUserInRole(role);
	}

	@Override
	public void removeAttribute(final String name) {
		// filter internal attributes
		if (name.startsWith(INTERNAL_ATTR_PREFIX)) {
			return;
		}
		getRequest().removeAttribute(name);
	}

	@Override
	public void setAttribute(final String name, final Object o) {
		// filter internal attributes
		if (name.startsWith(INTERNAL_ATTR_PREFIX)) {
			return;
		}
		getRequest().setAttribute(name, o);
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletRequest#setCharacterEncoding(java.lang.String)
	 */
	@Override
	public void setCharacterEncoding(final String env) throws UnsupportedEncodingException {
		getRequest().setCharacterEncoding(env);
	}
}
