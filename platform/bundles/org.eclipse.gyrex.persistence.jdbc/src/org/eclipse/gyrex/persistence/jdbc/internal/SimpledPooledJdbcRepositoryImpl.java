/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.jdbc.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

import org.eclipse.gyrex.persistence.jdbc.storage.JdbcRepository;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;

/**
 * A generic JDBC based repository.
 * <p>
 * This repository provides some basic pooling capabilities.
 * </p>
 */
public class SimpledPooledJdbcRepositoryImpl extends JdbcRepository {

	private final ConnectionPoolDataSource connectionPoolDataSource;

	private final ReentrantLock poolLock = new ReentrantLock();

	private volatile int maxPoolCapacity;
	private volatile int activeConnectionsCount;

	private final Set<PooledConnection> pooledConnections = new HashSet<PooledConnection>();
	private final Set<PooledConnection> activeConnections = new HashSet<PooledConnection>();
	private final Queue<PooledConnection> availableConnections = new ArrayDeque<PooledConnection>();

	private final ConnectionEventListener connectionEventListener = new ConnectionEventListener() {
		@Override
		public void connectionClosed(final ConnectionEvent event) {
			pooledConnectionClosed((PooledConnection) event.getSource(), false);
		}

		@Override
		public void connectionErrorOccurred(final ConnectionEvent event) {
			pooledConnectionClosed((PooledConnection) event.getSource(), true);
			getJdbcRepositoryMetrics().setSQLError("unknown", event.getSQLException());
		}
	};

	/**
	 * Creates a new instance.
	 * 
	 * @param repositoryId
	 *            the repository id
	 * @param repositoryType
	 *            the repository type
	 * @param connectionPoolDataSource
	 *            the configured data source for creating
	 *            {@link PooledConnection pooled connections}
	 */
	public SimpledPooledJdbcRepositoryImpl(final String repositoryId, final RepositoryProvider repositoryType, final ConnectionPoolDataSource connectionPoolDataSource, final int poolCapacity) {
		super(repositoryId, repositoryType, new SimplePooledJdbcRepositoryMetrics(createMetricsId(repositoryType, repositoryId), "open", "repository instance created", poolCapacity, 0));
		this.connectionPoolDataSource = connectionPoolDataSource;
		maxPoolCapacity = poolCapacity;
	}

	private void checkClosed() {
		if (isClosed()) {
			throw new IllegalStateException("repository '" + getRepositoryId() + "' closed");
		}
	}

	/**
	 * Closes the pooled connection.
	 * <p>
	 * Note, must be called when holding {@link #poolLock}.
	 * </p>
	 * 
	 * @param pooledConnection
	 */
	private void close(final PooledConnection pooledConnection) {
		// no longer track events
		pooledConnection.removeConnectionEventListener(connectionEventListener);

		// remove from the pool
		pooledConnections.remove(pooledConnection);

		// close the underlying connection
		try {
			pooledConnection.close();
		} catch (final SQLException e) {
			// we should eventually log this but it may just flood the logs
		}
	}

