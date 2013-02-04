/*******************************************************************************
 * Copyright (c) 2008, 2012 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context.internal.provider;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.common.lifecycle.IShutdownParticipant;
import org.eclipse.gyrex.context.provider.RuntimeContextObjectProvider;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The internal object provider registry.
 */
public class ObjectProviderRegistry implements IShutdownParticipant {

	/**
	 * service tracker
	 */
	private final class RuntimeContextObjectProviderTracker extends ServiceTracker<RuntimeContextObjectProvider, RuntimeContextObjectProvider> {
		private RuntimeContextObjectProviderTracker(final BundleContext context) {
			super(context, RuntimeContextObjectProvider.class, null);
		}

		@Override
		public RuntimeContextObjectProvider addingService(final ServiceReference<RuntimeContextObjectProvider> reference) {
			final RuntimeContextObjectProvider provider = super.addingService(reference); // get service
			if (null != provider) {
				registerProvider(provider, reference);
			}
			return provider;
		}

		@Override
		public void modifiedService(final ServiceReference<RuntimeContextObjectProvider> reference, final RuntimeContextObjectProvider service) {
			final RuntimeContextObjectProvider provider = service;
			if (null != provider) {
				flushProperties(provider, reference);
			}
		}

		@Override
		public void removedService(final ServiceReference<RuntimeContextObjectProvider> reference, final RuntimeContextObjectProvider service) {
			final RuntimeContextObjectProvider provider = service;
			if (null != provider) {
				unregisterProvider(provider, reference);
			}

			// unget service
			super.removedService(reference, service);
		}
	}

	private final ConcurrentMap<String, TypeRegistration> registrations = new ConcurrentHashMap<String, TypeRegistration>();
	private final AtomicReference<BundleContext> contextRef = new AtomicReference<BundleContext>();
	private ServiceTracker objectProviderTracker;

	/**
	 * Flushes any cached properties of a provider.
	 * 
	 * @param provider
	 * @param reference
	 */
	void flushProperties(final RuntimeContextObjectProvider provider, final ServiceReference reference) {
		for (final Class<?> type : provider.getObjectTypes()) {
			final TypeRegistration registration = registrations.get(type.getName());
			if (null != registration) {
				// flush properties
				registration.update(type, provider, reference);
			}
		}
	}

	/**
	 * Returns a type registration for the specified type name.
	 * 
	 * @return a type registration (maybe <code>null</code>)
	 */
	public TypeRegistration getType(final String objectTypeName) {
		return registrations.get(objectTypeName);
	}

	/**
	 * Registers a provider.
	 * 
	 * @param provider
	 *            the provider
	 * @param reference
	 *            the service reference
	 */
	void registerProvider(final RuntimeContextObjectProvider provider, final ServiceReference reference) {
		if (null == reference) {
			return;
		}

		for (final Class<?> type : provider.getObjectTypes()) {
			TypeRegistration registration = registrations.putIfAbsent(type.getName(), new TypeRegistration(type.getName()));
			if (null == registration) {
				// we have a new registration
				registration = registrations.get(type.getName());
			}

			// add provider
			registration.add(type, provider, reference);
		}

	}

	@Override
	public void shutdown() throws Exception {
		final BundleContext context = contextRef.getAndSet(null);
		if (null == context) {
			return;
		}

		objectProviderTracker.close();
		objectProviderTracker = null;
	}

	/**
	 * Starts the object provider registry.
	 * 
	 * @param context
	 * @throws IllegalStateException
	 *             if already started
	 */
	public void start(final BundleContext context) throws IllegalStateException {
		if (!contextRef.compareAndSet(null, context)) {
			throw new IllegalStateException("already (still?) active");
		}

		objectProviderTracker = new RuntimeContextObjectProviderTracker(context);
		objectProviderTracker.open();
	}

	/**
	 * Unregisters a provider.
	 * 
	 * @param provider
	 *            the provider
	 * @param reference
	 *            the providing bundle
	 */
	void unregisterProvider(final RuntimeContextObjectProvider provider, final ServiceReference reference) {
		for (final Class<?> type : provider.getObjectTypes()) {
			final TypeRegistration registration = registrations.get(type.getName());
			if (null != registration) {
				// remove provider
				registration.remove(type, provider, reference);
			}
		}
	}
}
