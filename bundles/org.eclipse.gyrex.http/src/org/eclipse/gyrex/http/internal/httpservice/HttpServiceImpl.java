/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.internal.httpservice;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.eclipse.equinox.http.servlet.ExtendedHttpService;

import org.eclipse.gyrex.http.application.context.IApplicationContext;
import org.eclipse.gyrex.http.internal.HttpDebug;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link HttpService} implementation backed by {@link IApplicationContext}.
 */
public class HttpServiceImpl implements HttpService, ExtendedHttpService {

	private static final Logger LOG = LoggerFactory.getLogger(HttpServiceImpl.class);

	private final IApplicationContext context;
	private final Bundle bundle;

	private final CopyOnWriteArraySet<String> aliasRegistrations = new CopyOnWriteArraySet<String>();
	private final CopyOnWriteArraySet<Filter> filterRegistrations = new CopyOnWriteArraySet<Filter>();

	private final org.osgi.framework.Filter registrationFilter;

	/**
	 * Creates a new instance.
	 * 
	 * @param registrationFilter
	 * @param applicationServiceSupport
	 * @param bundle2
	 */
	public HttpServiceImpl(final IApplicationContext context, final Bundle bundle, final org.osgi.framework.Filter registrationFilter) {
		this.context = context;
		this.bundle = bundle;
		this.registrationFilter = registrationFilter;
	}

	@Override
	public HttpContext createDefaultHttpContext() {
		return new DefaultHttpContext(bundle);
	}

	/**
	 * Returns the context.
	 * 
	 * @return the context
	 */
	public IApplicationContext getContext() {
		return context;
	}

	private boolean isFiltered(final String alias, final Filter filter) {
		if (null == registrationFilter) {
			return false;
		}

		final Map<String, Object> m = new HashMap<String, Object>(6);
		putBundleProperties(m);
		m.put("type", "filter");
		m.put("alias", alias);
		m.put(Constants.OBJECTCLASS, filter.getClass().getName());
		return registrationFilter.matches(m);
	}

	private boolean isFiltered(final String alias, final Servlet servlet) {
		if (null == registrationFilter) {
			return false;
		}

		final Map<String, Object> m = new HashMap<String, Object>(6);
		putBundleProperties(m);
		m.put("type", "servlet");
		m.put("alias", alias);
		m.put(Constants.OBJECTCLASS, servlet.getClass().getName());
		return registrationFilter.matches(m);
	}

	private boolean isFiltered(final String alias, final String name) {
		if (null == registrationFilter) {
			return false;
		}

		final Map<String, Object> m = new HashMap<String, Object>(6);
		putBundleProperties(m);
		m.put("type", "resource");
		m.put("alias", alias);
		m.put("resource", name);
		return registrationFilter.matches(m);
	}

	private void putBundleProperties(final Map<String, Object> m) {
		m.put("id", new Long(bundle.getBundleId()));
		m.put("location", bundle.getLocation());
		final String name = bundle.getSymbolicName();
		if (name != null) {
			m.put("name", name);
		}
	}

	@Override
	public void registerFilter(final String alias, final Filter filter, final Dictionary initparams, final HttpContext context) throws ServletException, NamespaceException {
		if (isFiltered(alias, filter)) {
			if (HttpDebug.httpService) {
				LOG.debug("Ignoring filtered filter with alias {} (bundle {} ({}), filter {})", new Object[] { alias, bundle.getSymbolicName(), bundle.getBundleId(), filter });
			}
			return;
		}

		if (HttpDebug.httpService) {
			LOG.debug("Registering filter with alias {} (bundle {} ({}), filter {})", new Object[] { alias, bundle.getSymbolicName(), bundle.getBundleId(), filter });
		}
		getContext().registerFilter(alias, filter, toMap(initparams));
		filterRegistrations.add(filter);
	}

	@Override
	public void registerResources(final String alias, final String name, final HttpContext context) throws NamespaceException {
		if (isFiltered(alias, name)) {
			if (HttpDebug.httpService) {
				LOG.debug("Ignoring filtered resource with alias {} (bundle {} ({}), name {})", new Object[] { alias, bundle.getSymbolicName(), bundle.getBundleId(), name });
			}
			return;
		}

		try {
			if (HttpDebug.httpService) {
				LOG.debug("Registering resource with alias {} (bundle {} ({}), name {})", new Object[] { alias, bundle.getSymbolicName(), bundle.getBundleId(), name });
			}
			getContext().registerResources(alias, name, new DefaultResourceProvider(bundle, context));
			aliasRegistrations.add(alias);
		} catch (final org.eclipse.gyrex.http.application.context.NamespaceException e) {
			throw new NamespaceException(alias, e);
		}
	}

	@Override
	public void registerServlet(final String alias, final Servlet servlet, final Dictionary initparams, final HttpContext context) throws ServletException, NamespaceException {
		if (isFiltered(alias, servlet)) {
			if (HttpDebug.httpService) {
				LOG.debug("Ignoring filtered servlet with alias {} (bundle {} ({}), servlet {})", new Object[] { alias, bundle.getSymbolicName(), bundle.getBundleId(), servlet });
			}
			return;
		}

		try {
			if (HttpDebug.httpService) {
				LOG.debug("Registering servlet with alias {} (bundle {} ({}), servlet {})", new Object[] { alias, bundle.getSymbolicName(), bundle.getBundleId(), servlet });
			}
			getContext().registerServlet(alias, servlet, toMap(initparams));
			aliasRegistrations.add(alias);
		} catch (final org.eclipse.gyrex.http.application.context.NamespaceException e) {
			throw new NamespaceException(alias, e);
		}
	}

	private Map<String, String> toMap(final Dictionary initparams) {
		if (initparams == null) {
			return null;
		}

		final HashMap<String, String> initparamsMap = new HashMap<String, String>(initparams.size());
		final Enumeration keys = initparams.keys();
		while (keys.hasMoreElements()) {
			final Object key = keys.nextElement();
			try {
				initparamsMap.put((String) key, (String) initparams.get(key));
			} catch (final ClassCastException e) {
				throw new IllegalArgumentException("invalid key in init properties; only string key and string value supported; key: " + key, e);
			}
		}
		return initparamsMap;
	}

	@Override
	public void unregister(final String alias) {
		if (HttpDebug.httpService) {
			LOG.debug("Unregistering alias {} (bundle {} ({}))", new Object[] { alias, bundle.getSymbolicName(), bundle.getBundleId() });
		}
		getContext().unregister(alias);
		aliasRegistrations.remove(alias);
	}

	/**
	 * Unregisters all aliases registered by that bundle
	 */
	public void unregisterAll() {
		if (HttpDebug.httpService) {
			LOG.debug("Unregistering all known registrations for bundle {} ({})", new Object[] { bundle.getSymbolicName(), bundle.getBundleId() });
		}
		for (final Filter filter : filterRegistrations) {
			try {
				unregisterFilter(filter);
			} catch (final Exception e) {
				// ignore
			}
		}
		for (final String alias : aliasRegistrations) {
			try {
				unregister(alias);
			} catch (final Exception e) {
				// ignore
			}
		}
	}

	@Override
	public void unregisterFilter(final Filter filter) {
		if (HttpDebug.httpService) {
			LOG.debug("Unregistering filter (bundle {} ({}), filter {})", new Object[] { bundle.getSymbolicName(), bundle.getBundleId(), filter });
		}
		getContext().unregister(filter);
		filterRegistrations.remove(filter);
	}
}
