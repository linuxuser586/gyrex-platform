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
import org.eclipse.gyrex.http.application.provider.ApplicationProvider;
import org.eclipse.gyrex.http.internal.application.defaultapp.DefaultApplicationProvider;

import org.eclipse.osgi.service.datalocation.Location;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
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

	private ServiceTracker packageAdminTracker;
	private ServiceTracker gatewayTracker;
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

		// track the http gateway
		if (null == gatewayTracker) {
			gatewayTracker = new HttpGatewayTracker(context);
			gatewayTracker.open();
		}

		// register our default application provider
		getServiceHelper().registerService(ApplicationProvider.class.getName(), new DefaultApplicationProvider(), "Eclipse Gyrex", "Default Application Provider", null, null);
	}

	@Override
	protected synchronized void doStop(final BundleContext context) throws Exception {
		// stop gateway tracker
		if (null != gatewayTracker) {
			gatewayTracker.close();
			gatewayTracker = null;
		}

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
}
