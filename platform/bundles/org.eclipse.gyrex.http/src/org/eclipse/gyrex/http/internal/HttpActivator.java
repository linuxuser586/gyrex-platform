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
package org.eclipse.cloudfree.http.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.cloudfree.common.runtime.BaseBundleActivator;
import org.eclipse.cloudfree.common.services.IServiceProxy;
import org.eclipse.cloudfree.configuration.constraints.PlatformConfigurationConstraint;
import org.eclipse.cloudfree.http.application.manager.IApplicationManager;
import org.eclipse.cloudfree.http.application.provider.ApplicationProvider;
import org.eclipse.cloudfree.http.internal.application.manager.ApplicationManager;
import org.eclipse.cloudfree.http.internal.apps.dummy.DummyAppProvider;
import org.eclipse.cloudfree.http.internal.apps.dummy.RootContext;
import org.eclipse.equinox.http.jetty.JettyConfigurator;
import org.eclipse.osgi.service.datalocation.Location;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class HttpActivator extends BaseBundleActivator {

	/** PLUGIN_ID */
	public static final String PLUGIN_ID = "org.eclipse.cloudfree.http";

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
			throw new IllegalStateException("The CloudFree Platform HTTP Core has not been started.");
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
	 * @see org.eclipse.cloudfree.common.runtime.BaseBundleActivator#doStart(org.osgi.framework.BundleContext)
	 */
	@Override
	protected synchronized void doStart(final BundleContext context) throws Exception {
		sharedInstance = this;

		// get instance location
		instanceLocationRef.set(getServiceHelper().trackService(Location.class, context.createFilter(Location.INSTANCE_FILTER)));

		// register configuration checkers
		getServiceHelper().registerService(PlatformConfigurationConstraint.class.getName(), new JettyDefaultStartDisabledConstraint(context), "CloudFree", "Jetty Auto Start Check", null, null);

		// open package admin tracker
		if (null == packageAdminTracker) {
			packageAdminTracker = new ServiceTracker(context, PackageAdmin.class.getName(), null);
			packageAdminTracker.open();
		}

		// start the application registry
		startApplicationManager(context);
		getServiceHelper().registerService(IApplicationManager.class.getName(), applicationManager, "CloudFree.net", "CloudFree Application Manager", null, null);

		// open the http tracker
		if (null == httpServiceTracker) {
			httpServiceTracker = new HttpServiceTracker(context, applicationManager);
			httpServiceTracker.open();
		}

		// start the default web server
		new JettyStarter().schedule();

		// register our dummy app provider
		final Dictionary<String, Object> properties = new Hashtable<String, Object>(2);
		properties.put(Constants.SERVICE_DESCRIPTION, "Dummy Application");
		properties.put(Constants.SERVICE_VENDOR, "CloudFree");
		dummyProviderRegistration = context.registerService(ApplicationProvider.class.getName(), new DummyAppProvider(), properties);

		applicationManager.register("app1", DummyAppProvider.ID, new RootContext(), null);
		applicationManager.register("app2", DummyAppProvider.ID, new RootContext(), null);
		applicationManager.register("app3", DummyAppProvider.ID, new RootContext(), null);
		applicationManager.register("default", DummyAppProvider.ID, new RootContext(), null);

		applicationManager.mount("http://localhost/app1", "app1");
		applicationManager.mount("http://localhost/app2", "app2");
		applicationManager.mount("http://localhost/auchapp2", "app2");
		applicationManager.mount("http://localhost/irgendwas/app3", "app3");
		applicationManager.setDefaultApplication("default");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.common.runtime.BaseBundleActivator#doStop(org.osgi.framework.BundleContext)
	 */
	@Override
	protected synchronized void doStop(final BundleContext context) throws Exception {
		// unregister the dummy app provider
		dummyProviderRegistration.unregister();

		// stop Jetty
		JettyConfigurator.stopServer("default");

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
	 * @see org.eclipse.cloudfree.common.runtime.BaseBundleActivator#getDebugOptions()
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
