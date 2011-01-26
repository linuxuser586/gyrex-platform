/**
 * Copyright (c) 2009, 2010 AGETO Service GmbH and others.
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
import org.eclipse.gyrex.http.jetty.admin.IJettyManager;
import org.eclipse.gyrex.http.jetty.internal.admin.JettyManagerImpl;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;

import org.eclipse.jetty.util.IO;
import org.eclipse.osgi.util.NLS;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

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

	public static ServiceRegistration registerMetrics(final MetricSet metricSet) {
		return getInstance().getServiceHelper().registerService(MetricSet.SERVICE_NAME, metricSet, "Eclipse Gyrex", metricSet.getDescription(), null, null);
	}

	private volatile JettyManagerImpl jettyManager;
	private IServiceProxy<INodeEnvironment> nodeEnvironmentService;

	/**
	 * Creates a new instance.
	 */
	public HttpJettyActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instanceRef.set(this);

		jettyManager = new JettyManagerImpl();
		getServiceHelper().registerService(IJettyManager.class.getName(), jettyManager, "Eclipse Gyrex", "Jetty Engine Manager", null, null);

		nodeEnvironmentService = getServiceHelper().trackService(INodeEnvironment.class);
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instanceRef.set(null);
		jettyManager = null;
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
