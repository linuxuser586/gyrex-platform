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

import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.http.application.provider.ApplicationProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class HttpRegistryActivator extends BaseBundleActivator implements ServiceTrackerCustomizer {

	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.http.registry";

	private ServiceTracker packageAdminTracker;
	private ServiceTracker registryTracker;
	private ServiceTracker applicationManagerServiceTracker;

	private volatile PackageAdmin packageAdmin;
	private volatile IExtensionRegistry registry;
	private volatile BundleContext context;

	/**
	 * Creates a new instance.
	 */
	public HttpRegistryActivator() {
		super(SYMBOLIC_NAME);
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
	 */
	@Override
	public Object addingService(final ServiceReference reference) {
		final Object service = context.getService(reference);

		if ((service instanceof PackageAdmin) && (packageAdmin == null)) {
			packageAdmin = (PackageAdmin) service;
		}

		if ((service instanceof IExtensionRegistry) && (registry == null)) {
			registry = (IExtensionRegistry) service;
		}

		if ((packageAdmin != null) && (registry != null)) {
			applicationManagerServiceTracker = new ApplicationManagerServiceTracker(context, packageAdmin, registry);
			applicationManagerServiceTracker.open();
		}

		return service;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStart(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStart(final BundleContext context) throws Exception {
		this.context = context;
		getServiceHelper().registerService(ApplicationProvider.class.getName(), RegistryApplicationProvider.getInstance(), "Eclipse.org", "Registry Web Application Provider", null, null);

		packageAdminTracker = new ServiceTracker(context, PackageAdmin.class.getName(), this);
		packageAdminTracker.open();

		registryTracker = new ServiceTracker(context, IExtensionRegistry.class.getName(), this);
		registryTracker.open();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStop(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStop(final BundleContext context) throws Exception {
		packageAdminTracker.close();
		packageAdminTracker = null;
		registryTracker.close();
		registryTracker = null;
		this.context = null;
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	@Override
	public void modifiedService(final ServiceReference reference, final Object service) {
		// ignore
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
	 */
	@Override
	public void removedService(final ServiceReference reference, final Object service) {
		if (service == packageAdmin) {
			packageAdmin = null;
		}

		if (service == registry) {
			registry = null;
		}

		if ((packageAdmin == null) || (registry == null)) {
			if (applicationManagerServiceTracker != null) {
				applicationManagerServiceTracker.close();
				applicationManagerServiceTracker = null;
			}
		}
		context.ungetService(reference);
	}
}
