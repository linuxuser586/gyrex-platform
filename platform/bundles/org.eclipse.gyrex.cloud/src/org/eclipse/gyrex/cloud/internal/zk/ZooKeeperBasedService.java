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

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.gyrex.cloud.internal.CloudDebug;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate.IConnectionMonitor;

import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for ZooKeeper based service implementations.
 */
public abstract class ZooKeeperBasedService {

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperBasedService.class);

	private final AtomicBoolean closed = new AtomicBoolean(false);
	private final IConnectionMonitor connectionMonitor = new IConnectionMonitor() {
		private final AtomicBoolean everConnected = new AtomicBoolean();

		@Override
		public void connected(final ZooKeeperGate gate) {
			if (everConnected.compareAndSet(false, true)) {
				// ignore first connect call (this is the initial call)
			} else {
				reconnect();
			}
		}

		@Override
		public void disconnected(final ZooKeeperGate gate) {
			disconnect();
		}
	};

	private final long retryDelay;
	private final int retryCount;

	/**
	 * Creates a new instance.
	 */
	public ZooKeeperBasedService() {
		this(250l, 8);
	}

	/**
	 * Creates a new instance.
	 */
	public ZooKeeperBasedService(final long retryDelay, final int retryCount) {
		if (retryDelay < 50) {
			throw new IllegalArgumentException("retry delay to low");
		}
		if (retryCount < 1) {
			throw new IllegalArgumentException("retry count to low");
		}
		this.retryDelay = retryDelay;
		this.retryCount = retryCount;

		// hook connection monitor
		// (the assumption is that ZooKeeperGate is active when creating a ZooKeeperBasedService)
		ZooKeeperGate.addConnectionMonitor(connectionMonitor);
	}

	/**
	 * Closes the service.
	 */
	protected final void close() {
		if (closed.compareAndSet(false, true)) {
			try {
				doClose();
			} finally {
				ZooKeeperGate.removeConnectionMonitor(connectionMonitor);
			}
		}
	}

	/**
	 * Disconnects the service.
	 * <p>
	 * The default implementation just closes the service performing an
	 * irregular close. Subclasses may override and customize the behavior.
	 * </p>
	 */
	protected void disconnect() {
		// auto-close service on disconnect
		LOG.warn("Connection to the cloud has been lost. Closing active service {}.", ZooKeeperBasedService.this);
		close();
	}

	/**
	 * Called when the service closes as a result of an intentional close or
	 * unexpected network loss.
	 * <p>
	 * The default implementation does nothing. Subclasses may override and
	 * release any resources.
	 * </p>
	 * 
	 * @param regular
	 *            <code>true</code> if the close is a regular close, i.e. wanted
	 *            by a client and not because of a connection loss,
	 *            <code>false</code> otherwise
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
			} catch (final KeeperException.SessionExpiredException e) {
				LOG.warn("ZooKeeper session expired for service {}. The service may be invalid now.", this);
				// propagate this exception
				// (note, we rely on connectionMonitor to close the service)
				throw e;
			} catch (final KeeperException.ConnectionLossException e) {
				if (exception == null) {
					exception = e;
				}
				if (CloudDebug.debug) {
					LOG.debug("Connection to the server has been lost (retry attempt {}).", i);
				}
				sleep(i);
			}
		}
		throw exception;
	}

	/**
	 * Returns detail information for {@link #toString()}.
	 * 
	 * @return
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
	 * Reconnects the service.
	 * <p>
	 * The default implementation does nothing which basically means that
	 * reconnection is not supported. Subclasses may override and customize the
	 * behavior.
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
				final long sleepTime = attemptCount * retryDelay;
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
