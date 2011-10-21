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
package org.eclipse.gyrex.cloud.internal.zk;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Map;

import org.eclipse.gyrex.boot.internal.app.ServerApplication;
import org.eclipse.gyrex.cloud.internal.CloudActivator;
import org.eclipse.gyrex.cloud.internal.CloudDebug;
import org.eclipse.gyrex.common.internal.applications.BaseApplication;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.IPath;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.zookeeper.server.PurgeTxnLog;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An application which starts a ZooKeeper server.
 */
public class ZooKeeperServerApplication extends BaseApplication {

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperServerApplication.class);

	static volatile ZooKeeperGateApplication connectedGateApplication;

	private ZooKeeperServer zkServer;
	private Object factory;

	/**
	 * Creates a new instance.
	 */
	public ZooKeeperServerApplication() {
		debug = CloudDebug.zooKeeperServer;
	}

	private Object createFactory(final InetSocketAddress socketAddress, final int maxClientConnextions) throws Exception {
		// try ZooKeeper 3.4 way
		try {
			return CloudActivator.getInstance().getBundle().loadClass("org.apache.zookeeper.server.ServerCnxnFactory").getMethod("createFactory", InetSocketAddress.class, Integer.TYPE).invoke(null, socketAddress, maxClientConnextions);
		} catch (final Exception e) {
			LOG.debug("ZooKeeper 3.4 API not available. Falling back to ZooKeeper 3.3 API.");
		}

		// fallback to ZooKeeper 3.3 way
		// FIXME: remove this once we upgraded (bug 361524)
		try {
			return CloudActivator.getInstance().getBundle().loadClass("org.apache.zookeeper.server.NIOServerCnxn$Factory").getConstructor(InetSocketAddress.class, Integer.TYPE).newInstance(socketAddress, maxClientConnextions);
		} catch (final Exception e) {
			throw new IllegalStateException("Unable to create ZooKeeper NIO Factory using 3.3 API.", e);
		}
	}

	@Override
	protected void doStart(final Map arguments) throws Exception {
		try {
			runStandaloneEmbedded();
		} catch (final Exception e) {
			// shutdown the whole server
			if (Platform.inDevelopmentMode()) {
				ServerApplication.shutdown(new Exception("Could not start the embedded ZooKeeper server. " + ExceptionUtils.getRootCauseMessage(e), e));
			} else {
				LOG.error("Unable to start embedded ZooKeeper. {}", ExceptionUtils.getRootCauseMessage(e), e);
			}
			throw new StartAbortedException();
		}
	}

	@Override
	protected Object doStop() {
		// stop any running gate application first
		final ZooKeeperGateApplication gateApp = connectedGateApplication;
		if (null != gateApp) {
			connectedGateApplication = null;
			try {
				gateApp.stop();
			} catch (final Exception ignored) {
				// ignore
			}
		}

		// wait a little bit to let the server handle pending disconnects
		try {
			if (CloudDebug.zooKeeperServer) {
				LOG.debug("Preparing for ZooKeeper shutdown...");
			}
			Thread.sleep(250L);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		// shutdown standalone server if still running
		if (null != factory) {
			if (CloudDebug.zooKeeperServer) {
				LOG.debug("Shutting down standalone ZooKeeper server...");
			}
			try {
				factory.getClass().getMethod("shutdown").invoke(factory);
			} catch (final Exception e) {
				LOG.error("Error stopping server {}. {}", new Object[] { factory, ExceptionUtils.getRootCauseMessage(e), e });
			}
			factory = null;

			if (zkServer.isRunning()) {
				zkServer.shutdown();
			}
			zkServer = null;

			LOG.info("ZooKeeper server stopped.");
		}

		return EXIT_OK;
	}

	@Override
	protected Logger getLogger() {
		return LOG;
	}

	private void runStandaloneEmbedded() throws Exception {
		// disable LOG4J JMX stuff
		System.setProperty("zookeeper.jmx.log4j.disable", Boolean.TRUE.toString());

		// get directories
		final IPath zkBase = Platform.getInstanceLocation().append("zookeeper");
		final File dataDir = zkBase.toFile();
		final File snapDir = zkBase.append("logs").toFile();

		// clean old logs
		PurgeTxnLog.purge(dataDir, snapDir, 3);

		// create standalone server
		zkServer = new ZooKeeperServer();
		zkServer.setTxnLogFactory(new FileTxnSnapLog(dataDir, snapDir));

		// rely on defaults for the following values
		zkServer.setTickTime(ZooKeeperServer.DEFAULT_TICK_TIME);
		zkServer.setMinSessionTimeout(2 * ZooKeeperServer.DEFAULT_TICK_TIME);
		zkServer.setMaxSessionTimeout(10 * ZooKeeperServer.DEFAULT_TICK_TIME);

		// start factory on default port
		factory = createFactory(new InetSocketAddress(2181), 10);

		// start server
		LOG.info("Starting ZooKeeper standalone server.");
		factory.getClass().getMethod("startup", ZooKeeperServer.class).invoke(factory, zkServer);
	}
}
