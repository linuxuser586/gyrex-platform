/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import org.eclipse.gyrex.http.application.manager.IApplicationManager;
import org.eclipse.gyrex.http.internal.application.gateway.HttpGatewayBinding;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationManager;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationProviderRegistry;

import org.osgi.framework.BundleContext;

/**
 *
 */
public class HttpAppManagerApplication implements IApplication {

	private static AtomicReference<HttpAppManagerApplication> instanceRef = new AtomicReference<HttpAppManagerApplication>();

	/**
	 * Returns the instanceRef.
	 * 
	 * @return the instanceRef
	 */
	public static HttpAppManagerApplication getInstance() {
		final HttpAppManagerApplication application = instanceRef.get();
		if (null == application) {
			throw new IllegalStateException("HTTP Manager Application inactive");
		}
		return application;
	}

	private volatile ApplicationProviderRegistry providerRegistry;
	private volatile ApplicationManager applicationManager;

	private volatile HttpGatewayBinding gatewayBinding;

	private IApplicationContext context;

	/**
	 * Returns the applicationManager.
	 * 
	 * @return the applicationManager
	 */
	public ApplicationManager getApplicationManager() {
		final ApplicationManager manager = applicationManager;
		if (null == manager) {
			throw new IllegalStateException("HTTP Manager Application inactive");
		}
		return manager;
	}

	/**
	 * Returns the gatewayTracker.
	 * 
	 * @return the gatewayTracker
	 */
	public HttpGatewayBinding getGatewayBinding() {
		final HttpGatewayBinding binding = gatewayBinding;
		if (null == binding) {
			throw new IllegalStateException("HTTP Manager Application inactive");
		}
		return binding;
	}

	/**
	 * Returns the providerRegistry.
	 * 
	 * @return the providerRegistry
	 */
	public ApplicationProviderRegistry getProviderRegistry() {
		final ApplicationProviderRegistry registry = providerRegistry;
		if (null == registry) {
			throw new IllegalStateException("HTTP Manager Application inactive");
		}
		return registry;
	}

	@Override
	public Object start(final IApplicationContext context) throws Exception {
		if (!instanceRef.compareAndSet(null, this)) {
			throw new IllegalStateException("already started");
		}

		// get bundle context
		final BundleContext bundleContext = HttpActivator.getInstance().getBundle().getBundleContext();

		// track the available providers
		providerRegistry = new ApplicationProviderRegistry(bundleContext);
		providerRegistry.open();

		// application manager
		applicationManager = new ApplicationManager();
		HttpActivator.getInstance().getServiceHelper().registerService(IApplicationManager.class, applicationManager, "Eclipse Gyrex", "Application Management Service", null, null);

		// open gateway binding
		gatewayBinding = new HttpGatewayBinding(bundleContext);
		gatewayBinding.open();

		this.context = context;
		context.applicationRunning();

		return IApplicationContext.EXIT_ASYNC_RESULT;
	}

	@Override
	public void stop() {
		if (!instanceRef.compareAndSet(this, null)) {
			throw new IllegalStateException("not started");
		}

		// stop gateway binding
		gatewayBinding.close();
		gatewayBinding = null;

		// kill app manager
		applicationManager = null;

		// stop provider tracker
		providerRegistry.close();
		providerRegistry = null;

		// clean-up
		context.setResult(EXIT_OK, this);
		context = null;
	}
}
