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
package org.eclipse.cloudfree.http.application.servicesupport;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Map;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.cloudfree.http.application.Application;
import org.eclipse.cloudfree.http.application.ApplicationException;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

/**
 * Provides support to {@link Application applications} for dynamic servlet and
 * resource registration similar to {@link HttpService}.
 * <p>
 * The {@link IApplicationServiceSupport} provides a dynamic registry for
 * servlets and resources. It allows to built and expose application services
 * which are similar to an OSGi Http Service.
 * </p>
 * <p>
 * It's important to know that an {@link Application} does not implement the
 * {@link HttpService} interface because an application must
 * <strong>not</strong> made available as an OSGi Http Service. This would
 * conflict with the Http Service consumed by the CloudFree Platform itself and
 * result in recursive registration. Therefore, implementors must not export
 * application services using the OSGi {@link HttpService} interface.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IApplicationServiceSupport {

	/**
	 * Maps a file to a MIME type by asking all registered mime type providers.
	 * 
	 * @param file
	 *            determine the MIME type for this file
	 * @return MIME type (e.g. text/html) of the file or <code>null</code> to
	 *         indicate that the platform should determine the MIME type itself
	 * @see ServletContext#getMimeType(String)
	 * @see HttpContext#getMimeType(String)
	 */
	String getMimeType(String file);

	/**
	 * Maps a resource path to a URL by asking all registered resource
	 * providers.
	 * 
	 * @param path
	 *            a <code>String</code> specifying the path to the resource
	 * @return URL to the resource located at the named path, or
	 *         <code>null</code> if there is no resource at that path
	 * @throws MalformedURLException
	 *             if the pathname is not given in the correct form
	 */
	public URL getResource(final String path) throws MalformedURLException;

	/**
	 * Returns a directory-like listing of all the paths to resources within the
	 * application whose longest sub-path matches the supplied path argument.
	 * <p>
	 * Called by the platform to receive a directory-like listing. Typically,
	 * the platform will call this method to support the {@link ServletContext}
	 * method {@link ServletContext#getResourceAsStream(String)}.
	 * </p>
	 * <p>
	 * The application can control from where resources come. For example,
	 * resources can be mapped to files in the application bundle's persistent
	 * storage area via <code>bundleContext.getDataFile(path).toURL()</code> or
	 * to resources in the application's bundle via
	 * <code>bundle.getEntryPaths(path)</code> or contributed via a third
	 * bundle.
	 * </p>
	 * <p>
	 * The default implementation asks the registered resource providers.
	 * </p>
	 * 
	 * @param path
	 *            the partial path used to match the resources, which must start
	 *            with a <code>/</code>
	 * @return a Set containing the directory listing, or <code>null</code> if
	 *         there are no resources in the web application whose path begins
	 *         with the supplied path.
	 */
	Set getResourcePaths(final String path);

	/**
	 * Provides access to an application specific {@link ServletContext}.
	 * <p>
	 * The application servlet context is created and maintained by the
	 * CloudFree platform.
	 * </p>
	 * 
	 * @return the servlet context (maybe <code>null</code> if the application
	 *         is no longer active)
	 */
	public ServletContext getServletContext();

	/**
	 * Handles the request by matching the request's path info against the
	 * registered resources.
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
	 * @exception ApplicationException
	 *                if an exception occurs that interferes with the
	 *                application's normal operation
	 * @exception IOException
	 *                if an input or output exception occurs
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
	 * {@link HttpService#registerServlet(String, Servlet, Dictionary, HttpContext)}
	 * but adapted to the {@link Application} scope.
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
	 * The application will call the {@link IResourceProvider} argument to map
	 * resource names to URLs and MIME types.
	 * </p>
	 * <p>
	 * The specified {@link IResourceProvider} object must be created by an OSGi
	 * bundle. It will be the bundle that is responsible for the registration.
	 * The application will monitor the bundle so that the registrations will be
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
	 * An alias must begin with slash ('/') and must not end with slash ('/'),
	 * with the exception that an alias of the form &quot;/&quot; is used to
	 * denote the root alias. See the OSGi Http Service specification text for
	 * details on how HTTP requests are mapped to servlet and resource
	 * registrations.
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
