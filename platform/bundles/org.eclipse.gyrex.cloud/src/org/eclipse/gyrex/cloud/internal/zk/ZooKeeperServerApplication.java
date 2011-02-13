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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import org.eclipse.gyrex.cloud.internal.CloudDebug;
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
public class ZooKeeperServerApplication implements IApplication {

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperServerApplication.class);

	/** Exit object indicating error termination */
	private static final Integer EXIT_ERROR = Integer.valueOf(1);

	/** stopSignalRef */
	private static final AtomicReference<CountDownLatch> stopSignalRef = new AtomicReference<CountDownLatch>();

	private void runEnsemble(final ZooKeeperServerConfig config, final IApplicationContext context, final CountDownLatch stopSignal) throws IOException {
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
		final QuorumPeer quorumPeer = new QuorumPeer();
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

		// signal running
		context.applicationRunning();

		// wait for stop
		do {
			try {
				stopSignal.await();
			} catch (final InterruptedException e) {
				// reset interrupted state
				Thread.currentThread().interrupt();
			}
		} while ((stopSignal.getCount() > 0) && Thread.interrupted());

		// shutdown ZooKeeper if still running
		quorumPeer.shutdown();

		// clean logs
		PurgeTxnLog.purge(dataDir, snapDir, 3);

		LOG.info("ZooKeeper ensemble node stopped.");
	}

	private void runStandalone(final ZooKeeperServerConfig config, final IApplicationContext context, final CountDownLatch stopSignal) throws IOException {
		// disable LOG4J JMX stuff
		System.setProperty("zookeeper.jmx.log4j.disable", Boolean.TRUE.toString());

		// get directories
		final File dataDir = new File(config.getDataLogDir());
		final File snapDir = new File(config.getDataDir());

		// clean old logs
		PurgeTxnLog.purge(dataDir, snapDir, 3);

		// create server
		final ZooKeeperServer zkServer = new ZooKeeperServer();
		zkServer.setTxnLogFactory(new FileTxnSnapLog(dataDir, snapDir));
		zkServer.setTickTime(config.getTickTime());
		zkServer.setMinSessionTimeout(config.getMinSessionTimeout());
		zkServer.setMaxSessionTimeout(config.getMaxSessionTimeout());

		// create NIO factory
		final NIOServerCnxn.Factory factory = new NIOServerCnxn.Factory(config.getClientPortAddress(), config.getMaxClientCnxns());

		// start server
		LOG.info("Starting ZooKeeper standalone server.");
		try {
			factory.startup(zkServer);
		} catch (final InterruptedException e) {
			LOG.warn("Interrupted during server start.", e);
			Thread.currentThread().interrupt();
		}

		// signal running
		context.applicationRunning();

		// wait for stop
		do {
			try {
				stopSignal.await();
			} catch (final InterruptedException e) {
				// reset interrupted state
				Thread.currentThread().interrupt();
			}
		} while ((stopSignal.getCount() > 0) && Thread.interrupted());

		// shutdown ZooKeeper if still running
		factory.shutdown();

		// clean logs
		PurgeTxnLog.purge(dataDir, snapDir, 3);

		LOG.info("ZooKeeper server stopped.");
	}

	@Override
	public Object start(final IApplicationContext context) throws Exception {
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

		// set stop signal
		final CountDownLatch stopSignal = new CountDownLatch(1);
		if (!stopSignalRef.compareAndSet(null, stopSignal)) {
			throw new IllegalStateException("ZooKeeper server already running!");
		}

		try {
			// check which server type to launch
			if (config.getServers().size() > 1) {
				runEnsemble(config, context, stopSignal);
			} else {
				runStandalone(config, context, stopSignal);
			}

			// exit
			return IApplication.EXIT_OK;
		} catch (final Throwable e) {
			if (CloudDebug.zooKeeperServer) {
				LOG.error("Error while starting/running ZooKeeper: {}", e.getMessage(), e);
			} else {
				LOG.error("Error while starting/running ZooKeeper: {}", e.getMessage());
			}
			return EXIT_ERROR;
		} finally {
			// done, now reset signal to allow further starts
			stopSignalRef.compareAndSet(stopSignal, null);
		}
	}

	@Override
	public void stop() {
		if (CloudDebug.zooKeeperServer) {
			LOG.debug("Received stop signal for ZooKeeper server.");
		}
		final CountDownLatch signal = stopSignalRef.get();
		if (null != signal) {
			signal.countDown();
		}
	}

}
