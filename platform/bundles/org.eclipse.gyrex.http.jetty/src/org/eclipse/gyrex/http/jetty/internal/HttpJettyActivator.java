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

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.equinox.http.jetty.JettyConfigurator;
import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
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

	/**
	 * Creates a new instance.
	 */
	public HttpJettyActivator() {
		super(SYMBOLIC_NAME);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStart(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instanceRef.set(this);
		// start the default web server
		restartJetty();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStop(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instanceRef.set(null);
		// stop Jetty
		try {
			JettyConfigurator.stopServer(JettyStarter.ID_DEFAULT);
		} catch (final Exception e) {
			LOG.warn("Error while stopping Jetty: " + e.getMessage(), e);
		}
	}

	public void restartJetty() {
		new JettyStarter().schedule(500);
	}
}
