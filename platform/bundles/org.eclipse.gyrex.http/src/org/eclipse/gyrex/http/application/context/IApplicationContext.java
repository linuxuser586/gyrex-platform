/*******************************************************************************
 * Copyright (c) 2008, 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.application.context;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.ApplicationException;

import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Provides support to {@link Application applications} for dynamic servlet and
 * resource registration similar to {@link HttpService} and for communicating
 * with the underlying container.
 * <p>
 * The application context acts like a bridge between the application and the
 * servlet container. The {@link IApplicationContext} provides a dynamic
 * registry for servlets, filters, listeners and resources. It allows to built
 * and expose application level services which offer dynamic capabilities
 * similar to an OSGi Http Service. Additionally the underlying container can be
 * queried for further information (for example, such as mime types).
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IApplicationContext {

	/**
	 * A {@link #getServletContext() servlet context} attribute which value is
	 * the {@link Application} object (constant value
	 * <code>org.eclipse.gyrex.http.application</code>).
	 */
	String SERVLET_CONTEXT_ATTRIBUTE_APPLICATION = "org.eclipse.gyrex.http.application";

	/**
	 * A {@link #getServletContext() servlet context} attribute which value is
	 * the {@link IRuntimeContext} object (constant value
	 * <code>org.eclipse.gyrex.context</code>).
	 */
	String SERVLET_CONTEXT_ATTRIBUTE_CONTEXT = "org.eclipse.gyrex.context";

	/**
	 * Provides access to an application specific {@link ServletContext}.
	 * <p>
	 * The application servlet context is created and maintained by the
	 * underlying servlet container integration and provides access to the
	 * container. It can be viewed as a bridge between the application and the
	 * servlet container.
	 * </p>
	 * 
	 * @return the servlet context (maybe <code>null</code> if the application
	 *         is no longer active)
	 * @see #SERVLET_CONTEXT_ATTRIBUTE_APPLICATION
	 * @see #SERVLET_CONTEXT_ATTRIBUTE_CONTEXT
	 */
	public ServletContext getServletContext();

	/**
	 * Handles the request by matching the request's path info against the
	 * registered resources and servlets.
	 * <p>
	 * The matching happens similar to the OSGi Http Service. In addition to
	 * that, an extension mapping will be performed similar to the Eclipse
	 * Equinox Jetty based Http Service implementation.
	 * </p>
	 * 
	 * @param request
	 *            the request
	 * @param response
	 *            the response
	 * @return <code>true</code> if the request has been handled and a response
	 *         was sent back, <code>false</code> otherwise
	 * @throws IOException
	 *             if an input or output exception occurs
	 * @throws ApplicationException
	 *             if an exception occurs that interferes with the application's
	 *             normal operation
	 */
	boolean handleRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ApplicationException;

	/**
	 * Registers resources into the URI namespace.
	 * <p>
	 * The alias is the name in the URI namespace of the application at which
	 * the registration will be mapped.
	 * </p>
	 * <p>
	 * The purpose of this method is similar to
	 * {@link HttpService#registerResources(String, String, HttpContext)} but
	 * adapted to the {@link Application} scope.
	 * </p>
	 * <p>
	 * An alias must begin with slash ('/') and must not end with slash ('/'),
	 * with the exception that an alias of the form &quot;/&quot; is used to
	 * denote the root alias. The name parameter must also not end with slash
	 * ('/'). See the OSGi Http Service specification text for details on how
	 * HTTP requests are mapped to servlet and resource registrations.
	 * </p>
	 * <p>
	 * For example, suppose the resource name <code>/tmp</code> is registered to
	 * the alias <code>/files</code>. A request for <code>/files/foo.txt</code>
	 * will map to the resource name <code>/tmp/foo.txt</code>.
	 * </p>
	 * <p>
	 * The container will call the {@link IResourceProvider} argument to map
	 * resource names to URLs.
	 * </p>
	 * <p>
	 * The specified {@link IResourceProvider} object must be created by an OSGi
	 * bundle. It will be the bundle that is responsible for the registration.
	 * The container will monitor the bundle so that the registrations will be
	 * automatically (see {@link #unregister(String)} unregistered) when the
	 * bundle is stopped.
	 * </p>
	 * 
	 * @param alias
	 *            name in the URI namespace at which the resources are
	 *            registered
	 * @param name
	 *            the base name of the resources that will be registered
	 * @param provider
	 *            the {@link IResourceProvider} object for the registered
	 *            resources
	 * @throws NamespaceException
	 *             if the registration fails because the alias is already in use
	 * @throws java.lang.IllegalArgumentException
	 *             if any of the parameters are invalid
	 */
	void registerResources(final String alias, final String name, final IResourceProvider provider) throws NamespaceException;

	/**
	 * Registers a service into the URI namespace.
	 * <p>
	 * The alias is the name in the URI namespace of the application at which
	 * the registration will be mapped.
	 * </p>
	 * <p>
	 * An alias must begin with slash ('/') and must not end with slash ('/'),
	 * with the exception that an alias of the form &quot;/&quot; is used to
	 * denote the root alias. See the OSGi Http Service specification text for
	 * details on how HTTP requests are mapped to servlet and resource
	 * registrations.
	 * </p>
	 * <p>
	 * The application will call the service's <code>getAdapter</code> method to
	 * obtain an {@link IHttpService} object instance. If no such object is
	 * available, a HTTP request will be rejected with a 503 code (service
	 * unavailable).
	 * </p>
	 * <p>
	 * Under the cover, a servlet will be registered for each service object.
	 * All services registered with the same application object will share the
	 * same {@link ServletContext}.
	 * </p>
	 * <p>
	 * The specified service object must be created by an OSGi bundle. It will
	 * be the bundle that is responsible for the registration. The application
	 * will monitor the bundle so that the registrations will be automatically
	 * (see {@link #unregister(String)} unregistered) when the bundle is
	 * stopped.
	 * </p>
	 * 
	 * @param alias
	 *            name in the URI namespace at which the servlet is registered
	 * @param service
	 *            the service object to register
	 * @param initparams
	 *            initialization arguments for the service or <code>null</code>
	 *            if there are none. This argument is used by the underlying
	 *            servlet's <code>ServletConfig</code> object
	 * @throws NamespaceException
	 *             if the registration fails because the alias is already in use
	 * @throws ServletException
	 *             if the service servlet's <code>init</code> method throws an
	 *             exception
	 * @throws java.lang.IllegalArgumentException
	 *             if any of the arguments are invalid
	 */
	//void registerService(final String alias, final IAdaptable service) throws ServletException, NamespaceException;

	/**
	 * Registers a servlet into the URI namespace.
	 * <p>
	 * The alias is the name in the URI namespace of the application at which
	 * the registration will be mapped.
	 * </p>
	 * <p>
	 * The purpose of this method is similar to
	 * {@link HttpService#registerServlet(String, Servlet, Dictionary, HttpContext)}
	 * but adapted to the {@link Application} scope.
	 * </p>
	 * <p>
	 * An alias must begin with slash ('/') or a wildcard followed by a dot
	 * ('*.') and must not end with slash ('/'), with the exception that an
	 * alias of the form &quot;/&quot; is used to denote the root/default alias.
	 * See the OSGi Http Service specification text for details on how HTTP
	 * requests are mapped to servlet and resource registrations. Additionally,
	 * extension aliases are supported. An extension alias begins with a
	 * wildcard followed by a dot followed by the file extension. The servlet is
	 * called for every matching file extension.
	 * </p>
	 * <p>
	 * The application will call the servlet's <code>init</code> method before
	 * returning.
	 * </p>
	 * <p>
	 * All servlets registered with the same application object will share the
	 * same {@link ServletContext}.
	 * </p>
	 * <p>
	 * The specified {@link Servlet} object must be created by an OSGi bundle.
	 * It will be the bundle that is responsible for the registration. The
	 * application will monitor the bundle so that the registrations will be
	 * automatically (see {@link #unregister(String)} unregistered) when the
	 * bundle is stopped.
	 * </p>
	 * 
	 * @param alias
	 *            name in the URI namespace at which the servlet is registered
	 * @param servlet
	 *            the servlet object to register
	 * @param initparams
	 *            initialization arguments for the servlet or <code>null</code>
	 *            if there are none. This argument is used by the servlet's
	 *            <code>ServletConfig</code> object
	 * @throws NamespaceException
	 *             if the registration fails because the alias is already in use
	 * @throws javax.servlet.ServletException
	 *             if the servlet's <code>init</code> method throws an
	 *             exception, or the given servlet object has already been
	 *             registered at a different alias
	 * @throws java.lang.IllegalArgumentException
	 *             if any of the arguments are invalid
	 */
	void registerServlet(final String alias, final Servlet servlet, final Map<String, String> initparams) throws ServletException, NamespaceException;

	/**
	 * Unregisters a previous registration done by
	 * {@link #registerServlet(String, Servlet, Dictionary)} or
	 * {@link #registerResources(String, String, IResourceProvider)} methods.
	 * <p>
	 * After this call, the registered alias in the URI name-space will no
	 * longer be available. If the registration was for a servlet, the
	 * application must call the <code>destroy</code> method of the servlet
	 * before returning.
	 * </p>
	 * <p>
	 * If a bundle which was specified during the registration is stopped
	 * without calling {@link #unregister(String)} then the application
	 * automatically unregisters the registration of that bundle. However, if
	 * the registration was for a servlet, the <code>destroy</code> method of
	 * the servlet will not be called in this case since the bundle may be
	 * stopped. {@link #unregister(String)} must be explicitly called to cause
	 * the <code>destroy</code> method of the servlet to be called. This can be
	 * done in the <code>BundleActivator.stop</code> method of the bundle
	 * registering the servlet.
	 * </p>
	 * 
	 * @param alias
	 *            name in the URI name-space of the registration to unregister
	 * @throws java.lang.IllegalArgumentException
	 *             if there is no registration for the alias or the calling
	 *             bundle was not the bundle which registered the alias.
	 */
	void unregister(final String alias);
}
