/*******************************************************************************
 * Copyright (c) 2008 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Cognos Incorporated, IBM Corporation - concept/implementation from
 *                                            org.eclipse.equinox.http.registry
 *     Gunnar Wagenknecht - adaption to Gyrex
 *******************************************************************************/
package org.eclipse.gyrex.http.registry.internal;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.http.application.provider.ApplicationProvider;

import org.eclipse.core.runtime.IExtensionRegistry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class HttpRegistryActivator extends BaseBundleActivator implements ServiceTrackerCustomizer {

	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.http.registry";

	private ServiceTracker packageAdminTracker;
	private ServiceTracker extensionRegistryTracker;
	private ServiceTracker contextRegistryTracker;
	private ServiceTracker applicationManagerServiceTracker;

	private volatile PackageAdmin packageAdmin;
	private volatile IExtensionRegistry extensionRegistry;
	private volatile IRuntimeContextRegistry contextRegistry;
	private volatile BundleContext context;

	/**
	 * Creates a new instance.
	 */
	public HttpRegistryActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	public Object addingService(final ServiceReference reference) {
		final Object service = context.getService(reference);

		if ((service instanceof PackageAdmin) && (packageAdmin == null)) {
			packageAdmin = (PackageAdmin) service;
		}

		if ((service instanceof IExtensionRegistry) && (extensionRegistry == null)) {
			extensionRegistry = (IExtensionRegistry) service;
		}

		if ((service instanceof IRuntimeContextRegistry) && (contextRegistry == null)) {
			contextRegistry = (IRuntimeContextRegistry) service;
		}

		if ((packageAdmin != null) && (extensionRegistry != null) && (contextRegistry != null)) {
			applicationManagerServiceTracker = new ApplicationManagerServiceTracker(context, packageAdmin, extensionRegistry, contextRegistry);
			applicationManagerServiceTracker.open();
		}

		return service;
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		this.context = context;
		getServiceHelper().registerService(ApplicationProvider.class.getName(), RegistryApplicationProvider.getInstance(), "Eclipse.org", "Registry Web Application Provider", null, null);

		packageAdminTracker = new ServiceTracker(context, PackageAdmin.class.getName(), this);
		packageAdminTracker.open();

		extensionRegistryTracker = new ServiceTracker(context, IExtensionRegistry.class.getName(), this);
		extensionRegistryTracker.open();

		contextRegistryTracker = new ServiceTracker(context, IRuntimeContextRegistry.class.getName(), this);
		contextRegistryTracker.open();
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		packageAdminTracker.close();
		packageAdminTracker = null;
		extensionRegistryTracker.close();
		extensionRegistryTracker = null;
		this.context = null;
	}

	@Override
	protected Class getDebugOptions() {
		return HttpRegistryDebug.class;
	}

	@Override
	public void modifiedService(final ServiceReference reference, final Object service) {
		// ignore
	}

	@Override
	public void removedService(final ServiceReference reference, final Object service) {
		if (service == packageAdmin) {
			packageAdmin = null;
		}

		if (service == extensionRegistry) {
			extensionRegistry = null;
		}

		if (service == contextRegistry) {
			contextRegistry = null;
		}

		if ((null == packageAdmin) || (null == extensionRegistry) || (null == contextRegistry)) {
			if (applicationManagerServiceTracker != null) {
				applicationManagerServiceTracker.close();
				applicationManagerServiceTracker = null;
			}
		}
		context.ungetService(reference);
	}
}
