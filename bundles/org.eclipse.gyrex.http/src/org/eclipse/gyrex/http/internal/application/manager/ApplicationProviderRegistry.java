/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.gyrex.http.application.provider.ApplicationProvider;
import org.eclipse.gyrex.http.internal.HttpDebug;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A registry for tracking application providers.
 * 
 * @TODO we need to support providers in multiple versions
 */
public class ApplicationProviderRegistry extends ServiceTracker<ApplicationProvider, ApplicationProvider> {

	public static interface ProviderListener {
		void providerAdded(ApplicationProviderRegistration registration);

		void providerRemoved(ApplicationProviderRegistration providerRegistration);
	}

	private static final Logger LOG = LoggerFactory.getLogger(ApplicationProviderRegistry.class);

	private final ConcurrentMap<String, ApplicationProviderRegistration> providersById = new ConcurrentHashMap<String, ApplicationProviderRegistration>(1);
	private final List<ProviderListener> providerListeners = new CopyOnWriteArrayList<ApplicationProviderRegistry.ProviderListener>();

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
		if (HttpDebug.applicationLifecycle)
			LOG.debug("Registering new ApplicationProvider: {}", provider);

		// create registration
		final ApplicationProviderRegistration registration = new ApplicationProviderRegistration(reference, provider);
		final ApplicationProviderRegistration existingRegistration = providersById.putIfAbsent(provider.getId(), registration);
		if (null != existingRegistration)
			// log at least an error so that we can track this
			LOG.error("Unable to add provider ({}) using id '{}' due to conflict with existing registration ({}). Please open a bug and ask for supporting multiple providers for the same id in different versions.");
		else
			// inform listeners
			for (final ProviderListener l : providerListeners)
				try {
					l.providerAdded(registration);
				} catch (final Exception e) {
					if (HttpDebug.debug)
						LOG.debug("Ignored exception in listener ({})", l, e);
				}

		return provider;
	}

	public void addProviderListener(final ProviderListener listener) {
		providerListeners.add(listener);
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
		if (HttpDebug.applicationLifecycle)
			LOG.debug("Unregistering ApplicationProvider: {}", provider);

		// remove provider registration
		final ApplicationProviderRegistration providerRegistration = providersById.remove(provider.getId());
		if (null != providerRegistration) {
			// unmount and destroy all applications bound to the provider
			providerRegistration.destroy();

			// inform listeners
			for (final ProviderListener l : providerListeners)
				try {
					l.providerRemoved(providerRegistration);
				} catch (final Exception e) {
					if (HttpDebug.debug)
						LOG.debug("Ignored exception in listener ({})", l, e);
				}
		}

		// unget the service
		super.removedService(reference, provider);
	}

	public void removeProviderListener(final ProviderListener listener) {
		providerListeners.remove(listener);
	}
}