	/**
	 * Creates a new connection from the underlying data source and adds it to
	 * the pool.
	 * <p>
	 * Note, must be called when holding {@link #poolLock}.
	 * </p>
	 * 
	 * @return the created connection.
	 * @throws SQLException
	 *             if an error occured in the underlying pool data source while
	 *             creating the connection.
	 */
	private PooledConnection create() throws SQLException {
		// create connection
		final PooledConnection pooledConnection = connectionPoolDataSource.getPooledConnection();

		// add to the pool
		pooledConnections.add(pooledConnection);

		// track the connection
		pooledConnection.addConnectionEventListener(connectionEventListener);

		return pooledConnection;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.persistence.storage.Repository#doClose()
	 */
	@Override
	protected void doClose() {
		// close all connections
		final ReentrantLock lock = poolLock;
		lock.lock();
		try {
			// close all connections
			final PooledConnection[] connections = pooledConnections.toArray(new PooledConnection[0]);
			for (final PooledConnection pooledConnection : connections) {
				// passivate
				markInactive(pooledConnection);

				// close and remove from pool
				close(pooledConnection);
			}
		} finally {
			// release lock
			lock.unlock();
		}

		// update metrics
		getJdbcRepositoryMetrics().getPoolStatusMetric().setStatus("closed", "repository closed through API call");
	}

	/**
	 * Returns the active connections count.
	 * 
	 * @return the activeConnectionsCount
	 */
	public int getActiveConnectionsCount() {
		return activeConnectionsCount;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.persistence.jdbc.storage.JdbcRepository#getConnection()
	 */
	@Override
	public Connection getConnection() throws SQLException {
		checkClosed();
		final long start = System.currentTimeMillis();
		final ReentrantLock lock = poolLock;
		lock.lock();
		try {
			// get connection from pool
			final Connection connection = getPooledConnection();

			// update metrics
			if (null != connection) {
				getJdbcRepositoryMetrics().getPoolMetric().channelStarted(System.currentTimeMillis() - start);
			} else {
				getJdbcRepositoryMetrics().getPoolMetric().channelDenied();
			}

			// return connections
			return connection;
		} finally {
			lock.unlock();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.persistence.jdbc.storage.JdbcRepository#getConnection(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public Connection getConnection(final long timeout, final TimeUnit timeUnit) throws SQLException {
		checkClosed();
		final long start = System.currentTimeMillis();
		final ReentrantLock lock = poolLock;
		try {
			if (!lock.tryLock(timeout, timeUnit)) {

				// could not get lock -> abort

				// update metrics
				getJdbcRepositoryMetrics().getPoolMetric().channelDenied();

				// no connection
				return null;
			}
		} catch (final InterruptedException e) {
			// Restore the interrupted status
			Thread.currentThread().interrupt();

			// we have been interrupted while waiting on the lock,
			// we don't have the lock -> abort

			// update metrics
			getJdbcRepositoryMetrics().getPoolMetric().channelDenied();

			// no connection
			return null;
		}
		try {
			// get connection from pool
			final Connection connection = getPooledConnection();

			// update metrics
			if (null != connection) {
				getJdbcRepositoryMetrics().getPoolMetric().channelStarted(System.currentTimeMillis() - start);
			} else {
				getJdbcRepositoryMetrics().getPoolMetric().channelDenied();
			}

			// return connections
			return connection;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Creates and returns a Connection object that is a handle for the physical
	 * connection that the specified PooledConnection object represents.
	 * 
	 * @param pooledConnection
	 * @return the connection handle
	 * @throws SQLException
	 *             if a database access error occurs
	 */
	private Connection getConnection(final PooledConnection pooledConnection) throws SQLException {
		return pooledConnection.getConnection();
	}

	/**
	 * Returns the metrics as {@link SimplePooledJdbcRepositoryMetrics}.
	 * 
	 * @return the metrics as {@link SimplePooledJdbcRepositoryMetrics}
	 */
	public SimplePooledJdbcRepositoryMetrics getJdbcRepositoryMetrics() {
		return (SimplePooledJdbcRepositoryMetrics) getMetrics();
	}

	/**
	 * Returns a pooled connection.
	 * <p>
	 * The pool will dynamically grow if no connection is available in the pool.
	 * </p>
	 * <p>
	 * Note, must be called when holding {@link #poolLock}.
	 * </p>
	 * 
	 * @return a database connection
	 * @throws SQLException
	 */
	private Connection getPooledConnection() throws SQLException {
		PooledConnection pooledConnection = null;

		// re-use existing if possible
		if (activeConnectionsCount < maxPoolCapacity) {
			pooledConnection = availableConnections.poll();
			getJdbcRepositoryMetrics().getPoolMetric().channelBusy();
		}

		// create new if necessary
		if (null == pooledConnection) {
			pooledConnection = create();
		}

		// mark activate
		markActive(pooledConnection);

		// get real connection
		final Connection connection = getConnection(pooledConnection);

		// return the connection
		return connection;
	}

	/**
	 * Adds a connection to the list of active connections.
	 * <p>
	 * Note, must be called when holding {@link #poolLock}.
	 * </p>
	 * 
	 * @param pooledConnection
	 */
	private void markActive(final PooledConnection pooledConnection) {
		try {
			activeConnections.add(pooledConnection);
		} finally {
			activeConnectionsCount = activeConnections.size();
		}
	}

	/**
	 * Removes a connection from the list of active connections.
	 * <p>
	 * Note, must be called when holding {@link #poolLock}.
	 * </p>
	 * 
	 * @param pooledConnection
	 */
	private void markInactive(final PooledConnection pooledConnection) {
		try {
			activeConnections.remove(pooledConnection);
		} finally {
			activeConnectionsCount = activeConnections.size();
		}
	}

	/**
	 * Notifies the pool that a connection was "closed" by the application and
	 * releases it back to the pool if desired.
	 * 
	 * @param pooledConnection
	 * @param forceRemoveFromPool
	 */
	void pooledConnectionClosed(final PooledConnection pooledConnection, final boolean forceRemoveFromPool) {
		final ReentrantLock lock = poolLock;
		lock.lock();
		try {
			// remove from list of active connections
			markInactive(pooledConnection);

			if (forceRemoveFromPool || (activeConnectionsCount >= maxPoolCapacity)) {
				// remove it from the pool
				close(pooledConnection);
			} else {
				// keep for re-use
				availableConnections.add(pooledConnection);
				getJdbcRepositoryMetrics().getPoolMetric().channelIdle();
			}
		} finally {
			lock.unlock();
		}

		// update metric
		getJdbcRepositoryMetrics().getPoolMetric().channelFinished();
	}

	/**
	 * Sets the maximum pool size.
	 * <p>
	 * Note there is currently no hard limit. Whenever the pool limit is reach a
	 * new connection is created. However, only the number of connections set
	 * here will be kept open in the pool.
	 * </p>
	 * 
	 * @param maximumPoolSize
	 */
	public void setPoolCapacity(final int maximumPoolSize) {
		maxPoolCapacity = maximumPoolSize;
		getJdbcRepositoryMetrics().getPoolMetric().setChannelsCapacity(maximumPoolSize);
	}

}
