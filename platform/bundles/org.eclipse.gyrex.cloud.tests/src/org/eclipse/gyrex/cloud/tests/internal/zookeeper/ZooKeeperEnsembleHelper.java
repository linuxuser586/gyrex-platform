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
package org.eclipse.gyrex.cloud.tests.internal.zookeeper;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.eclipse.gyrex.cloud.tests.internal.CloudTestsActivator;
import org.eclipse.gyrex.server.Platform;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;

/**
 * Helper for setting up a local ZK ensemble
 */
public class ZooKeeperEnsembleHelper {

	/** ensemble size */
	private static final int ENSEMBLE_SIZE = 3;

	private static List<Process> peers;

	private static void createDir(final File baseDir) {
		if (!baseDir.mkdirs()) {
			throw new IllegalStateException(String.format("Unable to create directory '%s'. Please ensure that the location is writeble for the server.", baseDir));
		}
	}

	private static String[] generateCommandLine(final int serverId, final File zooCfg) {
		final File zkInstall = getZooKeeperInstall();

		File zkServerScript;
		if (isWindows()) {
			zkServerScript = new File(zkInstall, "bin/zkServer.cmd");
		} else {
			zkServerScript = new File(zkInstall, "bin/zkServer.sh");
		}

		if (!zkServerScript.isFile()) {
			throw new IllegalStateException(String.format("ZooKeeper start script at '%s' not found!", zkServerScript.getAbsolutePath()));
		}

		final List<String> commandLine = new ArrayList<String>();
		if (isWindows()) {
			commandLine.add("cmd");
			commandLine.add("/C");
			commandLine.add(String.format("start \"ZK %d\" \"%s\"", serverId, zkServerScript.getAbsolutePath()));
		} else {
			commandLine.add("bash");
			commandLine.add(zkServerScript.getAbsolutePath());
		}
		return commandLine.toArray(new String[commandLine.size()]);
	}

	private static String[] generateEnvironment(final File zooCfg, final File zooLogDir) {
		final List<String> env = new ArrayList<String>();
		for (final Entry<String, String> entry : System.getenv().entrySet()) {
			env.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
		}
		;
		env.add(String.format("ZOOCFG=%s", zooCfg.getAbsolutePath()));
		env.add(String.format("ZOO_LOG_DIR=%s", zooLogDir.getAbsolutePath()));
		return env.toArray(new String[env.size()]);
	}

	private static File getBaseDir() {
		return Platform.getStateLocation(CloudTestsActivator.getInstance().getBundle()).append("zk").toFile();
	}

	public static synchronized String getClientConnectString() {
		if (null == peers) {
			throw new IllegalStateException("Not started!");
		}

		final StringBuilder connectString = new StringBuilder();
		for (int i = 1; i <= peers.size(); i++) {
			if (i > 1) {
				connectString.append(',');
			}
			connectString.append(String.format("127.0.0.1:%d", getClientPort(i)));
		}
		return connectString.toString();
	}

	public static int getClientPort(final int serverId) {
		return (serverId * 10000) + 2181;
	}

	private static Object getServerElectionPort(final int serverId) {
		return (serverId * 10000) + 3888;
	}

	private static Object getServerPort(final int serverId) {
		return (serverId * 10000) + 2888;
	}

	private static File getZooKeeperInstall() {
		final String zkInstall = System.getenv("ZOOKEEPER_INSTALL");
		if (StringUtils.isBlank(zkInstall)) {
			throw new IllegalStateException("Please set environment variable ZOOKEEPER_INSTALL pointing to the extracted ZooKeeper tar ball.");
		}
		return new File(zkInstall);
	}

	private static boolean isWindows() {
		return org.eclipse.core.runtime.Platform.OS_WIN32.equals(org.eclipse.core.runtime.Platform.getOS());
	}

	public static synchronized void startEnsemble() throws Exception {

		if (null != peers) {
			throw new IllegalStateException("Already started!");
		}

		peers = new ArrayList<Process>(ENSEMBLE_SIZE);

		// cleanup directory
		final File baseDir = getBaseDir();
		if (baseDir.exists()) {
			FileUtils.deleteQuietly(baseDir);
		}
		createDir(baseDir);

		final Properties properties = new Properties();
		for (int i = 1; i <= ENSEMBLE_SIZE; i++) {
			properties.put(String.format("server.%d", i), String.format("127.0.0.1:%d:%d", getServerPort(i), getServerElectionPort(i)));
		}

		properties.put("clientPortAddress", "127.0.0.1");

		properties.put("tickTime", "2000");
		properties.put("initLimit", "5");
		properties.put("syncLimit", "2");

		for (int i = 1; i <= ENSEMBLE_SIZE; i++) {
			final File dataDir = new File(baseDir, String.format("%02d_data", i));
			final File snapDir = new File(baseDir, String.format("%02d_logs", i));
			createDir(dataDir);
			createDir(snapDir);
			properties.put("dataDir", dataDir.getAbsolutePath());
			properties.put("dataLogDir", snapDir.getAbsolutePath());

			properties.put("clientPort", String.valueOf(getClientPort(i)));

			// create node id
			FileUtils.write(new File(dataDir, "myid"), String.valueOf(i), CharEncoding.US_ASCII);

			// write config file
			final File zooCfg = new File(dataDir, "zoo.cfg");
			final FileOutputStream zooCfgOut = FileUtils.openOutputStream(zooCfg);
			properties.store(zooCfgOut, "generate ZK config");
			zooCfgOut.close();

			// start process
			final Process peer = Runtime.getRuntime().exec(generateCommandLine(i, zooCfg), generateEnvironment(zooCfg, dataDir), getZooKeeperInstall());

//			final QuorumPeerConfig config = new QuorumPeerConfig();
//			config.parseProperties(properties);
//
//			// create NIO factory
//			final NIOServerCnxn.Factory cnxnFactory = new NIOServerCnxn.Factory(config.getClientPortAddress(), config.getMaxClientCnxns());
//
//			// create server
//			final QuorumPeer quorumPeer = new QuorumPeer();
//			quorumPeer.setClientPortAddress(config.getClientPortAddress());
//			quorumPeer.setTxnFactory(new FileTxnSnapLog(dataDir, snapDir));
//			quorumPeer.setQuorumPeers(config.getServers());
//			quorumPeer.setElectionType(config.getElectionAlg());
//			quorumPeer.setMyid(config.getServerId());
//			quorumPeer.setTickTime(config.getTickTime());
//			quorumPeer.setMinSessionTimeout(config.getMinSessionTimeout());
//			quorumPeer.setMaxSessionTimeout(config.getMaxSessionTimeout());
//			quorumPeer.setInitLimit(config.getInitLimit());
//			quorumPeer.setSyncLimit(config.getSyncLimit());
//			quorumPeer.setQuorumVerifier(config.getQuorumVerifier());
//			quorumPeer.setCnxnFactory(cnxnFactory);
//			quorumPeer.setZKDatabase(new ZKDatabase(quorumPeer.getTxnFactory()));
//			quorumPeer.setLearnerType(config.getPeerType());
//
//			// start server
//			LOG.info("Starting ZooKeeper ensemble node {}.", i);
//			quorumPeer.start();
//
			peers.add(peer);
		}

	}

	public static synchronized void stopEnsemble() throws Exception {
		if (null == peers) {
			return;
		}

		for (final Process peer : peers) {
			peer.destroy();
		}
		peers = null;
	}
}
