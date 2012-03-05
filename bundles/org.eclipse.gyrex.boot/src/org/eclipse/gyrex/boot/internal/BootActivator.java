/*******************************************************************************
 * Copyright (c) 2008, 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.boot.internal;

import java.net.URL;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.boot.internal.logback.LogbackConfigurator;
import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.server.Platform;
import org.eclipse.gyrex.server.internal.opsmode.OperationMode;
import org.eclipse.gyrex.server.internal.opsmode.OpsMode;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

import org.apache.commons.lang.UnhandledException;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The activator class controls the plug-in life cycle
 */
@SuppressWarnings("deprecation")
public class BootActivator extends BaseBundleActivator {

	private static final String BUNDLE_STATE_LOCATION = ".metadata/.plugins";
	private static final Logger LOG = LoggerFactory.getLogger(BootActivator.class);

	// The plug-in ID
	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.boot";

	// The shared instance
	private static BootActivator sharedInstance;

	private static final AtomicReference<OpsMode> opsModeRef = new AtomicReference<OpsMode>();
	private static final AtomicBoolean debugModeRef = new AtomicBoolean();
	private static int portOffset;

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static BootActivator getInstance() {
		return sharedInstance;
	}

	public static OpsMode getOpsMode() {
		return opsModeRef.get();
	}

	/**
	 * Returns the portOffset.
	 * 
	 * @return the portOffset
	 */
	public static int getPortOffset() {
		return portOffset;
	}

	public static boolean isDebugMode() {
		return debugModeRef.get();
	}

	public static boolean isDevMode() {
		final OpsMode mode = getOpsMode();
		if (mode == null) {
			return true;
		}
		return mode.getMode() != OperationMode.PRODUCTION;
	}

	private BundleContext context;
	private ServiceTracker<PackageAdmin, PackageAdmin> bundleTracker;
	private volatile IServiceProxy<Location> instanceLocationProxy;
	private volatile IPath instanceLocationPath;
	private ServiceRegistration frameworkLogServiceRegistration;

	/**
	 * The constructor
	 */
	public BootActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		sharedInstance = this;
		this.context = context;

		// track instance location
		instanceLocationProxy = getServiceHelper().trackService(Location.class, Location.INSTANCE_FILTER);

		// configure dev mode
		final OpsMode opsMode = new OpsMode();
		opsModeRef.set(opsMode);
		if (!opsMode.isSet()) {
			getServiceHelper().registerService(IStatus.class.getName(), new Status(IStatus.WARNING, SYMBOLIC_NAME, "The system operation mode has not been configured yet. Therefore the system operates in development mode."), "Eclipse Gyrex", "System operation mode status information.", SYMBOLIC_NAME.concat(".status.operationMode"), null);
		}

		// configure debug mode
		debugModeRef.set((context.getProperty("osgi.debug") != null) || (getOpsMode().getMode() == OperationMode.DEVELOPMENT));

		// allow switching debugging at runtime
		try {
			final Object consoleCommands = getBundle().loadClass("org.eclipse.gyrex.boot.internal.console.DebugConsoleCommands").newInstance();
			getServiceHelper().registerService("org.eclipse.osgi.framework.console.CommandProvider", consoleCommands, "Eclipse Gyrex", "Console commands for configuring debug options at runtime", null, null);
		} catch (final ClassNotFoundException e) {
			// ignore
		} catch (final LinkageError e) {
			// ignore
		} catch (final Exception e) {
			// error (but do not fail)
			LOG.warn("Error while registering debug options command provider. ", e);
		}

		// initial logback configuration
		loggingOn();

		// get port offset
		final String portOffset = context.getProperty("gyrex.portOffset");
		if (portOffset != null) {
			int offset;
			try {
				offset = Integer.parseInt(portOffset);
			} catch (final Exception e) {
				throw new UnhandledException("Invalid port offset. ", e);
			}
			if (offset < 0) {
				throw new IllegalStateException("Negativ port offset not allowed!");
			}
			BootActivator.portOffset = offset;
		}
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		loggingOff();
		sharedInstance = null;
		this.context = null;
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

