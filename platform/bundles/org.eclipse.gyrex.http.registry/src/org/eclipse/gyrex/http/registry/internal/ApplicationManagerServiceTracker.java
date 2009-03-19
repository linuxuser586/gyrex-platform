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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.gyrex.http.application.manager.IApplicationManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tracks an {@link IApplicationManager} and registers applications with it.
 */
public class ApplicationManagerServiceTracker extends ServiceTracker {

	private final ConcurrentMap<ServiceReference, ApplicationRegistryManager> applicationRegistryManagers = new ConcurrentHashMap<ServiceReference, ApplicationRegistryManager>(2);
	private final PackageAdmin packageAdmin;
	private final IExtensionRegistry registry;

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 * @param packageAdmin
	 * @param registry
	 */
	public ApplicationManagerServiceTracker(final BundleContext context, final PackageAdmin packageAdmin, final IExtensionRegistry registry) {
		super(context, IApplicationManager.class.getName(), null);
		this.packageAdmin = packageAdmin;
		this.registry = registry;
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
	 */
	@Override
	public Object addingService(final ServiceReference reference) {
		final IApplicationManager applicationManager = (IApplicationManager) super.addingService(reference);
		if (applicationManager == null) {
			return null;
		}

		final ApplicationRegistryManager applicationRegistryManager = new ApplicationRegistryManager(reference, applicationManager, packageAdmin, registry);
		if (null == applicationRegistryManagers.putIfAbsent(reference, applicationRegistryManager)) {
			applicationRegistryManager.start();
		}

		return applicationManager;
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	@Override
	public void removedService(final ServiceReference reference, final Object service) {
		final ApplicationRegistryManager applicationManager = applicationRegistryManagers.remove(reference);
		if (applicationManager != null) {
			applicationManager.stop();
		}
		super.removedService(reference, service);
	}
}
