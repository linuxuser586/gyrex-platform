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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;


import org.eclipse.cloudfree.http.internal.application.IApplicationServletConstants;
import org.eclipse.cloudfree.http.internal.application.manager.ApplicationMount;
import org.eclipse.cloudfree.http.internal.application.manager.ApplicationRegistration;
import org.mortbay.util.URIUtil;

/**
 * Unwraps the request so it can be processed by the underlying servlet
 * container.
 */
public class ApplicationRequestDispatcherAdapter implements RequestDispatcher, IApplicationServletConstants {

	private final RequestDispatcher applicationServletDispatcher;
	private final String pathInContext;
	private final String queryString;

	/**
	 * Creates a new instance.
	 * 
	 * @param applicationRegistration
	 * @param pathInContext
	 * @param queryString
	 */
	public ApplicationRequestDispatcherAdapter(final RequestDispatcher applicationServletDispatcher, final String pathInContext, final String queryString) {
		this.applicationServletDispatcher = applicationServletDispatcher;
		this.pathInContext = pathInContext;
		this.queryString = queryString;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.RequestDispatcher#forward(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
	 */
	@Override
	public void forward(ServletRequest request, final ServletResponse response) throws ServletException, IOException {

		// When a servlet has been invoked by a forward, the path related 
		// methods of the request object return the path of the target servlet.
		//
		// Request attributes javax.servlet.forward.* contain values 
		// corresponding to the invoking servlet. 

		// get ApplicationRequestAdapter
		final ApplicationRequestAdapter appRequestAdapter = unwrapApplicationRequestAdapter(request);

		// unwrap the request
		request = appRequestAdapter.getRequest();

		// read existing attributes
		final ApplicationRegistration applicationRegistration = (ApplicationRegistration) request.getAttribute(INTERNAL_ATTR_APP_REGISTRATION);
		final ApplicationMount applicationMount = (ApplicationMount) request.getAttribute(INTERNAL_ATTR_APP_MOUNT);
		final String origRequestUrl = (String) request.getAttribute(INTERNAL_ATTR_REQUEST_URL);
		final String origRequestUri = (String) request.getAttribute(INTERNAL_ATTR_REQUEST_URI);
		final String origContextPath = (String) request.getAttribute(INTERNAL_ATTR_CONTEXT_PATH);
		final String origPathInfo = (String) request.getAttribute(INTERNAL_ATTR_PATH_INFO);
		final String origQueryString = (String) request.getAttribute(INTERNAL_ATTR_QUERY_STRING);

		boolean cleanupForwarded = false;
		try {
			// set request url
			request.setAttribute(INTERNAL_ATTR_REQUEST_URL, URIUtil.addPaths(applicationMount.getMountPoint().toString(), pathInContext));

			// set path info
			request.setAttribute(INTERNAL_ATTR_PATH_INFO, pathInContext);

			// set query string
			request.setAttribute(INTERNAL_ATTR_QUERY_STRING, queryString);

			// set forward attribute overrides (if not forwarded yet)
			if (null == request.getAttribute(INTERNAL_ATTR_FORWARDED)) {
				cleanupForwarded = true;
				request.setAttribute(INTERNAL_ATTR_FORWARDED, Boolean.TRUE);
				request.setAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_FORWARD_REQUEST_URI, origRequestUri);
				request.setAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_FORWARD_CONTEXT_PATH, origContextPath);
				request.setAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_FORWARD_SERVLET_PATH, ""); // application servlet path is always empty
				request.setAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_FORWARD_PATH_INFO, origPathInfo);
				request.setAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_FORWARD_QUERY_STRING, origQueryString);
			}

			// forward
			applicationServletDispatcher.forward(request, response);

		} finally {
			// cleanup forward attribute overrides (if previously set)
			if (cleanupForwarded) {
				request.removeAttribute(INTERNAL_ATTR_FORWARDED);
				request.removeAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_FORWARD_REQUEST_URI);
				request.removeAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_FORWARD_CONTEXT_PATH);
				request.removeAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_FORWARD_SERVLET_PATH);
				request.removeAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_FORWARD_PATH_INFO);
				request.removeAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_FORWARD_QUERY_STRING);
			}

			// restore existing attributes
			request.setAttribute(INTERNAL_ATTR_APP_REGISTRATION, applicationRegistration);
			request.setAttribute(INTERNAL_ATTR_APP_MOUNT, applicationMount);
			request.setAttribute(INTERNAL_ATTR_REQUEST_URL, origRequestUrl);
			request.setAttribute(INTERNAL_ATTR_REQUEST_URI, origRequestUri);
			request.setAttribute(INTERNAL_ATTR_CONTEXT_PATH, origContextPath);
			request.setAttribute(INTERNAL_ATTR_PATH_INFO, origPathInfo);
			request.setAttribute(INTERNAL_ATTR_QUERY_STRING, origQueryString);
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.RequestDispatcher#include(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
	 */
	@Override
	public void include(ServletRequest request, final ServletResponse response) throws ServletException, IOException {
		// When a servlet has been invoked by an include, the path related 
		// methods of the request object correspond to the invoking servlet.
		//
		// Request attributes javax.servlet.include.* contain values 
		// corresponding to the target.  

		// get ApplicationRequestAdapter
		final ApplicationRequestAdapter appRequestAdapter = unwrapApplicationRequestAdapter(request);

		// unwrap the request
		request = appRequestAdapter.getRequest();

		// backup existing include attribute overrides
		final boolean alreadyIncluded = null != request.getAttribute(INTERNAL_ATTR_INCLUDED);
		final String origRequestUri = (String) request.getAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_INCLUDE_REQUEST_URI);
		final String origContextPath = (String) request.getAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_INCLUDE_CONTEXT_PATH);
		final String origServletPath = (String) request.getAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_INCLUDE_SERVLET_PATH);
		final String origPathInfo = (String) request.getAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_INCLUDE_PATH_INFO);
		final String origQueryString = (String) request.getAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_INCLUDE_QUERY_STRING);

		try {
			// set include attribute overrides
			request.setAttribute(INTERNAL_ATTR_INCLUDED, Boolean.TRUE);
			request.setAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_INCLUDE_REQUEST_URI, pathInContext);
			request.setAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_INCLUDE_CONTEXT_PATH, origContextPath); // stays the same
			request.setAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_INCLUDE_SERVLET_PATH, origServletPath); // stays the same
			request.setAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_INCLUDE_PATH_INFO, pathInContext);
			request.setAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_INCLUDE_QUERY_STRING, queryString);

			// include
			applicationServletDispatcher.include(request, response);

		} finally {
			if (alreadyIncluded) {
				// restore original include attribute overrides
				request.setAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_INCLUDE_REQUEST_URI, origRequestUri);
				request.setAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_INCLUDE_CONTEXT_PATH, origContextPath);
				request.setAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_INCLUDE_SERVLET_PATH, origServletPath);
				request.setAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_INCLUDE_PATH_INFO, origPathInfo);
				request.setAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_INCLUDE_QUERY_STRING, origQueryString);
			} else {
				// remove include attribute overrides
				request.removeAttribute(INTERNAL_ATTR_INCLUDED);
				request.removeAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_INCLUDE_REQUEST_URI);
				request.removeAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_INCLUDE_CONTEXT_PATH);
				request.removeAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_INCLUDE_SERVLET_PATH);
				request.removeAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_INCLUDE_PATH_INFO);
				request.removeAttribute(INTERNAL_ATTR_PREFIX + REQ_ATTR_INCLUDE_QUERY_STRING);
			}
		}
	}

	private ApplicationRequestAdapter unwrapApplicationRequestAdapter(ServletRequest request) {
		final int MAX_TRIES = 50;
		int i = 0;
		while (!(request instanceof ApplicationRequestAdapter)) {
			if ((i == MAX_TRIES) || !(request instanceof ServletRequestWrapper)) {
				throw new IllegalArgumentException("invalid servlet request (the specified request must wrap the original request)");
			}
			request = ((ServletRequestWrapper) request).getRequest();
			i++;
		}
		return (ApplicationRequestAdapter) request;
	}

}