	@Deprecated
	private PackageAdmin getBundleAdmin() {
		if (bundleTracker == null) {
			if (context == null) {
				return null;
			}
			bundleTracker = new ServiceTracker<PackageAdmin, PackageAdmin>(context, PackageAdmin.class, null);
			bundleTracker.open();
		}
		return bundleTracker.getService();
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
		Collection<ServiceReference<ApplicationDescriptor>> serviceReferences;
		try {
			serviceReferences = context.getServiceReferences(ApplicationDescriptor.class, filterString);
		} catch (final InvalidSyntaxException e) {
			throw new IllegalStateException(NLS.bind("Internal error while looking for application {0} using filer {1}. {2}", new Object[] { applicationId, filterString, e.getMessage() }));
		}
		if ((serviceReferences == null) || (serviceReferences.isEmpty())) {
			throw new IllegalStateException(NLS.bind("Application {0} not found!", applicationId));
		} else if (serviceReferences.size() > 1) {
			throw new IllegalStateException(NLS.bind("Multiple applications with id {0} found! Just one expected!", applicationId));
		}
		final ServiceReference<ApplicationDescriptor> serviceReference = serviceReferences.iterator().next();
		try {
			return context.getService(serviceReference);
		} finally {
			// immediately unget the service to let the application go away
			context.ungetService(serviceReference);
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

	public Location getInstanceLocation() throws IllegalStateException {
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
		if (instanceLocationPath != null) {
			return instanceLocationPath;
		}
		final URL url = getInstanceLocation().getURL();
		if (url == null) {
			throw new IllegalStateException("instance location not available");
		}
		if (!url.getProtocol().equals("file")) {
			throw new IllegalStateException("instance location must be on local file system");
		}
		return instanceLocationPath = new Path(url.getPath());
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

	private void loggingOff() {
		// disable framework logging
		if (frameworkLogServiceRegistration != null) {
			frameworkLogServiceRegistration.unregister();
			frameworkLogServiceRegistration = null;
		}

		// reset logback
		try {
			LogbackConfigurator.reset();
		} catch (final ClassNotFoundException e) {
			// logback not available
		} catch (final NoClassDefFoundError e) {
			// logback not available
		} catch (final Exception e) {
			// error (but do not fail)
			LOG.warn("Error while de-configuring logback. Please re-configure logging manually. {}", ExceptionUtils.getRootCauseMessage(e), e);
			// however, at this point it might not be possible to use a logger, Logback might be in a broken state
			// thus, we also print as much information to the console as possible
			System.err.printf("Error while de-configuring logback. Please re-configure logging manually. %s", ExceptionUtils.getFullStackTrace(e));
		}
	}

	private void loggingOn() {
		// configure logback
		try {
			LogbackConfigurator.configureDefaultContext();
		} catch (final ClassNotFoundException e) {
			// logback not available
			LOG.debug("Logback not available. Please configure logging manually. ({})", e.getMessage());
		} catch (final LinkageError e) {
			// logback not available
			LOG.debug("Logback not available. Please configure logging manually. ({})", e.getMessage());
		} catch (final Exception e) {
			// error (but do not fail)
			LOG.warn("Error while configuring logback. Please configure logging manually. {}", ExceptionUtils.getRootCauseMessage(e), e);
			// however, at this point it might not be possible to use a logger, Logback might be in a broken state
			// thus, we also print as much information to the console as possible
			System.err.printf("Error while configuring logback. Please configure logging manually. %s", ExceptionUtils.getFullStackTrace(e));
		}

		// hook FrameworkLog with SLF4J forwarder
		// (note, we use strings here in order to not import those classes)
		frameworkLogServiceRegistration = BootActivator.getInstance().getServiceHelper().registerService(Logger.class.getName(), LoggerFactory.getLogger("org.eclipse.equinox.logger"), "Eclipse Gyrex", "SLF4J Equinox Framework Logger", "org.slf4j.Logger-org.eclipse.equinox.logger", null);
	}
}
