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

import org.eclipse.gyrex.common.services.IServiceProxy;

import org.osgi.framework.BundleContext;

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
	private volatile BundleContext bundleContext;

	/**
	 * Creates a new instance.
	 * 
	 * @param bundleContext
	 */
	public ServiceProxyPool(final BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	public void dispose() {
		// unsetting the bundle context makes getter fail (aka. "disposes the pool")
		bundleContext = null;

		// synchronize on map (even though it's a concurrent map) to wait for pending ongoing gets/creates
		synchronized (trackedServices) {
			for (final ServiceProxy<?> serviceProxy : trackedServices.values()) {
				serviceProxy.dispose();
			}
			trackedServices.clear();
		}
	}

	/**
	 * Returns the {@link BundleContext} tracked by the proxy pool.
	 * 
	 * @return the {@link BundleContext}
	 */
	public BundleContext getBundleContext() {
		final BundleContext context = bundleContext;
		if (null == context) {
			throw new IllegalStateException("disposed");
		}
		return context;
	}

	private <T> String getKey(final Class<T> serviceInterface, final String filter) {
		if (null != filter) {
			return serviceInterface.getName().intern().concat(filter);
		} else {
			return serviceInterface.getName().intern();
		}
	}

	public <T> IServiceProxy<T> getOrCreate(final Class<T> serviceInterface) {
		return getOrCreate(serviceInterface, null);
	}

	@SuppressWarnings("unchecked")
	public <T> IServiceProxy<T> getOrCreate(final Class<T> serviceInterface, final String filter) {
		// check for disposal
		getBundleContext();

		// get lookup key
		final String key = getKey(serviceInterface, filter);

		// lookup proxy
		ServiceProxy<?> proxy = trackedServices.get(key);
		if (null == proxy) {
			synchronized (trackedServices) {
				proxy = trackedServices.get(key);
				if (null == proxy) {
					// create proxy (but use getBundleContext() here in order to check for disposal again)
					proxy = new ServiceProxy<T>(getBundleContext(), serviceInterface, filter);
					proxy.addDisposalListener(new RemoveOnDisposalListener(key));
					trackedServices.put(key, proxy);
				}
			}
		}
		return (IServiceProxy<T>) proxy;
	}
}
