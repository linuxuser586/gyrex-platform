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
package org.eclipse.gyrex.model.common.internal;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;


import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.model.common.internal.registry.ModelProviderRegistry;
import org.osgi.framework.BundleContext;

/**
 * The core model plug-in.
 */
public class ModelActivator extends BaseBundleActivator {

	/** PLUGIN_ID */
	public static final String PLUGIN_ID = "org.eclipse.gyrex.model.common";

	/** the shared instance */
	private static final AtomicReference<ModelActivator> sharedInstance = new AtomicReference<ModelActivator>();
	private static final AtomicReference<IServiceProxy<IAdapterManager>> adapterManagerRef = new AtomicReference<IServiceProxy<IAdapterManager>>();

	/**
	 * Returns the adapter manager.
	 * 
	 * @return the adapter manager
	 */
	public static IAdapterManager getAdapterManager() {
		final IServiceProxy<IAdapterManager> serviceProxy = adapterManagerRef.get();
		if (null == serviceProxy) {
			throw new IllegalStateException("Bundle '" + PLUGIN_ID + "' is not active.");
		}
		return serviceProxy.getService();
	}

	/**
	 * Returns the shared instance.
	 * <p>
	 * A <code>{@link IllegalStateException}</code> will be thrown if the bundle
	 * has not been started.
	 * </p>
	 * 
	 * @return the shared instance
	 * @throws IllegalStateException
	 *             if the bundle has not been started
	 */
	public static ModelActivator getInstance() {
		final ModelActivator activator = sharedInstance.get();
		if (null == activator) {
			throw new IllegalStateException(MessageFormat.format("Bundle {0} has not been started.", PLUGIN_ID));
		}

		return activator;
	}

	/** the model registry */
	private final AtomicReference<ModelProviderRegistry> modelRegistry = new AtomicReference<ModelProviderRegistry>();

	/**
	 * Creates a new instance.
	 * <p>
	 * Note, this is called by the OSGi platform. <b>Clients should never call
	 * this method.</b>
	 * </p>
	 */
	public ModelActivator() {
		super(PLUGIN_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStart(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStart(final BundleContext context) throws Exception {
		sharedInstance.set(this);

		// consume adapter manager
		adapterManagerRef.set(getServiceHelper().trackService(IAdapterManager.class));

		// start the registry
		startModelRegistry(context);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStop(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStop(final BundleContext context) throws Exception {
		// stop the registry
		stopModelRegistry();

		adapterManagerRef.set(null);
		sharedInstance.set(null);
	}

	/**
	 * Returns the modelRegistry.
	 * 
	 * @return the modelRegistry
	 * @throws IllegalStateException
	 *             if the registry has not been started
	 */
	public ModelProviderRegistry getModelRegistry() throws IllegalStateException {
		final ModelProviderRegistry registry = modelRegistry.get();
		if (null == registry) {
			throw new IllegalStateException("The model registry has not been started.");
		}

		return registry;
	}

	/**
	 * Starts the model registry.
	 */
	private void startModelRegistry(final BundleContext context) {
		if (modelRegistry.compareAndSet(null, new ModelProviderRegistry())) {
			modelRegistry.get().start(context);
		}
	}

	/**
	 * Stops the model registry.
	 */
	private void stopModelRegistry() {
		final ModelProviderRegistry registry = modelRegistry.getAndSet(null);
		if (null != registry) {
			registry.stop();
		}
	}

}
