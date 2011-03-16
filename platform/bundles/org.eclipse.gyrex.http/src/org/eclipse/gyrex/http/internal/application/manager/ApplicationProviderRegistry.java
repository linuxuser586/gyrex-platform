/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.internal.application.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.gyrex.http.application.provider.ApplicationProvider;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A registry for tracking application providers.
 * 
 * @TODO we need to support providers in multiple versions
 */
public class ApplicationProviderRegistry extends ServiceTracker<ApplicationProvider, ApplicationProvider> {

	private final ConcurrentMap<String, ApplicationProviderRegistration> providersById = new ConcurrentHashMap<String, ApplicationProviderRegistration>(1);

	/**
	 * Creates a new instance.
	 */
	public ApplicationProviderRegistry(final BundleContext context) {
		super(context, ApplicationProvider.class, null);
	}

	@Override
	public ApplicationProvider addingService(final ServiceReference<ApplicationProvider> reference) {
		// get service
		final ApplicationProvider provider = super.addingService(reference);

		// create registration
		final ApplicationProviderRegistration registration = new ApplicationProviderRegistration(reference, provider);
		providersById.putIfAbsent(provider.getId(), registration);

		return provider;
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

	public Map<String, ApplicationProviderRegistration> getRegisteredProviders() {
		// return a copy
		return new HashMap<String, ApplicationProviderRegistration>(providersById);
	}

	@Override
	public void removedService(final ServiceReference<ApplicationProvider> reference, final ApplicationProvider provider) {
		// remove provider registration
		final ApplicationProviderRegistration providerRegistration = providersById.remove(provider.getId());
		if (null != providerRegistration) {
			// unmount and destroy all applications bound to the provider
			providerRegistration.destroy();
		}

		// unget the service
		super.removedService(reference, provider);
	}
}
