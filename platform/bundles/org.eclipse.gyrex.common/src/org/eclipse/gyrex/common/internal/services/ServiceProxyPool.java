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
package org.eclipse.gyrex.common.internal.services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.common.services.IServiceProxy;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;

/**
 * A pool of service proxies which keeps week references to the created proxies
 * and re-creates them when necessary,
 */
public class ServiceProxyPool {

	/** removes a key from the pool when its proxy got disposed */
	private final class RemoveOnDisposalListener implements IServiceProxyDisposalListener {
		private final String key;

		private RemoveOnDisposalListener(final String key) {
			this.key = key;
		}

		@Override
		public void disposed(final IServiceProxy<?> proxy) {
			synchronized (trackedServices) {
				trackedServices.remove(key);
			}
		}
	}

	private final ConcurrentHashMap<String, ServiceProxy<?>> trackedServices = new ConcurrentHashMap<String, ServiceProxy<?>>();
	private final AtomicReference<BundleContext> bundleContextRef = new AtomicReference<BundleContext>();

	/**
	 * Creates a new instance.
	 * 
	 * @param bundleContext
	 */
	public ServiceProxyPool(final BundleContext bundleContext) {
		bundleContextRef.set(bundleContext);
	}

	public void dispose() {
		bundleContextRef.set(null);
		for (final ServiceProxy<?> serviceProxy : trackedServices.values()) {
			serviceProxy.dispose();
		}
		trackedServices.clear();
	}

	/**
	 * Returns the {@link BundleContext} tracked by the proxy pool.
	 * 
	 * @return the {@link BundleContext}
	 */
	public BundleContext getBundleContext() {
		final BundleContext context = bundleContextRef.get();
		if (null == context) {
			throw new IllegalStateException("disposed");
		}
		return context;
	}

	@SuppressWarnings("unchecked")
	public <T> IServiceProxy<T> getOrCreate(final Class<T> serviceInterface) {
		final BundleContext bundleContext = bundleContextRef.get();
		if (null == bundleContext) {
			throw new IllegalStateException("inactive");
		}
		final String key = serviceInterface.getName().intern();
		ServiceProxy<?> proxy = trackedServices.get(key);
		if (null == proxy) {
			synchronized (trackedServices) {
				proxy = trackedServices.get(key);
				if (null == proxy) {
					proxy = new ServiceProxy<T>(getBundleContext(), serviceInterface);
					proxy.addDisposalListener(new RemoveOnDisposalListener(key));
					trackedServices.put(key, proxy);
				}
			}
		}
		return (IServiceProxy<T>) proxy;
	}

	@SuppressWarnings("unchecked")
	public <T> IServiceProxy<T> getOrCreate(final Class<T> serviceInterface, final Filter filter) {
		final BundleContext bundleContext = bundleContextRef.get();
		if (null == bundleContext) {
			throw new IllegalStateException("inactive");
		}
		final String key = serviceInterface.getName().intern().concat(filter.toString().intern());
		ServiceProxy<?> proxy = trackedServices.get(key);
		if (null == proxy) {
			synchronized (trackedServices) {
				proxy = trackedServices.get(key);
				if (null == proxy) {
					proxy = new ServiceProxy<T>(bundleContext, serviceInterface, filter);
					proxy.addDisposalListener(new RemoveOnDisposalListener(key));
					trackedServices.put(key, proxy);
				}
			}
		}
		return (IServiceProxy<T>) proxy;
	}
}
