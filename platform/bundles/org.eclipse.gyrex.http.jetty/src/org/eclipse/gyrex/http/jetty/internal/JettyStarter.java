/**
 * Copyright (c) 2008, 2011 AGETO Service GmbH and others.
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

import org.eclipse.gyrex.boot.internal.app.ServerApplication;
import org.eclipse.gyrex.preferences.PlatformScope;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jetty.http.HttpGenerator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
final class JettyStarter extends Job {

	private static final Logger LOG = LoggerFactory.getLogger(JettyStarter.class);

	static final String ID_DEFAULT = "default";
	private final Server server;
	private long delay = 1000l;

	/**
	 * Creates a new instance.
	 * 
	 * @param server
	 */
	JettyStarter(final Server server) {
		super("Jetty Starter");
		this.server = server;
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		monitor.beginTask("Staring Jetty", 100);
		try {
			final IEclipsePreferences preferences = new PlatformScope().getNode(HttpJettyActivator.SYMBOLIC_NAME);

			final SelectChannelConnector connector = new SelectChannelConnector();
			connector.setPort(preferences.getInt("port", 80));
			connector.setMaxIdleTime(200000);
			connector.setAcceptors(2);
			connector.setStatsOn(false);
			connector.setConfidentialPort(443);
			connector.setLowResourcesConnections(20000);
			connector.setLowResourcesMaxIdleTime(5000);
			connector.setForwarded(true);
			server.addConnector(connector);

			// enable SSL if necessary
			final int sslPort = preferences.getInt("https.port", 0);
			if (sslPort > 0) {
				final SslSelectChannelConnector sslConnector = new SslSelectChannelConnector();
				sslConnector.setPort(sslPort);
				sslConnector.setKeystore(preferences.get("ssl.keystore", null));
				sslConnector.setPassword(preferences.get("ssl.password", null));
				sslConnector.setKeyPassword(preferences.get("ssl.keypassword", null));
				sslConnector.setMaxIdleTime(200000);
				sslConnector.setAcceptors(2);
				sslConnector.setStatsOn(false);
				sslConnector.setConfidentialPort(sslPort);
				sslConnector.setLowResourcesConnections(20000);
				sslConnector.setLowResourcesMaxIdleTime(5000);
				sslConnector.setForwarded(true);
				server.addConnector(sslConnector);
			}

			// tweak server config
			server.setSendServerVersion(true);
			server.setSendDateHeader(true); // required by some (older) browsers to support caching
			server.setGracefulShutdown(5000);

			// set thread pool
			final QueuedThreadPool threadPool = new QueuedThreadPool();
			threadPool.setName("jetty-server");
			server.setThreadPool(threadPool);

			// start the server
			server.start();

			// don't expose too detailed version info
			// (must be set after server started)
			HttpGenerator.setServerVersion("7");

			if (JettyDebug.debug) {
				LOG.debug("Jetty Server Started!");
				LOG.debug(server.dump());
			}
		} catch (final IllegalStateException e) {
			// wait for preferences to come up, retry later
			LOG.warn("Unable to start Jetty due to some inactive dependencies. Will retry in {} seconds. ({})", String.valueOf(delay / 1000), e.getMessage());
			schedule(delay);
			delay = delay < 300000 ? delay * 2 : 300000;
			return Status.CANCEL_STATUS;
		} catch (final Exception e) {
			// shutdown the Jetty does not come up
			LOG.error("Unable to start Jetty. Please check the log files. System will be shutdown.", e);
			ServerApplication.signalShutdown(new Exception("Could not start the Jetty server. " + ExceptionUtils.getRootCauseMessage(e), e));
			return Status.CANCEL_STATUS;
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}
}