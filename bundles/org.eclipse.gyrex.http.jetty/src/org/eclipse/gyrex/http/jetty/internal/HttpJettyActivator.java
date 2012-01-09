/**
 * Copyright (c) 2009, 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.http.jetty.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.cloud.environment.INodeEnvironment;
import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.http.internal.BundleFinder;
import org.eclipse.gyrex.http.jetty.admin.IJettyManager;
import org.eclipse.gyrex.http.jetty.internal.admin.JettyManagerImpl;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jetty.util.IO;
import org.eclipse.osgi.util.NLS;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class HttpJettyActivator extends BaseBundleActivator {

	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.http.jetty";
	private static final AtomicReference<HttpJettyActivator> instanceRef = new AtomicReference<HttpJettyActivator>();

	/**
	 * Returns the shared instance.
	 * 
	 * @return the shared instance
	 */
	public static HttpJettyActivator getInstance() throws IllegalStateException {
		final HttpJettyActivator httpJettyActivator = instanceRef.get();
		if (null == httpJettyActivator) {
			throw new IllegalStateException("Bundle '" + SYMBOLIC_NAME + "' is inactive.");
		}
		return httpJettyActivator;
	}

	public static IStatus getPlatformStatus() {
		try {
			final StatusMonitor monitor = getInstance().statusMonitor;
			if (monitor != null) {
				return monitor.getOverallStatus();
			}
		} catch (final IllegalStateException e) {
			// ignored
		}

		return new Status(IStatus.ERROR, SYMBOLIC_NAME, "Jetty Integration is not active.");
	}

	public static byte[] readBundleResource(final String bundleResource) {
		final URL eclipseIconUrl = getInstance().getBundle().getEntry(bundleResource);
		if (null == eclipseIconUrl) {
			throw new IllegalStateException("Bundle resource not found: " + bundleResource);
		}
		InputStream in = null;
		try {
			in = eclipseIconUrl.openStream();
			return IO.readBytes(in);
		} catch (final IOException e) {
			throw new IllegalStateException(NLS.bind("Error reading resource {0}: {1}", bundleResource, e.getMessage()));
		} finally {
			IO.close(in);
		}
	}

	private volatile JettyManagerImpl jettyManager;
	private IServiceProxy<INodeEnvironment> nodeEnvironmentService;

	private volatile StatusMonitor statusMonitor;

	/**
	 * Creates a new instance.
	 */
	public HttpJettyActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instanceRef.set(this);

		statusMonitor = new StatusMonitor(context);
		statusMonitor.open();

		jettyManager = new JettyManagerImpl();
		getServiceHelper().registerService(IJettyManager.class.getName(), jettyManager, "Eclipse Gyrex", "Jetty Engine Manager", null, null);

		nodeEnvironmentService = getServiceHelper().trackService(INodeEnvironment.class);
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instanceRef.set(null);
		jettyManager = null;

		statusMonitor.close();
		statusMonitor = null;
	}

	public Bundle getCallingBundle() {
		final Bundle bundle = getBundle();
		if (null == bundle) {
			return null;
		}

		return new BundleFinder(bundle).getCallingBundle();
	}

	@Override
	protected Class getDebugOptions() {
		return JettyDebug.class;
	}

	/**
	 * Returns the jettyManager.
	 * 
	 * @return the jettyManager
	 */
	public IJettyManager getJettyManager() {
		final JettyManagerImpl manager = jettyManager;
		if (manager == null) {
			throw createBundleInactiveException();
		}
		return manager;
	}

	/**
	 * Returns the nodeEnvironmentService.
	 * 
	 * @return the nodeEnvironmentService
	 */
	public INodeEnvironment getNodeEnvironment() {
		final IServiceProxy<INodeEnvironment> proxy = nodeEnvironmentService;
		if (null == proxy) {
			throw createBundleInactiveException();
		}
		return proxy.getService();
	}

}
