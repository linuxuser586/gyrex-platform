/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.jetty.internal.app;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.gyrex.http.application.ApplicationException;
import org.eclipse.gyrex.http.application.context.IApplicationContext;
import org.eclipse.gyrex.http.application.context.IResourceProvider;
import org.eclipse.gyrex.http.application.context.NamespaceException;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.http.PathMap;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.LazyList;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.resource.Resource;

public class ApplicationContext implements IApplicationContext {

	/** applicationContextHandler */
	private final ApplicationContextHandler applicationContextHandler;

	private final Lock registryModificationLock = new ReentrantLock();
	private final Set<String> registeredAliases = new HashSet<String>();

	/**
	 * Creates a new instance.
	 * 
	 * @param applicationContextHandler
	 */
	ApplicationContext(final ApplicationContextHandler applicationContextHandler) {
		this.applicationContextHandler = applicationContextHandler;
	}

	@Override
	public String getMimeType(final String file) {
		final MimeTypes mimeTypes = applicationContextHandler.getMimeTypes();
		if (mimeTypes == null) {
			return null;
		}
		final Buffer mime = mimeTypes.getMimeByExtension(file);
		if (mime != null) {
			return mime.toString();
		}
		return null;
	}

	@Override
	public URL getResource(final String path) throws MalformedURLException {
		final Resource resource = applicationContextHandler.getResource(path);
		if ((null != resource) && resource.exists()) {
			return resource.getURL();
		}
		return null;
	}

	@Override
	public Set getResourcePaths(final String path) {
		return applicationContextHandler.getResourcePaths(path);
	}

	@Override
	public ServletContext getServletContext() {
		return applicationContextHandler.getServletContext();
	}

	@Override
	public boolean handleRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ApplicationException {
		// call the servlet handler
		return applicationContextHandler.applicationServletHandler.handleDelegatedRequest(request, response);
	}

	/**
	 * Checks and normalizes an OSGi alias to the path spec (as used by Jetty's
	 * {@link PathMap}).
	 * 
	 * @param alias
	 *            the alias
	 * @return the path spec
	 * @throws IllegalArgumentException
	 *             if the alias is invalid
	 */
	private String normalizeAliasToPathSpec(final String alias) throws IllegalArgumentException {
		// sanity check alias
		if (null == alias) {
			throw new IllegalArgumentException("alias must not be null");
		}
		if (!alias.startsWith(URIUtil.SLASH) && !alias.startsWith("*.")) {
			throw new IllegalArgumentException("alias must start with '/' or '*.'");
		}
		if (alias.endsWith("/*")) {
			throw new IllegalArgumentException("alias must not end with '/*'");
		}
		if (!URIUtil.SLASH.equals(alias) && StringUtil.endsWithIgnoreCase(alias, URIUtil.SLASH)) {
			throw new IllegalArgumentException("alias must not end with '/'");
		}

		// use extension alias as is
		if (alias.startsWith("*.")) {
			return alias;
		}

		// make all other aliases implicit to simulate OSGi prefix matching
		// note, '/' must also be made implicit so that internally it matches as '/*'
		return URIUtil.SLASH.equals(alias) ? "/*" : alias.concat("/*");
	}

	private void registerAlias(final String alias) throws NamespaceException {
		if (registeredAliases.contains(alias)) {
			throw new NamespaceException(alias);
		}
		registeredAliases.add(alias);
	}

	@Override
	public void registerResources(final String alias, final String name, final IResourceProvider provider) throws NamespaceException {
		final String pathSpec = normalizeAliasToPathSpec(alias);

		// register resource
		registryModificationLock.lock();
		try {
			// reserve alias
			registerAlias(alias);

			// register resource provider
			applicationContextHandler.resourcesMap.put(pathSpec, new ResourceProviderHolder(name, provider));

			// register resource servlet
			applicationContextHandler.applicationServletHandler.addServletWithMapping(ApplicationResourceServlet.newHolder(applicationContextHandler), pathSpec);
		} finally {
			registryModificationLock.unlock();
		}
	}

	@Override
	public void registerServlet(final String alias, final Servlet servlet, final Map<String, String> initparams) throws ServletException, NamespaceException {
		final String pathSpec = normalizeAliasToPathSpec(alias);

		// register servlet
		registryModificationLock.lock();
		try {
			// reserve alias
			registerAlias(alias);

			// create holder
			final ApplicationRegisteredServletHolder holder = new ApplicationRegisteredServletHolder(servlet);
			if (null != initparams) {
				holder.setInitParameters(initparams);
			}

			// register servlet
			applicationContextHandler.applicationServletHandler.addServletWithMapping(holder, pathSpec);
		} finally {
			registryModificationLock.unlock();
		}
	}

	@Override
	public void unregister(final String alias) {
		final String pathSpec = normalizeAliasToPathSpec(alias);

		registryModificationLock.lock();
		try {
			unregisterAlias(alias);

			// unregister resources provider
			applicationContextHandler.resourcesMap.remove(pathSpec);

			// collect list of new mappings and remaining servlets
			boolean removedSomething = false;
			final ServletMapping[] mappings = applicationContextHandler.applicationServletHandler.getServletMappings();
			final List<ServletMapping> newMappings = new ArrayList<ServletMapping>(mappings.length);
			final Set<String> mappedServlets = new HashSet<String>(mappings.length);
			for (final ServletMapping mapping : mappings) {
				final String[] pathSpecs = mapping.getPathSpecs();
				for (final String spec : pathSpecs) {
					if (pathSpec.equals(spec)) {
						mapping.setPathSpecs((String[]) LazyList.removeFromArray(mapping.getPathSpecs(), spec));
						removedSomething = true;
					}
				}
				if (mapping.getPathSpecs().length > 0) {
					newMappings.add(mapping);
					mappedServlets.add(mapping.getServletName());
				}
			}

			// sanity check
			if (!removedSomething) {
				throw new IllegalStateException("alias '" + alias + "' registered but nothing removed");
			}

			// find servlets to remove
			final ServletHolder[] servlets = applicationContextHandler.applicationServletHandler.getServlets();
			final List<ServletHolder> servletsToRemove = new ArrayList<ServletHolder>(servlets.length);
			for (final ServletHolder servlet : servlets) {
				if (!mappedServlets.contains(servlet)) {
					servletsToRemove.add(servlet);
				}
			}

			// update mappings and servlets
			applicationContextHandler.applicationServletHandler.setServlets(mappedServlets.toArray(new ServletHolder[mappedServlets.size()]));
			applicationContextHandler.applicationServletHandler.setServletMappings(newMappings.toArray(new ServletMapping[newMappings.size()]));

			// stop removed servlets
			for (final ServletHolder servlet : servletsToRemove) {
				try {
					servlet.doStop();
				} catch (final Exception e) {
					Log.ignore(e);
				}
			}
		} finally {
			registryModificationLock.unlock();
		}
	}

	private void unregisterAlias(final String alias) {
		if (!registeredAliases.contains(alias)) {
			throw new IllegalStateException("alias '" + alias + "' not registered");
		}
		registeredAliases.remove(alias);
	}
}