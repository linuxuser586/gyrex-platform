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

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.cloud.internal.CloudActivator;
import org.eclipse.gyrex.cloud.internal.CloudDebug;
import org.eclipse.gyrex.cloud.internal.NodeInfo;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate.IConnectionMonitor;
import org.eclipse.gyrex.common.internal.applications.BaseApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An application which maintains the lifecycle of the ZooKeeper gate.
 */
public class ZooKeeperGateApplication extends BaseApplication {

	private final class ConnectRunnable implements Runnable, IConnectionMonitor {
		private static final int INITIAL_CONNECT_DELAY = 1000;
		private static final int MAX_CONNECT_DELAY = 300000;

		private final ScheduledExecutorService executor;
		private final ZooKeeperGateConfig config;
		private volatile int delay;

		/**
		 * Creates a new instance.
		 * 
		 * @param executor
		 * @param config
		 * @param delay
		 */
		private ConnectRunnable(final ScheduledExecutorService executor, final ZooKeeperGateConfig config, final int delay) {
			this.executor = executor;
			this.config = config;
			this.delay = delay;
		}

		@Override
		public void connected(final ZooKeeperGate gate) {
			// reset delay on successful connect
			delay = INITIAL_CONNECT_DELAY;
		}

		@Override
		public void disconnected(final ZooKeeperGate gate) {
			if (CloudDebug.zooKeeperGateLifecycle) {
				LOG.debug("Processing disconnect event from gate.");
			}

			// clean-up old gate instance
			final ZooKeeperGate oldGate = ZooKeeperGate.getAndSet(null);
			if (oldGate != null) {
				oldGate.shutdown(false); // don't fire events here (recursion!)
			}

			// re-connect if active
			scheduleReconnectIfPossible();
		}

		private int nextDelay() {
			return Math.min(MAX_CONNECT_DELAY, delay * 2);
		}

		public void run() {
			if (CloudDebug.zooKeeperGateLifecycle) {
				LOG.debug("Connecting to ZooKeeper.");
			}
			ZooKeeperGate oldGate = null;
			try {
				oldGate = ZooKeeperGate.getAndSet(new ZooKeeperGate(config, this));
				if (CloudDebug.zooKeeperGateLifecycle) {
					LOG.debug("Successfully establish ZooKeeper connection. Gate is almost ready.");
				}
			} catch (final Exception e) {
				// retry connect if possible
				LOG.warn("Unable to establish ZooKeeper connection. Will retry later. {}", e.getMessage());
				scheduleReconnectIfPossible();
			} finally {
				// clean-up old gate instance
				if (oldGate != null) {
					oldGate.shutdown(true);
				}
			}

		}

		private void scheduleReconnectIfPossible() {
			if (isActive()) {
				if (CloudDebug.zooKeeperGateLifecycle) {
					LOG.debug("Will re-connect because ZooKeeper Gate manager is still active.");
				}
				scheduleConnect(executor, nextDelay(), config);
			}
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperGateApplication.class);

	private ScheduledExecutorService executor;

	/**
	 * Creates a new instance.
	 */
	public ZooKeeperGateApplication() {
		debug = CloudDebug.zooKeeperGateLifecycle;
	}

	@Override
	protected void doCleanup() {
		// unset gate application
		ZooKeeperServerApplication.connectedGateApplication = null;

		// ensure execute is stopped
		if (null != executor) {
			try {
				executor.shutdownNow();
			} catch (final Exception ignored) {
				// ignored
			}
			executor = null;
		}
	}

	@Override
	protected void doStart(final Map arguments) throws Exception {
		// create initial config
		final ZooKeeperGateConfig config = new ZooKeeperGateConfig(new NodeInfo());
		config.readFromPreferences();

		// initialize executor
		executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(final Runnable r) {
				final Thread t = new Thread(r, "ZooKeeper Gate Connect Thread");
				t.setDaemon(true);
				return t;
			}
		});

		// kick off connection procedure
		scheduleConnect(executor, ConnectRunnable.INITIAL_CONNECT_DELAY, config);

		// register with embedded server if running
		if (CloudActivator.getInstance().getNodeEnvironment().inStandaloneMode()) {
			ZooKeeperServerApplication.connectedGateApplication = this;
		}

		// schedule our ping monitor
		executor.scheduleWithFixedDelay(new ZooKeeperPinger(), 1, 5, TimeUnit.MINUTES);
	}

	@Override
	protected Object doStop() {
		// shutdown executor
		executor.shutdownNow();
		executor = null;

		// unset gate application
		ZooKeeperServerApplication.connectedGateApplication = null;

		// shutdown ZooKeeper if still running
		final ZooKeeperGate gate = ZooKeeperGate.getAndSet(null);
		if (gate != null) {
			gate.shutdown(true);
		}

		// exit
		return EXIT_OK;
	}

	@Override
	protected Logger getLogger() {
		return LOG;
	}

	void scheduleConnect(final ScheduledExecutorService executor, final int delay, final ZooKeeperGateConfig config) {
		if (CloudDebug.zooKeeperGateLifecycle) {
			LOG.debug("Scheduling ZooKeeper connect attempt in {}s.", TimeUnit.MILLISECONDS.toSeconds(delay));
		}

		executor.schedule(new ConnectRunnable(executor, config, delay), delay, TimeUnit.MILLISECONDS);
	}
}
