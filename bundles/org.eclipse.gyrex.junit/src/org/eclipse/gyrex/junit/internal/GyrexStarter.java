/*******************************************************************************
 * Copyright (c) 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.junit.internal;

import static junit.framework.Assert.assertEquals;

import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.cloud.events.ICloudEventConstants;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.application.ApplicationDescriptor;
import org.osgi.service.application.ApplicationHandle;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import org.apache.commons.lang.text.StrBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GyrexStarter {

	private static final Logger LOG = LoggerFactory.getLogger(GyrexStarter.class);

	private final BundleContext context;
	private final Job shutdownJob = new Job("Gyrex Shutdown Delay") {
		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			shutdown();
			return Status.OK_STATUS;
		}
	};

	private ApplicationHandle applicationHandle;

	public GyrexStarter(final BundleContext context) {
		this.context = context;
	}

	private void bootstrapBundles() throws BundleException {
		// bundles that must be present and started
		// in order to start Equinox apps and to have
		// Gyrex start properly 
		// (look at Gyrex product and bootstrap for details)
		final Set<String> requiredBundles = new HashSet<>();
		requiredBundles.add("org.eclipse.equinox.app");
		requiredBundles.add("org.eclipse.equinox.registry");
		requiredBundles.add("org.eclipse.equinox.common");
		requiredBundles.add("org.eclipse.equinox.ds");
		requiredBundles.add("org.eclipse.equinox.event");

		// collect started bundles in order to check for multiple versions
		final Set<String> started = new HashSet<>();
		for (final Bundle bundle : context.getBundles()) {
			LOG.trace("Found bundle: {}", bundle);
			if (requiredBundles.contains(bundle.getSymbolicName())) {
				if ((bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
					if (started.contains(bundle.getSymbolicName()))
						throw new IllegalStateException(String.format("Please check your test environment. It looks like multiple versions for bundle '%s' are installed in the framework which is not supported.", bundle.getSymbolicName()));
					else {
						started.add(bundle.getSymbolicName());
					}
					LOG.info("Starting required bundle: {}", bundle);
					bundle.start(Bundle.START_TRANSIENT);
				} else {
					LOG.warn("Found unresolved required bundle: {}", bundle);
				}
			}
		}

		final int diff = requiredBundles.size() - started.size();
		if (diff > 0) {
			final StrBuilder errorMessage = new StrBuilder(diff > 1 ? "The following bundles are missing: " : "The following bundle is missing: ");
			for (final String name : requiredBundles) {
				String separator = "";
				if (!started.contains(name)) {
					errorMessage.appendln(separator).append(name);
					separator = ", ";
				}
			}
			throw new IllegalStateException(errorMessage.toString());
		}
	}

	public synchronized void dispose() {
		shutdownJob.cancel();
		shutdown();
	}

	/**
	 * Ensures the server is started an online.
	 * <p>
	 * The caller will block until the server is fully started and online.
	 * </p>
	 * 
	 * @return <code>true</code> if the server has been started,
	 *         <code>false</code> otherwise
	 */
	public synchronized boolean ensureStartedAndOnline() throws Exception {
		shutdownJob.cancel();

		if (applicationHandle != null) {
			LOG.debug("Gyrex already running!");
			return false;
		}

		LOG.debug("Starting Gyrex");

		bootstrapBundles();

		// test for bogus system properties
		if (Boolean.getBoolean("gyrex.preferences.instancebased")) {
			LOG.warn("Overriding system propert 'gyrex.preferences.instancebased' in order to force ZooKeeper based cloud preferences!");
			System.setProperty("gyrex.preferences.instancebased", Boolean.FALSE.toString());
		}

		// hook event listener to listen for the node to become online
		final CountDownLatch cloudOnlineWatch = new CountDownLatch(1);
		final EventHandler cloudOnlineHandler = new EventHandler() {
			@Override
			public void handleEvent(final Event event) {
				cloudOnlineWatch.countDown();
			}
		};
		final Hashtable<String, Object> properties = new Hashtable<String, Object>(1);
		properties.put(EventConstants.EVENT_TOPIC, ICloudEventConstants.TOPIC_NODE_ONLINE);
		context.registerService(EventHandler.class, cloudOnlineHandler, properties);

		// get hold of application handle and start application
		final Collection<ServiceReference<ApplicationDescriptor>> refs = context.getServiceReferences(ApplicationDescriptor.class, "(service.pid=org.eclipse.gyrex.boot.server)");
		assertEquals("Unable to find proper Gyrex Server application to start!", 1, refs.size());
		final ServiceReference<ApplicationDescriptor> sr = refs.iterator().next();
		final ApplicationDescriptor service = context.getService(sr);
		try {
			applicationHandle = service.launch(null);
		} finally {
			context.ungetService(sr);
		}

		//  wait for node becoming online
		final long timeout = Long.getLong("gyrex.servertestapp.timeout", 60000l);
		LOG.info("Waiting {}ms for node to become online...", timeout);
		if (!cloudOnlineWatch.await(timeout, TimeUnit.MILLISECONDS)) {
			LOG.error("Timeout waiting for node to become online.");
			throw new IllegalStateException("Timeout while waiting for node to establish connection with ZooKeeper. Unable to initialize cloud environment.");
		}

		return true;
	}

	/**
	 * Requests a shutdown.
	 * <p>
	 * The shutdown will be initiated if the server is running. It may complete
	 * asynchronously.
	 * </p>
	 */
	public synchronized void requestShutdown() {
		LOG.debug("Gyrex shutdown requested");
		shutdownJob.schedule(30000L);
	}

	void shutdown() {
		LOG.debug("Shutting down Gyrex");
		if (applicationHandle != null) {
			applicationHandle.destroy();
			applicationHandle = null;
		}
	}

}
