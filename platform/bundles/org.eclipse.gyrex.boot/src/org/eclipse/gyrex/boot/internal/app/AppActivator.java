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
package org.eclipse.gyrex.boot.internal.app;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The activator class controls the plug-in life cycle
 */
public class AppActivator extends BaseBundleActivator {

	private static final String PROP_SHUTDOWN_PORT = "eclipse.gyrex.shutdown.port";
	private static final String UTF8 = "UTF-8";
	private static final String CMD_SHUTDOWN = "eclipse.gyrex.shutdown.command";

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.gyrex.boot";

	// The shared instance
	private static AppActivator sharedInstance;

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static AppActivator getInstance() {
		return sharedInstance;
	}

	private BundleContext context;
	private ServiceTracker bundleTracker;
	private Job shutdownListener;
	private ServerSocket serverSocket;

	/**
	 * The constructor
	 */
	public AppActivator() {
		super(PLUGIN_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStart(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStart(final BundleContext context) throws Exception {
		sharedInstance = this;
		this.context = context;

		// open external shutdown listener
		final int shutdownPort = NumberUtils.toInt(context.getProperty(PROP_SHUTDOWN_PORT), 0);
		if (shutdownPort > 0) {
			startShutdownListener(shutdownPort);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStop(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStop(final BundleContext context) throws Exception {
		sharedInstance = null;
		stopShutdownListener();
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

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#getDebugOptions()
	 */
	@Override
	protected Class getDebugOptions() {
		return AppDebug.class;
	}

	private void startShutdownListener(final int shutdownPort) throws Exception {
		final ServerSocket serverSocket = this.serverSocket = new ServerSocket(shutdownPort, 1, InetAddress.getByName("127.0.0.1"));
		shutdownListener = new Job("Shutdown Listener") {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				Socket socket = null;
				try {
					socket = serverSocket.accept();

					// check if plug-in is shut down (i.e. canceled)
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}

					// read command
					final String command = IOUtils.toString(socket.getInputStream(), UTF8);
					if (StringUtils.equals(CMD_SHUTDOWN, command)) {
						ServerApplication.signalShutdown();
					} else {
						// continue waiting
						schedule();
						return Status.CANCEL_STATUS;
					}
				} catch (final IOException e) {
					// check if plug-in is shut down (i.e. canceled)
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}

					// error
					return new Status(IStatus.ERROR, PLUGIN_ID, 0, "Error while waiting for shutdown. " + e.getMessage(), e);
				} finally {
					// close socket
					if (null != socket) {
						try {
							socket.close();
						} catch (final IOException e) {
							// ignore;
						}
					}
				}
				return Status.OK_STATUS;
			}
		};
		shutdownListener.setSystem(true);
		shutdownListener.schedule();
	}

	private void stopShutdownListener() {
		if (null != shutdownListener) {
			shutdownListener.cancel();
			shutdownListener = null;
		}
		if (null != serverSocket) {
			try {
				serverSocket.close();
			} catch (final IOException e) {
				// ignore
			}
			serverSocket = null;
		}

	}

}
