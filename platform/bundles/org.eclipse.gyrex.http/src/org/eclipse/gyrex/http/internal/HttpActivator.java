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
package org.eclipse.gyrex.http.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.configuration.constraints.PlatformConfigurationConstraint;
import org.eclipse.gyrex.http.application.manager.IApplicationManager;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationManager;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class HttpActivator extends BaseBundleActivator {

	/** PLUGIN_ID */
	public static final String PLUGIN_ID = "org.eclipse.gyrex.http";

	/** server type for the default web server */
	public static final String TYPE_WEB = PLUGIN_ID + ".default";

	/** the shared instance */
	private static HttpActivator sharedInstance;

	/**
	 * Returns the instance.
	 * 
	 * @return the instance
	 */
	public static HttpActivator getInstance() throws IllegalStateException {
		final HttpActivator instance = sharedInstance;
		if (null == instance) {
			throw new IllegalStateException("Gyrex HTTP Core has not been started.");
		}
		return instance;
	}

	/**
	 * Returns the bundle version.
	 * <p>
	 * This method doesn't fail if the plug-in is inactive.
	 * </p>
	 * 
	 * @return the bundle version
	 */
	public static String getVersion() {
		try {
			return getInstance().getBundleVersion().toString();
		} catch (final RuntimeException e) {
			// don't fail
			return Version.emptyVersion.toString();
		}
	}

	private HttpServiceTracker httpServiceTracker;

	private ApplicationManager applicationManager;
	private ServiceTracker packageAdminTracker;
	private ServiceRegistration dummyProviderRegistration;
	private final AtomicReference<IServiceProxy<Location>> instanceLocationRef = new AtomicReference<IServiceProxy<Location>>();

	/**
	 * Creates a new instance.
	 * <p>
	 * Note, this constructor is called by the OSGi framework. It is not
	 * intended to be called by clients.
	 * </p>
	 */
	public HttpActivator() {
		super(PLUGIN_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStart(org.osgi.framework.BundleContext)
	 */
	@Override
	protected synchronized void doStart(final BundleContext context) throws Exception {
		sharedInstance = this;

		// get instance location
		instanceLocationRef.set(getServiceHelper().trackService(Location.class, context.createFilter(Location.INSTANCE_FILTER)));

		// register configuration checkers
		getServiceHelper().registerService(PlatformConfigurationConstraint.class.getName(), new JettyDefaultStartDisabledConstraint(context), "Gyrex", "Jetty Auto Start Check", null, null);

		// open package admin tracker
		if (null == packageAdminTracker) {
			packageAdminTracker = new ServiceTracker(context, PackageAdmin.class.getName(), null);
			packageAdminTracker.open();
		}

		// start the application registry
		startApplicationManager(context);
		getServiceHelper().registerService(IApplicationManager.class.getName(), applicationManager, "Eclipse Gyrex", "Gyrex Application Manager", null, null);

		// open the http tracker
		if (null == httpServiceTracker) {
			httpServiceTracker = new HttpServiceTracker(context, applicationManager);
			httpServiceTracker.open();
		}

		// register our dummy app provider
		//		final Dictionary<String, Object> properties = new Hashtable<String, Object>(2);
		//		properties.put(Constants.SERVICE_DESCRIPTION, "Dummy Application");
		//		properties.put(Constants.SERVICE_VENDOR, "Gyrex");
		//		dummyProviderRegistration = context.registerService(ApplicationProvider.class.getName(), new DummyAppProvider(), properties);
		//
		//		applicationManager.register("default", DummyAppProvider.ID, new RootContext(), null);
		//		applicationManager.setDefaultApplication("default");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStop(org.osgi.framework.BundleContext)
	 */
	@Override
	protected synchronized void doStop(final BundleContext context) throws Exception {
		// unregister the dummy app provider
		if (null != dummyProviderRegistration) {
			dummyProviderRegistration.unregister();
			dummyProviderRegistration = null;
		}

		// stop the HTTP service tracker
		if (null != httpServiceTracker) {
			httpServiceTracker.close();
			httpServiceTracker = null;
		}

		// stop the application registry
		stopApplicationManager();

		// stop package admin tracker
		if (null != packageAdminTracker) {
			packageAdminTracker.close();
			packageAdminTracker = null;
		}

		// unset instance location
		instanceLocationRef.set(null);

		// unset instance
		sharedInstance = null;
	}

	/**
	 * Returns the bundle that loaded the provided class, or <code>null</code>
	 * if the bundle could not be determined.
	 * 
	 * @return the bundle or <code>null</code>
	 */
	public Bundle getBundleId(final Class clazz) {
		if (clazz == null) {
			return null;
		}

		final ServiceTracker packageAdminTracker = this.packageAdminTracker;
		if (null == packageAdminTracker) {
			return null;
		}

		final PackageAdmin packageAdmin = (PackageAdmin) packageAdminTracker.getService();
		return null != packageAdmin ? packageAdmin.getBundle(clazz) : null;
	}

	/**
	 * Returns the bundle that contains the provided object, or
	 * <code>null</code> if the bundle could not be determined.
	 * 
	 * @return the bundle or <code>null</code>
	 */
	public Bundle getBundleId(final Object object) {
		return null != object ? getBundleId(object.getClass()) : null;
	}

	/**
	 * Returns the bundle that is calling us.
	 * 
	 * @return the bundle or <code>null</code>
	 */
	public Bundle getCallingBundle() {
		final ServiceTracker packageAdminTracker = this.packageAdminTracker;
		if (null == packageAdminTracker) {
			return null;
		}

		final PackageAdmin packageAdmin = (PackageAdmin) packageAdminTracker.getService();
		if (null == packageAdmin) {
			return null;
		}

		final Bundle bundle = getBundle();
		if (null == bundle) {
			return null;
		}

		return new BundleFinder(packageAdmin, bundle).getCallingBundle();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#getDebugOptions()
	 */
	@Override
	protected Class getDebugOptions() {
		return HttpDebug.class;
	}

	public Location getInstanceLocation() {
		final IServiceProxy<Location> serviceProxy = instanceLocationRef.get();
		if (null == serviceProxy) {
			throw createBundleInactiveException();
		}

		return serviceProxy.getService();
	}

	private void startApplicationManager(final BundleContext context) {
		if (null == applicationManager) {
			applicationManager = new ApplicationManager(context);
			applicationManager.open();
		}
	}

	private void stopApplicationManager() {
		if (null != applicationManager) {
			applicationManager.close();
			applicationManager = null;
		}
	}
}
