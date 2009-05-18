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
package org.eclipse.gyrex.model.common.internal.registry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;


import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.model.common.internal.ModelActivator;
import org.eclipse.gyrex.model.common.provider.ModelProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The internal model provider registry.
 */
public class ModelProviderRegistry implements ServiceTrackerCustomizer {

	private final AtomicReference<BundleContext> contextRef = new AtomicReference<BundleContext>();
	private ServiceTracker tracker;

	private final ConcurrentMap<String, ManagerRegistration> registrations = new ConcurrentHashMap<String, ManagerRegistration>();

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
	 */
	@Override
	public Object addingService(final ServiceReference reference) {
		final ModelProvider provider = (ModelProvider) contextRef.get().getService(reference);
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

	private void registerProvider(final ModelProvider provider, final Bundle bundle) {
		if (null == bundle) {
			return;
		}

		for (final Class<?> manager : provider.getProvidedManagers()) {
			ManagerRegistration registration = registrations.putIfAbsent(manager.getName(), new ManagerRegistration(manager.getName()));
			if (null == registration) {
				// we have a new registration
				registration = registrations.get(manager.getName());
			}

			// add provider
			registration.add(manager, provider, bundle);

			// in any case, re-register with the adapter manager
			ModelActivator.getAdapterManager().unregisterAdapters(registration);
			ModelActivator.getAdapterManager().registerAdapters(registration, IRuntimeContext.class);
		}

	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	@Override
	public void removedService(final ServiceReference reference, final Object service) {
		final ModelProvider provider = (ModelProvider) service;
		if (null != provider) {
			unregisterProvider(provider, reference.getBundle());
		}
		contextRef.get().ungetService(reference);
	}

	public void start(final BundleContext context) {
		if (!contextRef.compareAndSet(null, context)) {
			throw new IllegalStateException("already/still active");
		}

		tracker = new ServiceTracker(context, ModelProvider.class.getName(), this);
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

	private void unregisterProvider(final ModelProvider provider, final Bundle bundle) {
		for (final Class<?> manager : provider.getProvidedManagers()) {
			final ManagerRegistration registration = registrations.get(manager.getName());
			if (null != registration) {
				// remove provider
				registration.remove(manager, provider, bundle);

				// re-register with the adapter manager (to flush it)
				if (!registration.isEmpty()) {
					ModelActivator.getAdapterManager().unregisterAdapters(registration);
					ModelActivator.getAdapterManager().registerAdapters(registration, IRuntimeContext.class);
				} else {
					// un-register from the adapter manager
					if (registrations.remove(manager.getName(), registration)) {
						ModelActivator.getAdapterManager().unregisterAdapters(registration);
					}
				}

			}
		}

	}
}
