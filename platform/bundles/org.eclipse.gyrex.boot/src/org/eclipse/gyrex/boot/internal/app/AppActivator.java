/*******************************************************************************
 * Copyright (c) 2008, 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.boot.internal.app;

import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.server.Platform;
import org.eclipse.gyrex.server.internal.opsmode.OperationMode;
import org.eclipse.gyrex.server.internal.opsmode.OpsMode;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The activator class controls the plug-in life cycle
 */
public class AppActivator extends BaseBundleActivator {

	private static final String BUNDLE_STATE_LOCATION = ".metadata/.plugins";

	// The plug-in ID
	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.boot";

	// The shared instance
	private static AppActivator sharedInstance;

	private static final AtomicReference<OpsMode> opsMode = new AtomicReference<OpsMode>();
	private static final AtomicBoolean debugMode = new AtomicBoolean();

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static AppActivator getInstance() {
		return sharedInstance;
	}

	public static OpsMode getOpsMode() {
		return opsMode.get();
	}

	public static boolean isDebugMode() {
		return debugMode.get();
	}

	public static boolean isDevMode() {
		final OpsMode mode = getOpsMode();
		if (mode == null) {
			return true;
		}
		return mode.getMode() != OperationMode.PRODUCTION;
	}

	private BundleContext context;
	private ServiceTracker bundleTracker;
	private volatile IServiceProxy<Location> instanceLocationProxy;

	/**
	 * The constructor
	 */
	public AppActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		sharedInstance = this;
		this.context = context;

		// track instance location
		instanceLocationProxy = getServiceHelper().trackService(Location.class, context.createFilter(Location.INSTANCE_FILTER));

		// configure dev mode
		opsMode.set(new OpsMode());

		// configure debug mode
		debugMode.set((context.getProperty("osgi.debug") != null) || (getOpsMode().getMode() == OperationMode.DEVELOPMENT));
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		sharedInstance = null;
		this.context = null;

		instanceLocationProxy.dispose();
		instanceLocationProxy = null;
	}

	public Bundle getBundle(final String symbolicName) {
		final PackageAdmin packageAdmin = getBundleAdmin();
		if (packageAdmin == null) {
			return null;
		}
		final Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
		if (bundles == null) {
			return null;
		}
		// return the first bundle that is not installed or uninstalled
		for (int i = 0; i < bundles.length; i++) {
			if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
				return bundles[i];
			}
		}
		return null;
	}

	private PackageAdmin getBundleAdmin() {
		if (bundleTracker == null) {
			if (context == null) {
				return null;
			}
			bundleTracker = new ServiceTracker(context, PackageAdmin.class.getName(), null);
			bundleTracker.open();
		}
		return (PackageAdmin) bundleTracker.getService();
	}

	/**
	 * Returns the bundle context.
	 * 
	 * @return the bundle context
	 */
	public BundleContext getContext() {
		return context;
	}

	@Override
	protected Class getDebugOptions() {
		return BootDebug.class;
	}

	/**
	 * Returns an Eclipse application that can be launched on any thread.
	 * 
	 * @param applicationId
	 * @return
	 * @throws InvalidSyntaxException
	 */
	public ApplicationDescriptor getEclipseApplication(final String applicationId) throws IllegalStateException {
		final String filterString = NLS.bind("(&(objectClass={0})(service.pid={1})(application.container=org.eclipse.equinox.app)(eclipse.application.type=any.thread))", ApplicationDescriptor.class.getName(), applicationId);
		ServiceReference[] serviceReferences;
		try {
			serviceReferences = context.getServiceReferences(ApplicationDescriptor.class.getName(), filterString);
		} catch (final InvalidSyntaxException e) {
			throw new IllegalStateException(NLS.bind("Internal error while looking for application {0} using filer {1}. {2}", new Object[] { applicationId, filterString, e.getMessage() }));
		}
		if ((serviceReferences == null) || (serviceReferences.length == 0)) {
			throw new IllegalStateException(NLS.bind("Application {0} not found!", applicationId));
		} else if (serviceReferences.length > 1) {
			throw new IllegalStateException(NLS.bind("Multiple applications with id {0} found! Just one expected!", applicationId));
		}
		try {
			return (ApplicationDescriptor) context.getService(serviceReferences[0]);
		} finally {
			// immediately unget the service to let the application go away
			context.ungetService(serviceReferences[0]);
		}
	}

	public FrameworkLog getFrameworkLog() {
		try {
			return getServiceHelper().trackService(FrameworkLog.class).getService();
		} catch (final RuntimeException e) {
			// ignore
			return null;
		}
	}

	public Location getInstanceLocation() {
		final IServiceProxy<Location> proxy = instanceLocationProxy;
		if (null == proxy) {
			throw createBundleInactiveException();
		}
		return proxy.getService();
	}

	/**
	 * Implementation of {@link Platform#getInstanceLocation()}
	 * 
	 * @return path to the instance location
	 */
	public IPath getInstanceLocationPath() {
		final URL url = getInstanceLocation().getURL();
		if (url == null) {
			throw new IllegalStateException("instance location not available");
		}
		if (!url.getProtocol().equals("file")) {
			throw new IllegalStateException("instance location must be on local file system");
		}
		return new Path(url.getPath());
	}

	/**
	 * Implementation for {@link Platform#getStateLocation(Bundle)}.
	 */
	public IPath getStateLocation(final Bundle bundle) {
		if (bundle == null) {
			throw new IllegalArgumentException("bundle must not be null");
		}
		return getInstanceLocationPath().append(BUNDLE_STATE_LOCATION).append(bundle.getSymbolicName());
	}
}
