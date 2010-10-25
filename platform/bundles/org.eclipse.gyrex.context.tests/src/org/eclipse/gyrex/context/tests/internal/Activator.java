/*******************************************************************************
 * Copyright (c) 2010 AGETO and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context.tests.internal;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.context.manager.IRuntimeContextManager;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;

import org.osgi.framework.BundleContext;

public class Activator extends BaseBundleActivator {

	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.context.tests";

	private static Activator activator;

	/**
	 * Returns the activator.
	 * 
	 * @return the activator
	 */
	public static Activator getActivator() {
		final Activator activator = Activator.activator;
		if (null == activator) {
			throw new IllegalStateException("inactive");
		}
		return activator;
	}

	private BundleContext context;
	private IServiceProxy<IRuntimeContextRegistry> contextRegistryProxy;
	private IServiceProxy<IRuntimeContextManager> contextManagerProxy;

	public Activator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		activator = this;
		this.context = context;
		contextRegistryProxy = Activator.getActivator().getServiceHelper().trackService(IRuntimeContextRegistry.class);
		contextManagerProxy = Activator.getActivator().getServiceHelper().trackService(IRuntimeContextManager.class);
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		activator = null;
		this.context = null;
		contextRegistryProxy = null;
		contextManagerProxy = null;
	}

	/**
	 * Returns the context.
	 * 
	 * @return the context
	 */
	public BundleContext getContext() {
		final BundleContext context = this.context;
		if (null == context) {
			throw new IllegalStateException("inactive");
		}

		return context;
	}

	/**
	 * Returns the contextManager.
	 * 
	 * @return the contextManager
	 */
	public IRuntimeContextManager getContextManager() {
		final IServiceProxy<IRuntimeContextManager> proxy = contextManagerProxy;
		if (proxy == null) {
			throw createBundleInactiveException();
		}
		return proxy.getService();
	}

	/**
	 * Returns the contextRegistry.
	 * 
	 * @return the contextRegistry
	 */
	public IRuntimeContextRegistry getContextRegistry() {
		final IServiceProxy<IRuntimeContextRegistry> proxy = contextRegistryProxy;
		if (proxy == null) {
			throw createBundleInactiveException();
		}
		return proxy.getService();
	}
}
