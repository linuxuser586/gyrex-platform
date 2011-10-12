/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.jetty.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import org.eclipse.gyrex.cloud.environment.INodeEnvironment;
import org.eclipse.gyrex.http.internal.application.gateway.IHttpGateway;
import org.eclipse.gyrex.http.jetty.admin.ChannelDescriptor;
import org.eclipse.gyrex.http.jetty.admin.ICertificate;
import org.eclipse.gyrex.http.jetty.admin.IJettyManager;
import org.eclipse.gyrex.http.jetty.internal.app.JettyGateway;
import org.eclipse.gyrex.http.jetty.internal.connectors.CertificateSslContextFactory;
import org.eclipse.gyrex.preferences.CloudScope;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.jetty.http.HttpGenerator;
import org.eclipse.jetty.http.HttpSchemes;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JettyEngineApplication implements IApplication {

	private static final Logger LOG = LoggerFactory.getLogger(JettyEngineApplication.class);

	// OSGi Http Service suggest these properties for setting the default ports
	private static final String ORG_OSGI_SERVICE_HTTP_PORT = "org.osgi.service.http.port"; //$NON-NLS-1$
//	private static final String ORG_OSGI_SERVICE_HTTP_PORT_SECURE = "org.osgi.service.http.port.secure"; //$NON-NLS-1$

	// allow to disable NIO
	private static boolean noNio = Boolean.getBoolean("gyrex.jetty.nio.disabled");

	/** Exit object indicating error termination */
	private static final Integer EXIT_ERROR = Integer.valueOf(1);

	private static final AtomicReference<CountDownLatch> stopSignalRef = new AtomicReference<CountDownLatch>(null);
	private static final AtomicReference<Throwable> jettyErrorRef = new AtomicReference<Throwable>();

	/**
	 * Force a shutdown of the ZooKeeper gate.
	 */
	public static void forceShutdown() {
		final CountDownLatch stopSignal = stopSignalRef.get();
		if (stopSignal != null) {
			stopSignal.countDown();
		}
	}

	private void configureServer(final Server server) {
		if (JettyDebug.engine) {
			LOG.debug("Configuring server {}", server);
		}

		// collect node properties for filtering
		final Map<String, Object> nodeProperties = getNodeProperties();

		// create channels
		final IJettyManager jettyManager = HttpJettyActivator.getInstance().getJettyManager();
		final Collection<ChannelDescriptor> channels = jettyManager.getChannels();
		if (!channels.isEmpty()) {
			for (final ChannelDescriptor channel : channels) {
				createConnector(server, channel, jettyManager, nodeProperties);
			}
		} else {
			// start a default channel in development mode
			// or if OSGi property is explicitly set (bug 358859)
			if (Platform.inDevelopmentMode()) {
				final int port = Integer.getInteger(ORG_OSGI_SERVICE_HTTP_PORT, 8080);
				LOG.info("No channels configured. Enabling default channel on port {} in development mode.", port);
				final ChannelDescriptor defaultChannel = new ChannelDescriptor();
				defaultChannel.setId("default");
				defaultChannel.setPort(port);
				createConnector(server, defaultChannel, jettyManager, nodeProperties);
			} else if (null != Integer.getInteger(ORG_OSGI_SERVICE_HTTP_PORT)) {
				final int port = Integer.getInteger(ORG_OSGI_SERVICE_HTTP_PORT, 8080);
				LOG.info("No channels configured. Enabling channel on port {} configured via system property '{}'.", port, ORG_OSGI_SERVICE_HTTP_PORT);
				final ChannelDescriptor defaultChannel = new ChannelDescriptor();
				defaultChannel.setId("default");
				defaultChannel.setPort(port);
				createConnector(server, defaultChannel, jettyManager, nodeProperties);
			}
		}

		// tweak server
		server.setSendServerVersion(true);
		server.setSendDateHeader(true); // required by some (older) browsers to support caching
		server.setGracefulShutdown(5000);

		// set thread pool
		final QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setName("jetty-server");
		server.setThreadPool(threadPool);

		// configure thread monitor (if available)
		try {
			server.addBean(HttpJettyActivator.getInstance().getBundle().loadClass("org.eclipse.jetty.monitor.ThreadMonitor").newInstance());
		} catch (final ClassNotFoundException ignored) {
			LOG.warn("ThreadMonitor not found. Spinning thread detection won't be available.");
		} catch (final Exception e) {
			LOG.warn("Unable to initialize thread monitor. Spinning thread detection won't be available.", e);
		}
	}

	private void createConnector(final Server server, final ChannelDescriptor channel, final IJettyManager jettyManager, final Map<String, Object> nodeProperties) {
		if ((channel.getPort() <= 0) || (channel.getPort() > 65535)) {
			if (JettyDebug.engine) {
				LOG.debug("Ignoring disabled channel {}", channel);
			}
			return;
		}

		try {
			final String filter = channel.getNodeFilter();
			if (filter != null) {
				final Filter nodeFilter = FrameworkUtil.createFilter(filter);
				if (!nodeFilter.matches(nodeProperties)) {
					if (JettyDebug.engine) {
						LOG.debug("Ignoring channel {} which has a node filter that does not match this node.", channel);
					}
					return;
				}
			}

			if (JettyDebug.engine) {
				LOG.debug("Configuring channel {}", channel);
			}

			final AbstractConnector connector;

			if (channel.isSecure()) {
				final ICertificate certificate = jettyManager.getCertificate(channel.getCertificateId());
				connector = noNio ? new SslSocketConnector(new CertificateSslContextFactory(certificate)) : new SslSelectChannelConnector(new CertificateSslContextFactory(certificate));
			} else {
				connector = noNio ? new SocketConnector() : new SelectChannelConnector();
			}

			connector.setPort(channel.getPort());
			connector.setMaxIdleTime(200000);
			connector.setAcceptors(2);
			connector.setStatsOn(false);
			connector.setLowResourcesMaxIdleTime(5000);
			connector.setForwarded(true);
			if (connector instanceof SelectChannelConnector) {
				((SelectChannelConnector) connector).setLowResourcesConnections(20000);
			}

			if (null != channel.getSecureChannelId()) {
				final ChannelDescriptor secureChannel = jettyManager.getChannel(channel.getSecureChannelId());
				if (secureChannel != null) {
					connector.setConfidentialPort(secureChannel.getPort());
					connector.setConfidentialScheme(HttpSchemes.HTTPS);
					connector.setIntegralPort(secureChannel.getPort());
					connector.setIntegralScheme(HttpSchemes.HTTPS);
				}
			}

			server.addConnector(connector);
		} catch (final Exception e) {
			LOG.warn("Error configuring channel {}. Please check the channel configuration. {}", channel.getId(), ExceptionUtils.getRootCauseMessage(e));
		}
	}

	private Map<String, Object> getNodeProperties() {
		final INodeEnvironment nodeEnvironment = HttpJettyActivator.getInstance().getNodeEnvironment();
		final Map<String, Object> nodeProperties = new HashMap<String, Object>(2);
		nodeProperties.put("id", nodeEnvironment.getNodeId());
		final Set<String> tags = nodeEnvironment.getTags();
		if (!tags.isEmpty()) {
			nodeProperties.put("tag", tags.toArray(new String[tags.size()]));
		}
		return nodeProperties;
	}

	boolean isActive() {
		final CountDownLatch stopSignal = stopSignalRef.get();
		return (stopSignal != null) && (stopSignal.getCount() > 0);
	}

	void signalStopped(final Throwable jettyError) {
		if (JettyDebug.engine) {
			LOG.debug("Received stop signal for Jetty engine.");
		}
		final CountDownLatch signal = stopSignalRef.get();
		if (null != signal) {
			jettyErrorRef.set(jettyError);
			signal.countDown();
		}
	}

	@Override
	public Object start(final IApplicationContext context) throws Exception {
		if (JettyDebug.engine) {
			LOG.debug("Starting Jetty engine.");
		}

		// set stop signal
		final CountDownLatch stopSignal = new CountDownLatch(1);
		if (!stopSignalRef.compareAndSet(null, stopSignal)) {
			throw new IllegalStateException("Jetty engine already running!");
		}

		try {
			// FIXME timing issue with "ON_CLOUD_CONNECT" and ZooKeeperBasedPreferences
			// there is a bit of a timing issue here; we need to wait a bit in order
			// for the PlatformPreferences to be available
			int timeout = 5000;
			while (timeout > 0) {
				try {
					CloudScope.INSTANCE.getNode(HttpJettyActivator.SYMBOLIC_NAME);
					break;
				} catch (final IllegalStateException e) {
					if (JettyDebug.engine) {
						LOG.debug("Platform preferences not available. Jetty start will be delayed.");
						try {
							timeout -= 500;
							Thread.sleep(500);
						} catch (final Exception e1) {
							// interrupted
							Thread.currentThread().interrupt();
						}
					}
				}
			}

			// initialize (but do not start) the Jetty server
			final Server server = new Server();

			// create gateway
			JettyGateway gateway = new JettyGateway(server);

			// tweak server config
			configureServer(server);

			// start the server
			server.start();

			// don't expose too detailed version info
			// (must be set after server started)
			HttpGenerator.setServerVersion("7");

			if (JettyDebug.engine) {
				LOG.debug("Jetty server started!");
				LOG.debug(server.dump());
			}

			// activate HTTP gateway
			final ServiceRegistration gatewayServiceRegistration = HttpJettyActivator.getInstance().getServiceHelper().registerService(IHttpGateway.class.getName(), gateway, "Eclipse Gyrex", "Jetty based HTTP gateway.", null, null);

			if (JettyDebug.engine) {
				LOG.debug("Jetty HTTP gateway registered!");
				LOG.debug(server.dump());
			}

			// signal running
			context.applicationRunning();

			// wait for stop
			try {
				stopSignal.await();
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			// remove gateway
			gatewayServiceRegistration.unregister();

			// shutdown Jetty
			try {
				server.stop();
			} catch (final Exception e) {
				if (JettyDebug.debug) {
					LOG.warn("Error while stopping Jetty. {}", new Object[] { ExceptionUtils.getRootCauseMessage(e), e });
				} else {
					LOG.warn("Error while stopping Jetty. {}", ExceptionUtils.getRootCauseMessage(e));
				}
			}

			// destroy gateway
			if (null != gateway) {
				gateway.close();
				gateway = null;
			}

			if (JettyDebug.engine) {
				LOG.debug("Jetty engine shutdown complete.");
			}

			// exit
			final Throwable error = jettyErrorRef.getAndSet(null);
			return error == null ? IApplication.EXIT_OK : EXIT_ERROR;
		} catch (final Exception e) {
			// shutdown the whole server when Jetty does not come up
			LOG.error("Unable to start Jetty. Please check the log files. System will be shutdown.", e);
//			ServerApplication.signalShutdown(new Exception("Could not start the Jetty server. " + ExceptionUtils.getRootCauseMessage(e), e));
			return EXIT_ERROR;
		} finally {
			// done, now reset signal to allow further starts
			stopSignalRef.compareAndSet(stopSignal, null);
		}
	}

	@Override
	public void stop() {
		signalStopped(null);
	}

}
