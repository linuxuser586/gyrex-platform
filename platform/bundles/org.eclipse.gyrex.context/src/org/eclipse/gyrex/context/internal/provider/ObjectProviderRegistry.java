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
package org.eclipse.gyrex.context.internal.provider;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.common.lifecycle.IShutdownParticipant;
import org.eclipse.gyrex.context.provider.ContextObjectProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The internal object provider registry.
 */
public class ObjectProviderRegistry implements IShutdownParticipant {

	private final ConcurrentMap<String, TypeRegistration> registrations = new ConcurrentHashMap<String, TypeRegistration>();

	private final AtomicReference<BundleContext> contextRef = new AtomicReference<BundleContext>();

	private ServiceTracker tracker;
	private final ServiceTrackerCustomizer serviceTrackerCustomizer = new ServiceTrackerCustomizer() {
		@Override
		public Object addingService(final ServiceReference reference) {
			final ContextObjectProvider provider = (ContextObjectProvider) contextRef.get().getService(reference);
			if (null != provider) {
				registerProvider(provider, reference);
			}
			return provider;
		}

		@Override
		public void modifiedService(final ServiceReference reference, final Object service) {
			final ContextObjectProvider provider = (ContextObjectProvider) service;
			if (null != provider) {
				flushProperties(provider, reference);
			}
		}

		@Override
		public void removedService(final ServiceReference reference, final Object service) {
			final ContextObjectProvider provider = (ContextObjectProvider) service;
			if (null != provider) {
				unregisterProvider(provider, reference);
			}
			contextRef.get().ungetService(reference);
		}
	};

	/**
	 * Flushes any cached properties of a provider.
	 * 
	 * @param provider
	 * @param reference
	 */
	void flushProperties(final ContextObjectProvider provider, final ServiceReference reference) {
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
	void registerProvider(final ContextObjectProvider provider, final ServiceReference reference) {
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

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.lifecycle.IShutdownParticipant#shutdown()
	 */
	@Override
	public void shutdown() throws Exception {
		final BundleContext context = contextRef.getAndSet(null);
		if (null == context) {
			return;
		}

		tracker.close();
		tracker = null;
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

		tracker = new ServiceTracker(context, ContextObjectProvider.class.getName(), serviceTrackerCustomizer);
		tracker.open();
	}

	/**
	 * Unregisters a provider.
	 * 
	 * @param provider
	 *            the provider
	 * @param reference
	 *            the providing bundle
	 */
	void unregisterProvider(final ContextObjectProvider provider, final ServiceReference reference) {
		for (final Class<?> type : provider.getObjectTypes()) {
			final TypeRegistration registration = registrations.get(type.getName());
			if (null != registration) {
				// remove provider
				registration.remove(type, provider, reference);
			}
		}
	}
}
