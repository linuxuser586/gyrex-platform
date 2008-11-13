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
 *     Cognos Incorporated, IBM Corporation - concept/implementation from 
 *                                            org.eclipse.equinox.http.servlet
 *******************************************************************************/
package org.eclipse.cloudfree.http.internal.application;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.cloudfree.http.application.ApplicationException;
import org.eclipse.cloudfree.http.application.servicesupport.IApplicationServiceSupport;
import org.eclipse.cloudfree.http.application.servicesupport.IResourceProvider;
import org.eclipse.cloudfree.http.application.servicesupport.NamespaceException;
import org.eclipse.cloudfree.http.internal.HttpActivator;
import org.eclipse.cloudfree.http.internal.application.helpers.ApplicationServletConfig;
import org.eclipse.cloudfree.http.internal.application.helpers.ServletUtil;
import org.eclipse.cloudfree.http.internal.application.registrations.Registration;
import org.eclipse.cloudfree.http.internal.application.registrations.RegistrationsManager;
import org.eclipse.cloudfree.http.internal.application.registrations.ServletRegistration;
import org.osgi.framework.Bundle;

/**
 * {@link IApplicationServiceSupport} implementation.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public final class ApplicationServiceSupport implements IApplicationServiceSupport {

	private static final String ROOT_ALIAS = "/";
	private final AtomicReference<RegistrationsManager> registrationManager = new AtomicReference<RegistrationsManager>();
	private final AtomicReference<ServletContext> servletContext = new AtomicReference<ServletContext>();

	/**
	 * Creates a new instance.
	 * 
	 * @param servletContext
	 */
	public ApplicationServiceSupport(final ServletContext servletContext) {
		this.servletContext.set(servletContext);
	}

	private String findExtensionAlias(final String alias) {
		final String lastSegment = alias.substring(alias.lastIndexOf('/') + 1);
		final int dot = lastSegment.indexOf('.');
		if (dot == -1) {
			return null;
		}
		final String extension = lastSegment.substring(dot + 1);
		if (extension.length() == 0) {
			return null;
		}
		return "/*." + extension; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.http.application.service.IApplicationServiceSupport#getMimeType(java.lang.String)
	 */
	@Override
	public String getMimeType(final String file) {
		// ask the registered resource providers
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.http.application.service.IApplicationServiceSupport#getResource(java.lang.String)
	 */
	@Override
	public URL getResource(final String path) {
		// loop through the registered resource registrations
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.http.application.service.IApplicationServiceSupport#getResourcePaths(java.lang.String)
	 */
	@Override
	public Set getResourcePaths(final String path) {
		// collect from the registered resource registrations
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.http.application.service.IApplicationServiceSupport#getServletContext()
	 */
	@Override
	public ServletContext getServletContext() {
		final ServletContext context = servletContext.get();
		if (null == context) {
			throw new IllegalStateException("destroyed");
		}
		return context;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.http.application.service.IApplicationServiceSupport#handleRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public boolean handleRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ApplicationException {
		// get the alias
		String alias = ServletUtil.getPathInfo(request);
		if ((null == alias) || (alias.length() == 0)) {
			alias = ROOT_ALIAS;
		}

		// perfect match
		if (processAlias(request, response, alias, null)) {
			return true;
		}

		// get extension alias
		String extensionAlias = findExtensionAlias(alias);
		alias = alias.substring(0, alias.lastIndexOf('/'));

		// longest path match
		while (alias.length() != 0) {
			if (processAlias(request, response, alias, extensionAlias)) {
				return true;
			}
			alias = alias.substring(0, alias.lastIndexOf('/'));
		}

		// default handler match
		if (extensionAlias != null) {
			extensionAlias = extensionAlias.substring(1); // remove the leading '/'
		}
		return processAlias(request, response, ROOT_ALIAS, extensionAlias);
	}

	private void initRegistrationManager() {
		if (null == registrationManager.get()) {
			if (registrationManager.compareAndSet(null, new RegistrationsManager())) {
				registrationManager.get().init();
			}
		}
	}

	/**
	 * Finds the registration for an alias and processes it.
	 * 
	 * @param req
	 *            the request
	 * @param resp
	 *            the response
	 * @param alias
	 *            the alias
	 * @param extensionAlias
	 *            the extension alias (eg. <code>/*.gif</code>)
	 * @return <code>true</code> if a registration for the specified alias was
	 *         found and the request processed, <code>false</code> otherwise
	 * @throws ApplicationException
	 *             wraps a {@link ServletException} if thrown by the underlying
	 *             servlet while processing the request
	 * @throws IOException
	 *             if an input or output exception occurs
	 */
	private boolean processAlias(final HttpServletRequest req, final HttpServletResponse resp, String alias, final String extensionAlias) throws IOException, ApplicationException {
		final RegistrationsManager registrations = registrationManager.get();
		if (null == registrations) {
			return false;
		}
		Registration registration = null;
		if (extensionAlias == null) {
			registration = registrations.get(alias);
		} else {
			registration = registrations.get(alias + extensionAlias);
			if (registration != null) {
				// for ServletRegistrations extensions should be handled on the full alias
				if (registration instanceof ServletRegistration) {
					alias = ServletUtil.getPathInfo(req);
				}
			} else {
				registration = registrations.get(alias);
			}
		}

		// process the request
		if (registration != null) {
			try {
				return registration.handleRequest(req, resp, alias);
			} catch (final ServletException e) {
				throw new ApplicationException(e);
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.http.application.service.IApplicationServiceSupport#registerResources(java.lang.String, java.lang.String, org.eclipse.cloudfree.http.application.service.IResourceProvider)
	 */
	@Override
	public void registerResources(final String alias, final String name, final IResourceProvider provider) throws NamespaceException {
		// lazy initialize
		initRegistrationManager();
		registrationManager.get().registerResource(alias, name, provider, getServletContext());
	}

	//	/* (non-Javadoc)
	//	 * @see org.eclipse.cloudfree.http.application.servicesupport.IApplicationServiceSupport#registerService(java.lang.String, org.eclipse.core.runtime.IAdaptable)
	//	 */
	//	@Override
	//	public void registerService(final String alias, final IAdaptable service) throws ServletException, NamespaceException {
	//		// lazy initialize
	//		//initRegistrationManager();
	//
	//		// get service servlet
	//
	//		// register servlet
	//	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.http.application.service.IApplicationServiceSupport#registerServlet(java.lang.String, javax.servlet.Servlet, java.util.Map)
	 */
	@Override
	public void registerServlet(final String alias, final Servlet servlet, final Map<String, String> initparams) throws ServletException, NamespaceException {
		// lazy initialize
		initRegistrationManager();

		// servlet config
		final ServletConfig servletConfig = new ApplicationServletConfig(servlet.getClass().getName(), initparams, getServletContext());

		// register servlet
		registrationManager.get().registerServlet(alias, servlet, servletConfig);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.http.application.service.IApplicationServiceSupport#unregister(java.lang.String)
	 */
	@Override
	public void unregister(final String alias) {
		final RegistrationsManager registrationsManager = registrationManager.get();
		if (null == registrationsManager) {
			throw new IllegalArgumentException("alias '" + alias + "' not registered");
		}
		final Bundle callingBundle = HttpActivator.getInstance().getCallingBundle();
		registrationsManager.unregister(alias, true, callingBundle);
	}

}
