package org.eclipse.gyrex.cloud.internal.zk;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import org.eclipse.gyrex.cloud.internal.CloudDebug;
import org.eclipse.gyrex.cloud.internal.NodeInfo;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate.IConnectionMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An application which maintains the lifecycle of the ZooKeeper gate.
 */
public class ZooKeeperGateApplication implements IApplication {

	private final class ConnectRunnable implements Runnable, IConnectionMonitor {
		private static final int INITIAL_CONNECT_DELAY = 1000;
		private static final int MAX_CONNECT_DELAY = 240000;

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
		public void connected() {
			// reset delay on successful connect
			delay = INITIAL_CONNECT_DELAY;
		}

		@Override
		public void disconnected() {
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
			return delay < MAX_CONNECT_DELAY ? delay * 2 : MAX_CONNECT_DELAY;
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

	/** Exit object indicating error termination */
	private static final Integer EXIT_ERROR = new Integer(1);

	private static final AtomicReference<CountDownLatch> stopSignalRef = new AtomicReference<CountDownLatch>();
	private static final AtomicReference<Throwable> zkErrorRef = new AtomicReference<Throwable>();

	/**
	 * Force a shutdown of the ZooKeeper gate.
	 */
	public static void forceShutdown() {
		final CountDownLatch stopSignal = stopSignalRef.get();
		if (stopSignal != null) {
			stopSignal.countDown();
		}
	}

	boolean isActive() {
		final CountDownLatch stopSignal = stopSignalRef.get();
		return (stopSignal != null) && (stopSignal.getCount() > 0);
	}

	void scheduleConnect(final ScheduledExecutorService executor, final int delay, final ZooKeeperGateConfig config) {
		if (CloudDebug.zooKeeperGateLifecycle) {
			LOG.debug("Scheduling ZooKeeper connect attempt in {}s.", TimeUnit.MILLISECONDS.toSeconds(delay));
		}

		executor.schedule(new ConnectRunnable(executor, config, delay), delay, TimeUnit.MILLISECONDS);
	}

	void signalStopped(final Throwable zkError) {
		if (CloudDebug.zooKeeperGateLifecycle) {
			LOG.debug("Received stop signal for ZooKeeper Gate manager.");
		}
		final CountDownLatch signal = stopSignalRef.get();
		if (null != signal) {
			zkErrorRef.set(zkError);
			signal.countDown();
		}
	}

	@Override
	public Object start(final IApplicationContext context) throws Exception {
		if (CloudDebug.zooKeeperGateLifecycle) {
			LOG.debug("Starting ZooKeeper Gate manager.");
		}

		// create initial config
		final ZooKeeperGateConfig config = new ZooKeeperGateConfig(new NodeInfo());
		config.readFromPreferences();

		// set stop signal
		final CountDownLatch stopSignal = new CountDownLatch(1);
		if (!stopSignalRef.compareAndSet(null, stopSignal)) {
			throw new IllegalStateException("ZooKeeper Gate already running!");
		}

		try {
			// initialize executor
			final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
				@Override
				public Thread newThread(final Runnable r) {
					final Thread t = new Thread(r, "ZooKeeper Gate Connect Thread");
					t.setDaemon(true);
					return t;
				}
			});

			// kick off connection procedure
			scheduleConnect(executor, ConnectRunnable.INITIAL_CONNECT_DELAY, config);

			// signal running
			context.applicationRunning();

			// wait for stop
			try {
				stopSignal.await();
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			// shutdown executor
			executor.shutdownNow();

			// shutdown ZooKeeper if still running
			final ZooKeeperGate gate = ZooKeeperGate.getAndSet(null);
			if (gate != null) {
				gate.shutdown(true);
			}

			if (CloudDebug.zooKeeperGateLifecycle) {
				LOG.debug("ZooKeeper Gate manager shutdown complete.");
			}

			// exit
			final Throwable error = zkErrorRef.getAndSet(null);
			return error == null ? IApplication.EXIT_OK : EXIT_ERROR;

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
