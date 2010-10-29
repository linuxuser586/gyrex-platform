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
import org.apache.zookeeper.server.NIOServerCnxn.Factory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An application which starts a ZooKeeper server.
 */
public class ZooKeeperServerApplication implements IApplication {

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperServerApplication.class);

	/** Exit object indicating error termination */
	private static final Integer EXIT_ERROR = new Integer(1);

	private static final AtomicReference<CountDownLatch> stopSignalRef = new AtomicReference<CountDownLatch>();
	private static final AtomicReference<Throwable> zkErrorRef = new AtomicReference<Throwable>();
	private static final AtomicReference<ThreadGroup> zkThreadGroup = new AtomicReference<ThreadGroup>();

	static final AtomicReference<Factory> cnxnFactoryRef = new AtomicReference<Factory>();

	private Thread createZooKeeperServerThread(final ZooKeeperServerConfig config) {
		final ThreadGroup threadGroup = new ThreadGroup("ZooKeeper Server");
		threadGroup.setDaemon(true);
		if (!zkThreadGroup.compareAndSet(null, threadGroup)) {
			threadGroup.destroy();
			throw new IllegalStateException("Thread group already created!");
		}
		final Thread thread = new Thread(threadGroup, "ZooKeeper Server Runner") {
			@Override
			public void run() {
				try {
					// disable LOG4J JMX stuff
					System.setProperty("zookeeper.jmx.log4j.disable", Boolean.TRUE.toString());

					// check which server type to launch
					if (config.getServers().size() > 1) {
						runEnsemble(config);
					} else {
						runStandalone(config);
					}
					signalStopped(null);
				} catch (final Throwable e) {
					if (CloudDebug.zooKeeperServer) {
						LOG.error("Error while starting/running ZooKeeper: {}", e.getMessage(), e);
					} else {
						LOG.warn("Error while starting/running ZooKeeper: {}", e.getMessage());
					}
					signalStopped(e);
				}
			}

			private void runEnsemble(final ZooKeeperServerConfig config) {
//				final QuorumPeerMain zkServer = new QuorumPeerMain();
//				zkServer.runFromConfig(config);
				// TODO implement ensample

				// start server
				LOG.info("Starting ZooKeeper ensamble server.");

				// reset factory ref
				cnxnFactoryRef.set(null);

				LOG.info("ZooKeeper server stopped.");

			}

			private void runStandalone(final ZooKeeperServerConfig config) throws IOException, InterruptedException {
				// create server
				final ZooKeeperServer zkServer = new ZooKeeperServer();
				zkServer.setTxnLogFactory(new FileTxnSnapLog(new File(config.getDataLogDir()), new File(config.getDataDir())));
				zkServer.setTickTime(config.getTickTime());
				zkServer.setMinSessionTimeout(config.getMinSessionTimeout());
				zkServer.setMaxSessionTimeout(config.getMaxSessionTimeout());

				// create NIO factory
				final NIOServerCnxn.Factory factory = new NIOServerCnxn.Factory(config.getClientPortAddress(), config.getMaxClientCnxns());
				if (!cnxnFactoryRef.compareAndSet(null, factory)) {
					throw new IllegalStateException("ZooKeeper already/still running");
				}

				// start server
				LOG.info("Starting ZooKeeper standalone server.");
				factory.startup(zkServer);
				factory.join();
				if (zkServer.isRunning()) {
					zkServer.shutdown();
				}

				// reset factory ref
				cnxnFactoryRef.set(null);

				LOG.info("ZooKeeper server stopped.");
			}
		};
		return thread;
	}

	void signalStopped(final Throwable zkError) {
		if (CloudDebug.zooKeeperServer) {
			LOG.debug("Received stop signal for ZooKeeper server.");
		}
		final CountDownLatch signal = stopSignalRef.get();
		if (null != signal) {
			zkErrorRef.set(zkError);
			signal.countDown();
		}
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

		// create && start ZooKeeper server thread
		final Thread zkServerThread = createZooKeeperServerThread(config);
		zkServerThread.start();

		// signal running
		context.applicationRunning();

		// wait for stop
		try {
			stopSignal.await();
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		// shutdown ZooKeeper if still running
		final Factory zkServer = cnxnFactoryRef.getAndSet(null);
		if (zkServer != null) {
			zkServer.shutdown();
		}

		final ThreadGroup threadGroup = zkThreadGroup.getAndSet(null);
		if (threadGroup != null) {
			try {
				threadGroup.destroy();
			} catch (final Exception e) {
				LOG.warn("Error while destroying thread group. {}", e.getMessage());
			}
		}

		// exit
		final Throwable error = zkErrorRef.getAndSet(null);
		return error == null ? IApplication.EXIT_OK : EXIT_ERROR;
	}

	@Override
	public void stop() {
		signalStopped(null);
	}

}
