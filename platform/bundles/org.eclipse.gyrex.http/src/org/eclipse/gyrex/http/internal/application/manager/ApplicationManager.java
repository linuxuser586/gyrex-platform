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
package org.eclipse.gyrex.http.internal.application.manager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.http.application.manager.ApplicationRegistrationException;
import org.eclipse.gyrex.http.application.manager.IApplicationManager;
import org.eclipse.gyrex.http.application.manager.MountConflictException;
import org.eclipse.gyrex.http.application.provider.ApplicationProvider;
import org.eclipse.gyrex.http.internal.application.gateway.IHttpGateway;
import org.eclipse.gyrex.http.internal.application.gateway.IUrlRegistry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The application manager.
 * 
 * @TODO we need to support providers in multiple versions
 */
public class ApplicationManager implements IApplicationManager, ServiceTrackerCustomizer {

	private final BundleContext context;
	private final ServiceTracker providerTracker;
	private final ConcurrentMap<String, ApplicationProviderRegistration> providersById = new ConcurrentHashMap<String, ApplicationProviderRegistration>(1);
	private final ConcurrentMap<String, ApplicationRegistration> applicationsById = new ConcurrentHashMap<String, ApplicationRegistration>(1);
	private final IUrlRegistry urlRegistry;
	private final IHttpGateway httpGateway;
	private ServiceRegistration serviceRegistration;

	public ApplicationManager(final BundleContext context, final IHttpGateway httpGateway) {
		this.context = context;
		this.httpGateway = httpGateway;
		providerTracker = new ServiceTracker(context, ApplicationProvider.class.getName(), this);
		urlRegistry = httpGateway.getUrlRegistry(this);
	}

	@Override
	public Object addingService(final ServiceReference reference) {
		final Object service = context.getService(reference);
		if (service instanceof ApplicationProvider) {
			final ApplicationProvider provider = (ApplicationProvider) service;
			final ApplicationProviderRegistration registration = new ApplicationProviderRegistration(reference, provider);
			providersById.putIfAbsent(provider.getId(), registration);
		}
		return service;
	}

	/**
	 * Closes the manager and releases all resources held by it.
	 */
	public void close() {
		providerTracker.close();
		serviceRegistration.unregister();
		serviceRegistration = null;
	}

	/**
	 * Returns the registration for the specified application id.
	 * 
	 * @param applicationId
	 *            the application id
	 * @return the application registration, or <code>null</code> if no
	 *         application is registered (or the registration has been removed)
	 */
	public ApplicationRegistration getApplicationRegistration(final String applicationId) {
		return applicationsById.get(applicationId);
	}

	/**
	 * Returns a provider registration.
	 * 
	 * @param providerId
	 *            the provider id
	 * @return the provider registration, or <code>null</code> if no such
	 *         provider is registered (or was unregistered meanwhile)
	 */
	public ApplicationProviderRegistration getProviderRegistration(final String providerId) {
		return providersById.get(providerId);
	}

	/**
	 * Returns the ApplicationMountRegistry.
	 * 
	 * @return the ApplicationMountRegistry
	 */
	private IUrlRegistry getUrlRegistry() {
		if (null == urlRegistry) {
			throw new IllegalStateException("http gateway inactive");
		}
		return urlRegistry;
	}

	@Override
	public void modifiedService(final ServiceReference reference, final Object service) {
		// nothing
	}

	@Override
	public void mount(final String url, final String applicationId) throws MountConflictException, MalformedURLException {
		// parse the url
		final URL parsedUrl = parseAndVerifyUrl(url);

		// register url
		final String existingApplicationId = getUrlRegistry().registerIfAbsent(parsedUrl, applicationId);
		if (null != existingApplicationId) {
			throw new MountConflictException(url);
		}
	}

	/**
	 * Opens the manager.
	 */
	public void open() {
		providerTracker.open();

		final Dictionary<String, Object> props = new Hashtable<String, Object>(2);
		props.put(Constants.SERVICE_VENDOR, "Eclipse Gyrex");
		props.put(Constants.SERVICE_DESCRIPTION, "Application management service for gateway " + httpGateway.getName());
		serviceRegistration = context.registerService(IApplicationManager.class.getName(), this, props);
	}

	private URL parseAndVerifyUrl(final String url) throws MalformedURLException {
		if (null == url) {
			throw new IllegalArgumentException("url must not be null");
		}

		// parse the url
		final URL parsedUrl = new URL(url);

		// verify protocol
		final String protocol = parsedUrl.getProtocol();
		if (!(protocol.equals("http") || protocol.equals("https"))) {
			throw new IllegalArgumentException("url '" + url + "' must start with 'http://' or 'https://'");
		}
		return parsedUrl;
	}

	@Override
	public void register(final String applicationId, final String providerId, final IRuntimeContext context, final Map<String, String> properties) throws ApplicationRegistrationException {
		if (null == applicationId) {
			throw new IllegalArgumentException("application id must not be null");
		}
		if (null == providerId) {
			throw new IllegalArgumentException("url application provider id not be null");
		}
		if (null == context) {
			throw new IllegalArgumentException("context must not be null");
		}
		final ApplicationRegistration applicationRegistration = new ApplicationRegistration(applicationId, providerId, context, properties, this);
		final ApplicationRegistration existing = applicationsById.putIfAbsent(applicationId.intern(), applicationRegistration);
		if (null != existing) {
			throw new ApplicationRegistrationException(applicationId);
		}

	}

	@Override
	public void removedService(final ServiceReference reference, final Object service) {
		if (service instanceof ApplicationProvider) {
			removeProvider((ApplicationProvider) service);
		}

		// unget the service
		context.ungetService(reference);
	}

	private void removeProvider(final ApplicationProvider provider) {
		// remove provider registration
		final ApplicationProviderRegistration providerRegistration = providersById.remove(provider.getId());
		if (null == providerRegistration) {
			return;
		}

		// unmount and destroy all applications bound to the provider
		providerRegistration.destroy();
	}

	@Override
	public void unmount(final String url) throws MalformedURLException, IllegalArgumentException, IllegalStateException {
		// parse the url
		final URL parsedUrl = parseAndVerifyUrl(url);

		// remove
		final String applicationId = getUrlRegistry().unregister(parsedUrl);

		// throw IllegalStateException if nothing was removed
		if (null == applicationId) {
			throw new IllegalStateException("no application was mounted for url '" + parsedUrl.toExternalForm() + "' (submitted url was '" + url + "')");
		}
	}

	@Override
	public void unregister(final String applicationId) {
		final ApplicationRegistration applicationRegistration = applicationsById.remove(applicationId);
		if (applicationRegistration != null) {
			try {
				applicationRegistration.destroy();
			} finally {
				getUrlRegistry().applicationUnregistered(applicationId);
			}
		}
	}

}
