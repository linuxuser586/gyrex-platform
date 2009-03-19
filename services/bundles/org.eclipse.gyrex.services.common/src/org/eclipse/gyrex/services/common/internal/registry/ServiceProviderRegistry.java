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
package org.eclipse.gyrex.services.common.internal.registry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;


import org.eclipse.gyrex.common.context.IContext;
import org.eclipse.gyrex.services.common.internal.ServicesActivator;
import org.eclipse.gyrex.services.common.provider.ServiceProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The internal service provider registry.
 */
public class ServiceProviderRegistry implements ServiceTrackerCustomizer {

	private final AtomicReference<BundleContext> contextRef = new AtomicReference<BundleContext>();
	private ServiceTracker tracker;

	private final ConcurrentMap<String, ServiceRegistration> registrations = new ConcurrentHashMap<String, ServiceRegistration>();

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
	 */
	@Override
	public Object addingService(final ServiceReference reference) {
		final ServiceProvider provider = (ServiceProvider) contextRef.get().getService(reference);
		if (null != provider) {
			registerProvider(provider, reference.getBundle());
		}
		return provider;
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	@Override
	public void modifiedService(final ServiceReference reference, final Object service) {
		// nothing to do currently
	}

	private void registerProvider(final ServiceProvider provider, final Bundle bundle) {
		if (null == bundle) {
			return;
		}

		for (final Class<?> service : provider.getProvidedServices()) {
			ServiceRegistration registration = registrations.putIfAbsent(service.getName(), new ServiceRegistration(service.getName()));
			if (null == registration) {
				// we have a new registration
				registration = registrations.get(service.getName());
			}

			// add provider
			registration.add(service, provider, bundle);

			// in any case, re-register with the adapter service
			ServicesActivator.getAdapterManager().unregisterAdapters(registration);
			ServicesActivator.getAdapterManager().registerAdapters(registration, IContext.class);
		}

	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	@Override
	public void removedService(final ServiceReference reference, final Object service) {
		final ServiceProvider provider = (ServiceProvider) service;
		if (null != provider) {
			unregisterProvider(provider, reference.getBundle());
		}
		contextRef.get().ungetService(reference);
	}

	public void start(final BundleContext context) {
		if (!contextRef.compareAndSet(null, context)) {
			throw new IllegalStateException("already/still active");
		}

		tracker = new ServiceTracker(context, ServiceProvider.class.getName(), this);
		tracker.open();
	}

	public void stop() {
		final BundleContext context = contextRef.getAndSet(null);
		if (null == context) {
			return;
		}
		tracker.close();
		tracker = null;
	}

	private void unregisterProvider(final ServiceProvider provider, final Bundle bundle) {
		for (final Class<?> service : provider.getProvidedServices()) {
			final ServiceRegistration registration = registrations.get(service.getName());
			if (null != registration) {
				// remove provider
				registration.remove(service, provider, bundle);

				// re-register with the adapter service (to flush it)
				if (!registration.isEmpty()) {
					ServicesActivator.getAdapterManager().unregisterAdapters(registration);
					ServicesActivator.getAdapterManager().registerAdapters(registration, IContext.class);
				} else {
					// un-register from the adapter service
					if (registrations.remove(service.getName(), registration)) {
						ServicesActivator.getAdapterManager().unregisterAdapters(registration);
					}
				}

			}
		}

	}
}
