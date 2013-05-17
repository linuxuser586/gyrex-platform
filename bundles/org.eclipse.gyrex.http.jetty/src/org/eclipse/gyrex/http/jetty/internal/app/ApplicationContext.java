/*******************************************************************************
 * Copyright (c) 2010, 2013 AGETO Service GmbH and others.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.gyrex.http.application.ApplicationException;
import org.eclipse.gyrex.http.application.context.IApplicationContext;
import org.eclipse.gyrex.http.application.context.IResourceProvider;
import org.eclipse.gyrex.http.application.context.NamespaceException;
import org.eclipse.gyrex.http.jetty.internal.HttpJettyActivator;
import org.eclipse.gyrex.http.jetty.internal.JettyDebug;

import org.eclipse.jetty.http.PathMap;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletMapping;
import org.eclipse.jetty.util.ArrayUtil;
import org.eclipse.jetty.util.StringUtil;
import org.eclipse.jetty.util.URIUtil;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IApplicationContext} implementation.
 * <p>
 * The context acts as a bridge between the application and the various Jetty
 * handlers.
 * </p>
 */
public class ApplicationContext implements IApplicationContext {

	private final class BundleResourceMonitor implements BundleListener {
		private final String alias;
		private BundleContext bundleContext;
		private final long bundleId;

		BundleResourceMonitor(final String alias, final BundleContext bundleContext) {
			this.alias = alias;
			this.bundleContext = bundleContext;
			bundleId = bundleContext.getBundle().getBundleId();
		}

		void activate() {
			try {
				bundleContext.addBundleListener(this);
			} catch (final Exception e) {
				// ignore
			}
		}

		@Override
		public void bundleChanged(final BundleEvent event) {
			if (bundleId != event.getBundle().getBundleId())
				// ignore events for different bundles
				// (clarify if we should ever get those here; I got a stacktrace once that indicates this)
				return;
			if (event.getType() == Bundle.STOPPING) {
				try {
					unregister(alias);
				} catch (final Exception e) {
					// ignore
				} finally {
					remove();
				}
			}
		}

