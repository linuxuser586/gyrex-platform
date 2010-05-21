/*******************************************************************************
 * Copyright (c) 2008, 2009 Gunnar Wagenknecht and others.
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

	@SuppressWarnings("unchecked")
	public <T> IServiceProxy<T> getOrCreate(final Class<T> serviceInterface) {
		final BundleContext bundleContext = bundleContextRef.get();
		if (null == bundleContext) {
			throw new IllegalStateException("inactive");
		}
		final String key = serviceInterface.getName().intern();
		if (!trackedServices.contains(key)) {
			synchronized (trackedServices) {
				if (!trackedServices.contains(key)) {
					trackedServices.put(key, new ServiceProxy<T>(bundleContext, serviceInterface));
				}
			}
		}
		return (IServiceProxy<T>) trackedServices.get(key);
	}

	@SuppressWarnings("unchecked")
	public <T> IServiceProxy<T> getOrCreate(final Class<T> serviceInterface, final Filter filter) {
		final BundleContext bundleContext = bundleContextRef.get();
		if (null == bundleContext) {
			throw new IllegalStateException("inactive");
		}
		final String key = filter.toString().intern();
		if (!trackedServices.contains(key)) {
			synchronized (trackedServices) {
				if (!trackedServices.contains(key)) {
					trackedServices.put(key, new ServiceProxy<T>(bundleContext, serviceInterface, filter));
				}
			}
		}
		return (IServiceProxy<T>) trackedServices.get(key);
	}
}
