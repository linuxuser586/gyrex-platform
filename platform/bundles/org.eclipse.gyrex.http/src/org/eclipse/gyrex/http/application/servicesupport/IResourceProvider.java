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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import javax.servlet.ServletContext;

import org.eclipse.cloudfree.http.application.Application;

/**
 * This interface defines methods that the application may call to get access to
 * registered resources.
 * <p>
 * This interface is typically implemented by clients providing resources to an
 * application. It can be contributed to an application using its
 * {@link IApplicationServiceSupport}.
 * </p>
 */
public interface IResourceProvider extends IMimeTypeProvider {

	/**
	 * Maps a resource path to a URL.
	 * <p>
	 * Called by the application to map a resource path to a URL. For servlet
	 * registrations, the application will call this method to support the
	 * {@link ServletContext} methods {@link ServletContext#getResource(String)}
	 * and {@link ServletContext#getResourceAsStream(String)}. For resource
	 * registrations, the application will call this method to locate the
	 * resource.
	 * </p>
	 * 
	 * @param path
	 *            a <code>String</code> specifying the path to the resource
	 * @return URL to the resource located at the named path, or
	 *         <code>null</code> if there is no resource at that path
	 * @throws MalformedURLException
	 *             if the pathname is not given in the correct form
	 * @see Application#getResource(String)
	 */
	public abstract URL getResource(final String path) throws MalformedURLException;

	/**
	 * Returns a directory-like listing of all the paths to resources of the
	 * provider whose longest sub-path matches the supplied path argument.
	 * <p>
	 * Called by the application to receive a directory-like listing. Typically,
	 * the application will call this method to support the
	 * {@link ServletContext} method
	 * {@link ServletContext#getResourcePaths(String)}.
	 * </p>
	 * 
	 * @param path
	 *            the partial path used to match the resources, which must start
	 *            with a <code>/</code>
	 * @return a Set containing the directory listing, or null if there are no
	 *         resources in the web application whose path begins with the
	 *         supplied path.
	 * @see Application#getResourcePaths(String)
	 */
	public abstract Set getResourcePaths(final String path);

}
