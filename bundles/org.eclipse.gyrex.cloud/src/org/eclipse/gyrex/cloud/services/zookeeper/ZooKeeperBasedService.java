/*******************************************************************************
 * Copyright (c) 2011, 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.services.zookeeper;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.gyrex.cloud.internal.CloudDebug;
import org.eclipse.gyrex.cloud.internal.zk.GateDownException;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGateListener;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for ZooKeeper based service implementations.
 * <p>
 * This class provides the following convenience infrastructure for working with
 * {@link ZooKeeper} in Gyrex.
 * <ul>
 * <li>Automatic retry handling via {@link #execute(Callable)}</li>
 * <li>Access to the {@link ZooKeeper} via {@link ZooKeeperCallable}</li>
 * <li>Life-cycle control using {@link #activate()} and {@link #close()}</li>
 * <li>Connection handling via {@link #suspend()}, {@link #reconnect()} and
 * {@link #disconnect()}</li>
 * </ul>
 * </p>
 */
public abstract class ZooKeeperBasedService {

	/**
	 * Convenience {@link Callable} that provides direct access to
	 * {@link ZooKeeper}.
	 * 
	 * @param <V>
	 */
	protected static abstract class ZooKeeperCallable<V> implements Callable<V> {
		@Override
		public final V call() throws Exception {
			return call(ZooKeeperGate.get().getZooKeeper());
		}