		void remove() {
			try {
				bundleContext.removeBundleListener(this);
			} catch (final Exception e) {
				// ignore
			}
			bundleContext = null;
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(ApplicationContext.class);

	/** applicationContextHandler */
	private final ApplicationHandler applicationHandler;

	private final Lock registryModificationLock = new ReentrantLock();
	private final Set<String> registeredAliases = new HashSet<String>();
	private final Map<String, BundleResourceMonitor> bundleMonitors = new HashMap<String, BundleResourceMonitor>();

	/**
	 * Creates a new instance.
	 * 
	 * @param applicationHandler
	 */
	public ApplicationContext(final ApplicationHandler applicationHandler) {
		this.applicationHandler = applicationHandler;
	}

	private void addBundleResourceMonitor(final String alias, final Class<?> bundleClass) {
		final Bundle bundle = FrameworkUtil.getBundle(bundleClass);
		if (bundle != null) {
			final BundleContext bundleContext = bundle.getBundleContext();
			if (null != bundleContext) {
				final BundleResourceMonitor monitor = new BundleResourceMonitor(alias, bundleContext);
				bundleMonitors.put(alias, monitor);
				monitor.activate();
			}
		}
	}

	@Override
	public Map<String, String> getInitProperties() {
		return applicationHandler.getInitParams();
	}

	@Override
	public ServletContext getServletContext() {
		return applicationHandler.getServletContext();
	}

	@Override
	public boolean handleRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ApplicationException {
		return applicationHandler.getDelegateHandler().handleApplicationRequest(request, response);
	}

	private void initializeServletIfNecessary(final String alias, final ServletHolder holder) throws ServletException {
		if (applicationHandler.getServletHandler().isStarted() || applicationHandler.getServletHandler().isStarting()) {
			try {
				holder.start();
				holder.initialize();
			} catch (final Exception e) {
				// attempt a clean unregister
				try {
					unregister(alias);
				} catch (final Exception e2) {
					if (JettyDebug.debug) {
						LOG.debug("Exception during cleanup of failed registration.", e2);
					}
				}
				// fail
				throw new ServletException(String.format("Error starting servlet. %s", e.getMessage()), e);
			}
		}
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
		if (null == alias)
			throw new IllegalArgumentException("alias must not be null");
		if (!alias.startsWith(URIUtil.SLASH) && !alias.startsWith("*."))
			throw new IllegalArgumentException("alias must start with '/' or '*.'");
		if (alias.endsWith("/*"))
			throw new IllegalArgumentException("alias must not end with '/*'");
		if (!URIUtil.SLASH.equals(alias) && StringUtil.endsWithIgnoreCase(alias, URIUtil.SLASH))
			throw new IllegalArgumentException("alias must not end with '/'");

		// use extension alias as is
		if (alias.startsWith("*."))
			return alias;

		// make all other aliases implicit to simulate OSGi prefix matching
		// note, '/' must also be made implicit so that internally it matches as '/*'
		return URIUtil.SLASH.equals(alias) ? "/*" : alias.concat("/*");
	}

	private void registerAlias(final String alias) throws NamespaceException {
		if (registeredAliases.contains(alias)) {
			if (JettyDebug.applicationContext) {
				LOG.debug("{} alias already taken: {}", new Object[] { this, alias, new Exception("Call Stack") });
			}
			throw new NamespaceException(alias);
		}
		registeredAliases.add(alias);
	}

	@Override
	public void registerFilter(final String alias, final Filter filter, final Map<String, String> initparams) throws ServletException {
		final String pathSpec = normalizeAliasToPathSpec(alias);

		if (JettyDebug.applicationContext) {
			LOG.debug("{} registering filter: {} (normalized to {}) --> {}", new Object[] { this, alias, pathSpec, filter });
		}

		// synchronize access to registry modifications
		registryModificationLock.lock();
		try {
			// TODO: track bundle de-activation
			//addBundleResourceMonitor(filter);

			// create holder
			final FilterHolder holder = new FilterHolder(filter);
			if (null != initparams) {
				holder.setInitParameters(initparams);
			}

			// register servlet
			applicationHandler.getServletHandler().addFilterWithMapping(holder, pathSpec, null);

		} finally {
			registryModificationLock.unlock();
		}
	}

	@Override
	public void registerResources(final String alias, final String name, IResourceProvider provider) throws NamespaceException {
		final String pathSpec = normalizeAliasToPathSpec(alias);

		// create dynamic resource provider based on the calling bundle if non was specified
		if (provider == null) {
			final Bundle callingBundle = HttpJettyActivator.getInstance().getCallingBundle();
			if (null == callingBundle)
				throw new IllegalArgumentException("unable to determine the calling bundle; please specify a non-null resource provider");
			provider = new BundleResourceProvider(callingBundle);
		}

		if (JettyDebug.applicationContext) {
			LOG.debug("{} registering resource: {} (normalized to {}) --> {}", new Object[] { this, alias, pathSpec, provider });
		}

		// synchronize access to registry modifications
		registryModificationLock.lock();
		try {
			// reserve alias
			registerAlias(alias);

			// track bundle de-activation
			addBundleResourceMonitor(alias, provider.getClass());

			// register resource provider
			applicationHandler.addResource(pathSpec, new ResourceProviderHolder(name, provider));

			// register a resource servlet to make the resources accessible
			final ServletHolder holder = ApplicationResourceServlet.newHolder(applicationHandler);
			applicationHandler.getServletHandler().addServletWithMapping(holder, pathSpec);

			// initialize resource servlet if application already started
			try {
				initializeServletIfNecessary(alias, holder);
			} catch (final ServletException e) {
				throw new IllegalStateException(String.format("Unhandled error registering resources. %s", e.getMessage()), e);
			}
		} finally {
			registryModificationLock.unlock();
		}
	}

	@Override
	public void registerServlet(final String alias, final Class<? extends Servlet> servletClass, final Map<String, String> initparams) throws ServletException, NamespaceException {
		final String pathSpec = normalizeAliasToPathSpec(alias);

		if (JettyDebug.applicationContext) {
			LOG.debug("{} registering servlet: {} (normalized to {}) --> {}", new Object[] { this, alias, pathSpec, servletClass });
		}

		// synchronize access to registry modifications
		registryModificationLock.lock();
		try {
			// reserve alias
			registerAlias(alias);

			// track bundle de-activation
			addBundleResourceMonitor(alias, servletClass);

			// create holder
			final ServletHolder holder = new ServletHolder(servletClass);
			if (null != initparams) {
				holder.setInitParameters(initparams);
			}

			// register servlet
			applicationHandler.getServletHandler().addServletWithMapping(holder, pathSpec);

			// initialize servlet if application already started
			initializeServletIfNecessary(alias, holder);
		} finally {
			registryModificationLock.unlock();
		}
	}

	@Override
	public void registerServlet(final String alias, final Servlet servlet, final Map<String, String> initparams) throws ServletException, NamespaceException {
		final String pathSpec = normalizeAliasToPathSpec(alias);

		if (JettyDebug.applicationContext) {
			LOG.debug("{} registering servlet: {} (normalized to {}) --> {}", new Object[] { this, alias, pathSpec, servlet });
		}

		// synchronize access to registry modifications
		registryModificationLock.lock();
		try {
			// reserve alias
			registerAlias(alias);

			// track bundle de-activation
			addBundleResourceMonitor(alias, servlet.getClass());

			// create holder
			final ServletHolder holder = new ServletHolder(servlet);
			if (null != initparams) {
				holder.setInitParameters(initparams);
			}

			// register servlet
			applicationHandler.getServletHandler().addServletWithMapping(holder, pathSpec);

			// initialize servlet if application already started
			initializeServletIfNecessary(alias, holder);
		} finally {
			registryModificationLock.unlock();
		}

	}

	private void removeBundleResourceMonitor(final String alias) {
		final BundleResourceMonitor monitor = bundleMonitors.remove(alias);
		if (monitor != null) {
			monitor.remove();
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ApplicationContext [").append(applicationHandler.getApplicationId()).append("]");
		return builder.toString();
	}

	@Override
	public void unregister(final Filter filter) {
		if (JettyDebug.applicationContext) {
			LOG.debug("{} unregistering filter: {}", new Object[] { this, filter });
		}

		registryModificationLock.lock();
		try {
			// TODO: remove bundle monitor
			//removeBundleResourceMonitor(filter);

			// collect list of remaining filters and filters to remove
			final ApplicationServletHandler servletHandler = applicationHandler.getServletHandler();
			final FilterHolder[] filters = servletHandler.getFilters();
			final List<FilterHolder> newfilters = new ArrayList<FilterHolder>(filters.length);
			final Set<FilterHolder> toRemove = new HashSet<FilterHolder>(filters.length);
			final Set<String> toRemoveNames = new HashSet<String>(filters.length);
			for (final FilterHolder filterHolder : filters) {
				if (filterHolder.getFilter() == filter) {
					toRemove.add(filterHolder);
					toRemoveNames.add(filterHolder.getName());
				} else {
					newfilters.add(filterHolder);
				}
			}

			// sanity check
			if (toRemove.isEmpty())
				throw new IllegalStateException("filter '" + filter + "' not found");

			// collect remaining mappings
			final FilterMapping[] mappings = servletHandler.getFilterMappings();
			final List<FilterMapping> newMappings = new ArrayList<FilterMapping>(mappings.length);
			for (final FilterMapping mapping : mappings) {
				final String filterName = mapping.getFilterName();
				if (!toRemove.contains(filterName)) {
					newMappings.add(mapping);
				}
			}

			// update mappings and servlets
			servletHandler.setFilters(newfilters.toArray(new FilterHolder[newfilters.size()]));
			servletHandler.setFilterMappings(newMappings.toArray(new FilterMapping[newMappings.size()]));

			// stop removed filters
			for (final FilterHolder filterHolder : toRemove) {
				try {
					filterHolder.doStop();
				} catch (final Exception e) {
					// ignore
				}
			}
		} finally {
			registryModificationLock.unlock();
		}
	}

	@Override
	public void unregister(final String alias) {
		final String pathSpec = normalizeAliasToPathSpec(alias);

		if (JettyDebug.applicationContext) {
			LOG.debug("{} unregistering: {} (normalized to {})", new Object[] { this, alias, pathSpec });
		}

		registryModificationLock.lock();
		try {
			unregisterAlias(alias);

			// remove bundle monitor
			removeBundleResourceMonitor(alias);

			// unregister resources provider
			applicationHandler.removeResource(pathSpec);

			// collect list of new mappings and remaining servlets
			final ApplicationServletHandler servletHandler = applicationHandler.getServletHandler();
			boolean removedSomething = false;
			final ServletMapping[] mappings = servletHandler.getServletMappings();
			final List<ServletMapping> newMappings = new ArrayList<ServletMapping>(mappings.length);
			final Set<String> mappedServlets = new HashSet<String>(mappings.length);
			for (final ServletMapping mapping : mappings) {
				final String[] pathSpecs = mapping.getPathSpecs();
				for (final String spec : pathSpecs) {
					if (pathSpec.equals(spec)) {
						mapping.setPathSpecs(ArrayUtil.removeFromArray(mapping.getPathSpecs(), spec));
						removedSomething = true;
					}
				}
				if (mapping.getPathSpecs().length > 0) {
					newMappings.add(mapping);
					mappedServlets.add(mapping.getServletName());
				}
			}

			// sanity check
			if (!removedSomething)
				throw new IllegalStateException("alias '" + alias + "' registered but nothing removed");

			// find servlets to remove
			final ServletHolder[] servlets = servletHandler.getServlets();
			final List<ServletHolder> servletsToRemove = new ArrayList<ServletHolder>(servlets.length);
			final List<ServletHolder> newServlets = new ArrayList<ServletHolder>(servlets.length);
			for (final ServletHolder servlet : servlets) {
				if (!mappedServlets.contains(servlet.getName())) {
					servletsToRemove.add(servlet);
				} else {
					newServlets.add(servlet);
				}
			}

			// update mappings and servlets
			servletHandler.setServlets(newServlets.toArray(new ServletHolder[newServlets.size()]));
			servletHandler.setServletMappings(newMappings.toArray(new ServletMapping[newMappings.size()]));

			// stop removed servlets
			for (final ServletHolder servlet : servletsToRemove) {
				try {
					servlet.doStop();
				} catch (final Exception e) {
					// ignore
				}
			}
		} finally {
			registryModificationLock.unlock();
		}
	}

	private void unregisterAlias(final String alias) {
		if (!registeredAliases.contains(alias))
			throw new IllegalStateException("alias '" + alias + "' not registered");
		registeredAliases.remove(alias);
	}
}