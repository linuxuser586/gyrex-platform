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
import java.io.IOException;
import java.util.Map;

import org.eclipse.gyrex.cloud.internal.CloudDebug;
import org.eclipse.gyrex.common.internal.applications.BaseApplication;
import org.eclipse.gyrex.server.Platform;

import org.apache.zookeeper.server.NIOServerCnxn;
import org.apache.zookeeper.server.PurgeTxnLog;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An application which starts a ZooKeeper server.
 */
public class ZooKeeperServerApplication extends BaseApplication {

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperServerApplication.class);

	static volatile ZooKeeperGateApplication connectedGateApplication;

	private NIOServerCnxn.Factory factory;
	private QuorumPeer quorumPeer;

	private ZooKeeperServer zkServer;

	/**
	 * Creates a new instance.
	 */
	public ZooKeeperServerApplication() {
		debug = CloudDebug.zooKeeperServer;
	}

	@Override
	protected void doStart(final Map arguments) throws Exception {
		// create config
		final ZooKeeperServerConfig config = new ZooKeeperServerConfig();

		// load config file if available
		final File zkConfigFile = Platform.getInstanceLocation().append("etc/zookeeper.properties").toFile();
		if (zkConfigFile.isFile() && zkConfigFile.canRead()) {
			final String zkConfigFilePath = zkConfigFile.getAbsolutePath();
			LOG.warn("Running ZooKeeper with external configuration: {}", zkConfigFilePath);
			try {
				config.parse(zkConfigFilePath);
			} catch (final ConfigException e) {
				LOG.error("Error in ZooKeeper configuration (file {}). {}", zkConfigFilePath, e.getMessage());
				throw e;
			}
		} else {
			if (CloudDebug.zooKeeperServer) {
				LOG.debug("Running with managed ZooKeeper configuration.");
			}
			config.readFromPreferences();
		}

		// check which server type to launch
		if (config.getServers().size() > 1) {
			runEnsemble(config);
		} else {
			runStandalone(config);
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
			factory.shutdown();
			factory = null;

			if (zkServer.isRunning()) {
				zkServer.shutdown();
			}
			zkServer = null;

			LOG.info("ZooKeeper server stopped.");
		}

		// shutdown ensemble node if still running
		if (null != quorumPeer) {
			if (CloudDebug.zooKeeperServer) {
				LOG.debug("Shutting down ensemble node...");
			}
			quorumPeer.shutdown();
			quorumPeer = null;
			LOG.info("ZooKeeper ensemble node stopped.");
		}

		return EXIT_OK;
	}

	@Override
	protected Logger getLogger() {
		return LOG;
	}

	private void runEnsemble(final ZooKeeperServerConfig config) throws IOException {
		/*
		 * note, we could migrate everything into preferences; but this requires
		 * to keep up with every ZooKeeper change;
		 * A ZooKeeper ensemble should be taken seriously. Having this protection in
		 * place ensures that administrator writes a ZooKeeper configuration file
		 * and understands the implications of this.
		 */
		// sanity check that config is not preferences based
		if (config.isPreferencesBased()) {
			throw new IllegalArgumentException("Please create a ZooKeeper configuration file in order to setup an ensamble.");
		}

		// get directories
		final File dataDir = new File(config.getDataLogDir());
		final File snapDir = new File(config.getDataDir());

		// clean old logs
		PurgeTxnLog.purge(dataDir, snapDir, 3);

		// create NIO factory
		final NIOServerCnxn.Factory cnxnFactory = new NIOServerCnxn.Factory(config.getClientPortAddress(), config.getMaxClientCnxns());

		// create server
		quorumPeer = new QuorumPeer();
		quorumPeer.setClientPortAddress(config.getClientPortAddress());
		quorumPeer.setTxnFactory(new FileTxnSnapLog(dataDir, snapDir));
		quorumPeer.setQuorumPeers(config.getServers());
		quorumPeer.setElectionType(config.getElectionAlg());
		quorumPeer.setMyid(config.getServerId());
		quorumPeer.setTickTime(config.getTickTime());
		quorumPeer.setMinSessionTimeout(config.getMinSessionTimeout());
		quorumPeer.setMaxSessionTimeout(config.getMaxSessionTimeout());
		quorumPeer.setInitLimit(config.getInitLimit());
		quorumPeer.setSyncLimit(config.getSyncLimit());
		quorumPeer.setQuorumVerifier(config.getQuorumVerifier());
		quorumPeer.setCnxnFactory(cnxnFactory);
		quorumPeer.setZKDatabase(new ZKDatabase(quorumPeer.getTxnFactory()));
		quorumPeer.setLearnerType(config.getPeerType());

		// start server
		LOG.info("Starting ZooKeeper ensemble node.");
		quorumPeer.start();
	}

	private void runStandalone(final ZooKeeperServerConfig config) throws IOException {
		// disable LOG4J JMX stuff
		System.setProperty("zookeeper.jmx.log4j.disable", Boolean.TRUE.toString());

		// get directories
		final File dataDir = new File(config.getDataLogDir());
		final File snapDir = new File(config.getDataDir());

		// clean old logs
		PurgeTxnLog.purge(dataDir, snapDir, 3);

		// create standalone server
		zkServer = new ZooKeeperServer();
		zkServer.setTxnLogFactory(new FileTxnSnapLog(dataDir, snapDir));
		zkServer.setTickTime(config.getTickTime());
		zkServer.setMinSessionTimeout(config.getMinSessionTimeout());
		zkServer.setMaxSessionTimeout(config.getMaxSessionTimeout());

		factory = new NIOServerCnxn.Factory(config.getClientPortAddress(), config.getMaxClientCnxns());

		// start server
		LOG.info("Starting ZooKeeper standalone server.");
		try {
			factory.startup(zkServer);
		} catch (final InterruptedException e) {
			LOG.warn("Interrupted during server start.", e);
			Thread.currentThread().interrupt();
		}
	}
}
