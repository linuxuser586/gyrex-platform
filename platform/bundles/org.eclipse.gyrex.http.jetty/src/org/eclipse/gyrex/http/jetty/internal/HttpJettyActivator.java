/**
 * Copyright (c) 2009 AGETO Service GmbH and others.
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

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.http.internal.application.gateway.IHttpGateway;
import org.eclipse.gyrex.http.jetty.internal.app.JettyGateway;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.IO;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;

import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpJettyActivator extends BaseBundleActivator {

	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.http.jetty";

	private static final AtomicReference<HttpJettyActivator> instanceRef = new AtomicReference<HttpJettyActivator>();
	private static final Logger LOG = LoggerFactory.getLogger(HttpJettyActivator.class);

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

	private JettyGateway gateway;
	private Server server;

	private IServiceProxy<Location> instanceLocationRef;

	/**
	 * Creates a new instance.
	 */
	public HttpJettyActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instanceRef.set(this);

		// track instance location
		instanceLocationRef = getServiceHelper().trackService(Location.class, context.createFilter(Location.INSTANCE_FILTER));

		// initialize (but do not start) the Jetty server
		server = new Server();

		// create & register gateway
		gateway = new JettyGateway(server, instanceLocationRef.getService());
		getServiceHelper().registerService(IHttpGateway.class.getName(), gateway, "Eclipse Gyrex", "Jetty based HTTP gateway.", null, null);

		// start the server
		new JettyStarter(server).schedule(500);
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instanceRef.set(null);

		// stop Jetty
		try {
//			JettyConfigurator.stopServer(JettyStarter.ID_DEFAULT);
		} catch (final Exception e) {
			LOG.warn("Error while stopping Jetty: " + e.getMessage(), e);
		}

		// destroy gateway
		if (null != gateway) {
			gateway.close();
			gateway = null;
		}
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

}
