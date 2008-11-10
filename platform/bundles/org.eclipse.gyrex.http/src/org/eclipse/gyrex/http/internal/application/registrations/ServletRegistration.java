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

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;

/**
 * A servlet registrations.
 * <p>
 * This class was inherited and adapted from
 * <code>org.eclipse.equinox.http.servlet.internal.ServletRegistration</code>.
 * </p>
 */
public class ServletRegistration extends Registration {

	/** the servlet */
	private Servlet servlet;

	/** the context class loader used when registering the servlet */
	private ClassLoader registeredContextClassLoader;

	public ServletRegistration(final String alias, final Servlet servlet, final RegistrationsManager manager, final Bundle registrationBundle) {
		super(alias, manager, registrationBundle);
		this.servlet = servlet;
		registeredContextClassLoader = Thread.currentThread().getContextClassLoader();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.http.internal.application.registrations.Registration#doClose()
	 */
	@Override
	protected void doClose() {
		servlet = null;
		registeredContextClassLoader = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.http.internal.application.registrations.Registration#doDestroy()
	 */
	@Override
	protected void doDestroy() {
		final Servlet servlet = this.servlet;
		if (null != servlet) {
			servlet.destroy();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.http.internal.application.registrations.Registration#doHandleRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String)
	 */
	@Override
	protected boolean doHandleRequest(final HttpServletRequest req, final HttpServletResponse resp, final String alias) throws ServletException, IOException {
		final Servlet servlet = this.servlet;
		if (null == servlet) {
			return false;
		}

		// adapt the request
		final HttpServletRequest wrappedRequest = new ServletRegistrationRequestAdaptor(req, alias);

		final ClassLoader original = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(registeredContextClassLoader);
			servlet.service(wrappedRequest, resp);
		} finally {
			Thread.currentThread().setContextClassLoader(original);
		}
		return true;
	}

	/**
	 * Calls {@link Servlet#init(ServletConfig)}
	 * 
	 * @param servletConfig
	 * @throws ServletException
	 * @see {@link Servlet#init(ServletConfig)}
	 */
	public void initServlet(final ServletConfig servletConfig) throws ServletException {
		final Servlet servlet = this.servlet;
		if (null != servlet) {
			servlet.init(servletConfig);
		}
	}

}
