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
package org.eclipse.gyrex.services.common.internal;

import java.util.concurrent.atomic.AtomicReference;


import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.services.common.internal.registry.ServiceProviderRegistry;
import org.osgi.framework.BundleContext;

public class ServicesActivator extends BaseBundleActivator {

	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.services.common";

	private static final AtomicReference<ServicesActivator> instance = new AtomicReference<ServicesActivator>();
	private static final AtomicReference<IServiceProxy<IAdapterManager>> adapterManagerRef = new AtomicReference<IServiceProxy<IAdapterManager>>();

	/**
	 * Returns the adapter manager.
	 * 
	 * @return the adapter manager
	 */
	public static IAdapterManager getAdapterManager() {
		final IServiceProxy<IAdapterManager> serviceProxy = adapterManagerRef.get();
		if (null == serviceProxy) {
			throw new IllegalStateException("Bundle '" + SYMBOLIC_NAME + "' is not active.");
		}
		return serviceProxy.getService();
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @return the instance
	 */
	public static ServicesActivator getInstance() {
		final ServicesActivator activator = instance.get();
		if (null == activator) {
			throw new IllegalStateException("inactive");
		}
		return activator;
	}

	private final AtomicReference<ServiceProviderRegistry> serviceProviderRegistryRef = new AtomicReference<ServiceProviderRegistry>();

	/**
	 * Creates a new instance (called by the OSGi framework).
	 */
	public ServicesActivator() {
		super(SYMBOLIC_NAME);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStart(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instance.set(this);

		// consume adapter manager
		adapterManagerRef.set(getServiceHelper().trackService(IAdapterManager.class));

		// start service provider registry
		if (serviceProviderRegistryRef.compareAndSet(null, new ServiceProviderRegistry())) {
			serviceProviderRegistryRef.get().start(context);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStop(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStop(final BundleContext context) throws Exception {
		final ServiceProviderRegistry serviceProviderRegistry = serviceProviderRegistryRef.getAndSet(null);
		if (null != serviceProviderRegistry) {
			serviceProviderRegistry.stop();
		}
		instance.set(null);
		adapterManagerRef.set(null);
	}
}