		protected abstract V call(ZooKeeper keeper) throws Exception;
	}

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperBasedService.class);

	private final AtomicBoolean closed = new AtomicBoolean(false);
	private final ZooKeeperGateListener connectionMonitor = new ZooKeeperGateListener() {
		@Override
		public void gateDown(final ZooKeeperGate gate) {
			if (!isClosed()) {
				disconnect();
			}
		}

		@Override
		public void gateRecovering(final ZooKeeperGate gate) {
			if (!isClosed()) {
				suspend();
			}
		}

		@Override
		public synchronized void gateUp(final ZooKeeperGate gate) {
			if (!isClosed()) {
				// synchronized on the monitor in order to prevent entering #reconnect while processing RECOVERING event
				synchronized (this) {
					if (!isClosed()) {
						reconnect();
					}
				}
			}
		}

		@Override
		public String toString() {
			return String.format("ZooKeeperGateListener {%s}", ZooKeeperBasedService.this);
		};
	};

	private final long retryDelayInMs;
	private final int retryCount;
	private final ExecutorService executor;

	/**
	 * Creates a new instance using a default retry delay of 250ms and a retry
	 * count of 8.
	 * 
	 * @see ZooKeeperBasedService#ZooKeeperBasedService(long, int)
	 */
	public ZooKeeperBasedService() {
		this(250l, 8);
	}

	/**
	 * Creates a new instance.
	 * <p>
	 * Note, this will not activate the service. Sub-classes must activate the
	 * service by calling {@link #activate()} when appropriate. This can happen
	 * from within the constructor (after calling this constructor using
	 * <code>super(...)</code>) or lazily when active connection traction
	 * becomes necessary.
	 * </p>
	 * 
	 * @param retryDelayInMs
	 *            the retry delay in milliseconds (must be greater than or equal
	 *            to 50)
	 * @param retryCount
	 *            the number of retries to perform
	 */
	public ZooKeeperBasedService(final long retryDelayInMs, final int retryCount) {
		if (retryDelayInMs < 50)
			throw new IllegalArgumentException("retry delay to low");
		if (retryCount < 1)
			throw new IllegalArgumentException("retry count to low");
		this.retryDelayInMs = retryDelayInMs;
		this.retryCount = retryCount;
		executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(final Runnable r) {
				final Thread t = new Thread(r, String.format("%s Deferred Executor", ZooKeeperBasedService.this));
				t.setDaemon(true);
				t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
					@Override
					public void uncaughtException(final Thread t, final Throwable e) {
						LOG.error("Unhandled error processing operation in ({}). {}", ZooKeeperBasedService.this, ExceptionUtils.getRootCauseMessage(e), e);
					}
				});
				return t;
			}
		});
	}

	/**
	 * Activates the service.
	 * <p>
	 * This must be called when the service is ready in order to register
	 * necessary listener with the ZooKeeper client for proper connection
	 * tracking.
	 * </p>
	 */
	protected final void activate() {
		// don't do anything if closed
		if (isClosed())
			return;

		// hook connection monitor
		// (the assumption is that ZooKeeperGate is active when creating a ZooKeeperBasedService)
		ZooKeeperGate.addConnectionMonitor(connectionMonitor);
	}

	/**
	 * Closes the service.
	 * <p>
	 * After closing a service it must be considered not useable anymore.
	 * </p>
	 */
	protected final void close() {
		if (closed.compareAndSet(false, true)) {
			try {
				try {
					// abort any running executions
					executor.shutdown();
				} finally {
					// close
					doClose();
				}
			} finally {
				ZooKeeperGate.removeConnectionMonitor(connectionMonitor);
			}
		}
	}

	/**
	 * Disconnects the service.
	 * <p>
	 * This method is invoked in re-action to a gate DOWN event.
	 * </p>
	 * <p>
	 * The default implementation calls {@link #close()} which closes the
	 * service. Subclasses may override and customize the behavior.
	 * </p>
	 */
	protected void disconnect() {
		// don't do anything if already closed (#close may trigger this when removing the connection monitor)
		if (isClosed())
			return;

		// auto-close service on disconnect
		LOG.warn("Connection to the cloud has been lost. Closing active service {}.", ZooKeeperBasedService.this);
		close();
	}

	/**
	 * Called by {@link #close()} in order to close the service.
	 * <p>
	 * This may be a result of an intentional close or unexpected network loss.
	 * Thus, clients should be prepared that the ZooKeeper is not available
	 * anymore at this point.
	 * </p>
	 * <p>
	 * The default implementation does nothing. Subclasses may override and
	 * release any resources in order to allow the service being garbage
	 * collected.
	 * </p>
	 */
	protected void doClose() {
		// empty
	}

	/**
	 * Executes the specified operation.
	 * <p>
	 * Note, the operation is executed regardless of the service
	 * {@link #isClosed() closed state}. Clients should check
	 * {@link #isClosed()}in the beginning of the operation.
	 * </p>
	 * 
	 * @param operation
	 *            the operation to execute
	 * @return the result of the specified operation
	 * @throws Exception
	 *             if unable to compute a result
	 */
	protected <V> V execute(final Callable<V> operation) throws Exception {
		KeeperException exception = null;
		for (int i = 0; i < retryCount; i++) {
			try {
				return operation.call();
			} catch (final KeeperException.ConnectionLossException e) {
				if (exception == null) {
					exception = e;
				}
				if (CloudDebug.debug) {
					LOG.debug("Connection to the server has been lost (retry attempt {}).", i);
				}
				sleep(i);
			} catch (final KeeperException.SessionExpiredException e) {
				// we rely on connectionMonitor to close the service
				if (!isClosed()) {
					LOG.warn("ZooKeeper session expired. Service {} may be invalid now.", this);
				}
				// propagate this exception
				throw e;
			} catch (final GateDownException e) {
				// we rely on connectionMonitor to close the service
				if (!isClosed()) {
					LOG.warn("ZooKeeper gate is down. Service {} may be invalid now.", this);
				}
				// propagate this exception
				throw e;
			}
		}
		throw exception;
	}

	/**
	 * Returns detail information for {@link #toString()}.
	 * <p>
	 * This method is called from {@link #toString()} in order to build a more
	 * detailed {@link #toString()} info. Sub-classes must implemented and
	 * should return additional details.
	 * </p>
	 * 
	 * @return detail information for {@link #toString()}.
	 */
	protected abstract String getToStringDetails();

	/**
	 * Indicates if the service has been closed.
	 * 
	 * @return <code>true</code> if the service is closed, <code>false</code>
	 *         otherwise
	 */
	protected final boolean isClosed() {
		return closed.get();
	}

	/**
	 * Connects the service.
	 * <p>
	 * This method is invoked in re-action to a gate UP event. It indicates that
	 * the connection to ZooKeeper has been established (again).
	 * </p>
	 * <p>
	 * The default implementation does nothing. Subclasses may override and
	 * customize the behavior.
	 * </p>
	 */
	protected void reconnect() {

	}

	/**
	 * Suspends execution of the current thread if the attempt count is greater
	 * than zero.
	 * <p>
	 * 
	 * @param attemptCount
	 *            the number of the attempts performed so far
	 */
	protected final void sleep(final int attemptCount) {
		if (attemptCount > 0) {
			try {
				final long sleepTime = attemptCount * retryDelayInMs;
				if (CloudDebug.debug) {
					LOG.debug("Will sleep for {}ms.", sleepTime);
				}
				Thread.sleep(sleepTime);
			} catch (final InterruptedException e) {
				if (CloudDebug.debug) {
					LOG.debug("Sleep interrupted.");
				}
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Submits the specified operation for later execution (asynchronous
	 * processing).
	 * <p>
	 * Note, the operation is submitted regardless of the service
	 * {@link #isClosed() closed state}. Clients should check
	 * {@link #isClosed()} in the beginning of the operation.
	 * </p>
	 * <p>
	 * Many operations may be submitted. However, they are not executed in
	 * parallel, i.e. only one at a time.
	 * </p>
	 * 
	 * @param operation
	 *            the operation to execute
	 * @return a {@link Future} to access the result of the specified operation
	 */
	protected <V> Future<V> submit(final Callable<V> operation) {
		return executor.submit(new Callable<V>() {
			@Override
			public V call() throws Exception {
				return execute(operation);
			}
		});
	}

	/**
	 * Suspends the service.
	 * <p>
	 * This method is invoked in re-action to a gate RECOVERING event. The
	 * connection to ZooKeeper won't be available at this point. In case the
	 * service operates with active state (eg. ephemeral nodes) important
	 * operations might be suspended till the connection is re-established.
	 * </p>
	 * <p>
	 * The default implementation calls {@link #disconnect()}. Subclasses must
	 * override and customize the behavior if they want to support the suspended
	 * state.
	 * </p>
	 */
	protected void suspend() {
		disconnect();
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName());
		if (isClosed()) {
			builder.append(" CLOSED");
		}
		builder.append(" [").append(getToStringDetails()).append("]");
		return builder.toString();
	}

}
