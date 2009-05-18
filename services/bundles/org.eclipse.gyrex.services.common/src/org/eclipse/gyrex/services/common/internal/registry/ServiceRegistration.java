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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.services.common.IService;
import org.eclipse.gyrex.services.common.provider.ServiceProvider;
import org.osgi.framework.Bundle;

/**
 * 
 */
public class ServiceRegistration implements IAdapterFactory {

	private final List<Class> adapterList = new ArrayList<Class>(1);
	private final String serviceClassName;
	private final List<ProviderRegistration> providers = new CopyOnWriteArrayList<ProviderRegistration>();

	/**
	 * Creates a new instance.
	 * 
	 * @param name
	 */
	public ServiceRegistration(final String serviceClassName) {
		this.serviceClassName = serviceClassName;
	}

	public void add(final Class<?> service, final ServiceProvider provider, final Bundle bundle) {
		if (!serviceClassName.equals(service.getName())) {
			throw new IllegalArgumentException("service class name should be " + serviceClassName);
		}
		if (!adapterList.contains(service)) {
			adapterList.add(service);
		}

		providers.add(new ProviderRegistration(service, provider, bundle));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
	 */
	@Override
	public Object getAdapter(final Object adaptableObject, final Class adapterType) {
		// we only provide adapters of IRuntimeContext objects
		if (!(adaptableObject instanceof IRuntimeContext)) {
			return null;
		}

		// we only adapt to IService
		if (!IService.class.isAssignableFrom(adapterType)) {
			return null;
		}

		// the context
		final IRuntimeContext context = (IRuntimeContext) adaptableObject;

		// get the provider to use
		final ServiceProvider provider = getProvider(context, adapterType);
		if (null == provider) {
			return null;
		}

		// get the service for the specified context and repository
		// TODO: implement caching
		return provider.createServiceInstance(adapterType, context, new ServiceStatusMonitor());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
	 */
	@Override
	public Class[] getAdapterList() {
		return adapterList.toArray(new Class[0]);
	}

	/**
	 * Looks up the provider to use for the specified context.
	 * 
	 * @param context
	 * @param serviceType
	 * @return the provider to use
	 */
	private ServiceProvider getProvider(final IRuntimeContext context, final Class<?> serviceType) {
		// currently, we simply use the first matching provider
		// in the future a context might specify a preferred provider version
		for (final ProviderRegistration provider : providers) {
			if (serviceType.isAssignableFrom(provider.getManager())) {
				return provider.getProvider();
			}
		}

		// no service available
		return null;
	}

	public boolean isEmpty() {
		return providers.isEmpty();
	}

	public void remove(final Class<?> service, final ServiceProvider provider, final Bundle bundle) {
		if (!serviceClassName.equals(service.getName())) {
			throw new IllegalArgumentException("service class name should be " + serviceClassName);
		}
		providers.remove(new ProviderRegistration(service, provider, bundle));
	}

}
