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
package org.eclipse.cloudfree.persistence.derby.tests;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.cloudfree.persistence.jdbc.internal.JdbcRepositoryImpl;
import org.eclipse.cloudfree.persistence.storage.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JdbcRepositoryTest {

	private MockRepositoryType mockRepositoryType;

	private final AtomicLong getConnectionDuration = new AtomicLong();
	private final AtomicLong getConnectionCount = new AtomicLong();
	private final AtomicLong closeConnectionDuration = new AtomicLong();
	private final AtomicLong closeConnectionCount = new AtomicLong();

	private final AtomicInteger activeConnectionConcurreny = new AtomicInteger();

	private void close(final JdbcRepositoryImpl repository) {
		try {
			repository.close();
		} catch (final Exception e) {
			fail("Repository did not close properly: " + e.getMessage());
		}
		assertTrue("repository not closed", repository.isClosed());
	}

	private void closeConnection(final Connection connection) {
		try {
			connection.close();
		} catch (final Exception e) {
			fail("error while closing connection: " + e.getMessage());
		}
	}

	private Callable<Exception> createConnectionCloseTask(final Future<Connection> connectionFuture, final JdbcRepositoryImpl repository) {
		return new Callable<Exception>() {
			@Override
			public Exception call() throws Exception {
				while (true) {
					try {
						updateConcurrency(repository);
						final Connection connection = connectionFuture.get();
						updateConcurrency(repository);
						//System.out.println("[closing connection]: " + connection);
						final long start = System.currentTimeMillis();
						closeConnection(connection);
						final long duration = System.currentTimeMillis() - start;
						closeConnectionDuration.addAndGet(duration);
						closeConnectionCount.incrementAndGet();
					} catch (final InterruptedException e) {
						// Restore the interrupted status
						Thread.currentThread().interrupt();
						continue;
					} catch (final CancellationException e) {
						// canceled
						return e;
					} catch (final ExecutionException e) {
						return e;
					}
					return null;
				}
			}
		};
	}

	private Callable<Connection> createGetConnectionTask(final JdbcRepositoryImpl repository, final CountDownLatch startSignal) {
		return new Callable<Connection>() {

			@Override
			public Connection call() throws Exception {
				if (null != startSignal) {
					// wait for the start
					try {
						startSignal.await();
					} catch (final InterruptedException e) {
						// Restore the interrupted status
						Thread.currentThread().interrupt();
					}
				}

				final long start = System.currentTimeMillis();
				final Connection connection = getConnection(repository);
				final long duration = System.currentTimeMillis() - start;
				getConnectionDuration.addAndGet(duration);
				getConnectionCount.incrementAndGet();
				updateConcurrency(repository);
				//System.out.println("[got connection]:" + connection);
				return connection;
			}
		};
	}

	private JdbcRepositoryImpl createRepository() {
		final Repository repository = mockRepositoryType.createRepositoryInstance("test", null);
		assertNotNull("repository must not be null", repository);
		assertTrue("repository is not a JdbcRepository", repository instanceof JdbcRepositoryImpl);
		return (JdbcRepositoryImpl) repository;
	}

	/**
	 * @param repository
	 * @return
	 * @throws SQLException
	 */
	private Connection getConnection(final JdbcRepositoryImpl repository) throws SQLException {
		final Connection connection = repository.getConnection();
		assertNotNull("no connection returned", connection);
		return connection;
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		mockRepositoryType = new MockRepositoryType();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		mockRepositoryType = null;
	}

	@Test
	public void testCloseRepository() throws Exception {
		final JdbcRepositoryImpl repository = createRepository();
		close(repository);
	}

	@Test
	public void testCreateRepository() throws Exception {
		final Repository repository = createRepository();

		try {
			repository.close();
		} catch (final Exception e) {
			// we only test create here
		}
	}

	@Test
	public void testGetConnectionFromRepository() throws Exception {
		final JdbcRepositoryImpl repository = createRepository();

		final Connection connection = getConnection(repository);

		assertEquals("active connections count is not correct", 1, repository.getActiveConnectionsCount());

		closeConnection(connection);

		assertEquals("active connections count is not correct", 0, repository.getActiveConnectionsCount());

		close(repository);
	}

	@Test
	public void testGetConnectionFromRepositoryMany() throws Exception {
		final JdbcRepositoryImpl repository = createRepository();

		final Connection conn1 = getConnection(repository);
		assertEquals("active connections count is not correct", 1, repository.getActiveConnectionsCount());
		final Connection conn2 = getConnection(repository);
		assertEquals("active connections count is not correct", 2, repository.getActiveConnectionsCount());
		final Connection conn3 = getConnection(repository);
		assertEquals("active connections count is not correct", 3, repository.getActiveConnectionsCount());
		final Connection conn4 = getConnection(repository);
		assertEquals("active connections count is not correct", 4, repository.getActiveConnectionsCount());

		closeConnection(conn4);
		assertEquals("active connections count is not correct", 3, repository.getActiveConnectionsCount());
		closeConnection(conn1);
		assertEquals("active connections count is not correct", 2, repository.getActiveConnectionsCount());
		closeConnection(conn3);
		assertEquals("active connections count is not correct", 1, repository.getActiveConnectionsCount());
		closeConnection(conn2);
		assertEquals("active connections count is not correct", 0, repository.getActiveConnectionsCount());

		close(repository);
	}

	private void testGetConnectionFromRepositoryParallel(final int maxConcurrency, final int taskToSchedule, final int poolCapacity) {
		final JdbcRepositoryImpl repository = createRepository();
		repository.setPoolCapacity(poolCapacity);

		final CountDownLatch startSignal = new CountDownLatch(1);

		final ExecutorService pool = maxConcurrency > 0 ? Executors.newFixedThreadPool(maxConcurrency) : Executors.newCachedThreadPool();
		final List<Future<Exception>> results = new ArrayList<Future<Exception>>(taskToSchedule);

		long start = System.currentTimeMillis();
		// execute parallel tasks with constant wait time
		for (int i = 0; i < taskToSchedule; i++) {
			final Future<Connection> connectionFuture = pool.submit(createGetConnectionTask(repository, startSignal));
			results.add(pool.submit(createConnectionCloseTask(connectionFuture, repository)));
		}
		long duration = System.currentTimeMillis() - start;
		final String SYSOUTPREFIX = "[testGetConnectionFromRepositoryParallel " + maxConcurrency + "/" + taskToSchedule + "/" + poolCapacity + "] ";
		System.out.println(SYSOUTPREFIX + taskToSchedule + " tasks submitted in: " + duration + "ms");

		// now signal the start
		startSignal.countDown();

		// wait till all operations finished
		start = System.currentTimeMillis();
		pool.shutdown();
		while (!pool.isTerminated()) {
			try {
				pool.awaitTermination(5, TimeUnit.SECONDS);
			} catch (final InterruptedException e) {
				// Restore the interrupted status
				Thread.currentThread().interrupt();
			}
		}
		duration = System.currentTimeMillis() - start;
		System.out.println(SYSOUTPREFIX + "pool shutdown in: " + duration + "ms");

		// now evaluate all results for errors
		for (final Iterator<Future<Exception>> stream = results.iterator(); stream.hasNext();) {
			final Future<Exception> result = stream.next();
			while (true) {
				try {
					final Exception e = result.get();
					if (null != e) {
						e.printStackTrace();
						fail("Error while retrieving connection: " + e.getMessage());
					}
				} catch (final InterruptedException e) {
					// Restore the interrupted status
					Thread.currentThread().interrupt();
					continue;
				} catch (final CancellationException e) {
					// ok
				} catch (final ExecutionException e) {
					e.printStackTrace();
					fail("Error while closing connection: " + e.getMessage());
				}
				break;
			}
		}

		assertEquals("active connections count is not correct", 0, repository.getActiveConnectionsCount());

		System.out.println(SYSOUTPREFIX + "max active connections: " + activeConnectionConcurreny.get());
		System.out.println(SYSOUTPREFIX + "average getConnection time: " + Math.round(getConnectionDuration.doubleValue() / getConnectionCount.doubleValue()) + "ms");
		System.out.println(SYSOUTPREFIX + "average closeConnection time: " + Math.round(closeConnectionDuration.doubleValue() / closeConnectionCount.doubleValue()) + "ms");

		System.out.print(SYSOUTPREFIX + repository.getJdbcRepositoryMetrics());

		close(repository);
	}

	@Test
	public void testGetConnectionFromRepositoryParallel_01() throws Exception {
		testGetConnectionFromRepositoryParallel(0, 100, 0);
	}

	@Test
	public void testGetConnectionFromRepositoryParallel_02() throws Exception {
		testGetConnectionFromRepositoryParallel(0, 1000, 0);
	}

	@Test
	public void testGetConnectionFromRepositoryParallel_03() throws Exception {
		testGetConnectionFromRepositoryParallel(100, 1000, 0);
	}

	@Test
	public void testGetConnectionFromRepositoryParallel_04() throws Exception {
		testGetConnectionFromRepositoryParallel(1000, 1000, 0);
	}

	@Test
	public void testGetConnectionFromRepositoryParallel_05() throws Exception {
		testGetConnectionFromRepositoryParallel(1000, 2000, 0);
	}

	@Test
	public void testGetConnectionFromRepositoryParallel_06a() throws Exception {
		testGetConnectionFromRepositoryParallel(1000, 10000, 0);
	}

	@Test
	public void testGetConnectionFromRepositoryParallel_06b() throws Exception {
		testGetConnectionFromRepositoryParallel(1000, 10000, 10);
	}

	@Test
	public void testGetConnectionFromRepositoryParallel_06c() throws Exception {
		testGetConnectionFromRepositoryParallel(1000, 10000, 100);
	}

	private int updateConcurrency(final JdbcRepositoryImpl repository) {
		final int activeConnectionsCount = repository.getActiveConnectionsCount();
		final int activeConnectionConcurrenyCurrent = activeConnectionConcurreny.get();
		if (activeConnectionConcurrenyCurrent < activeConnectionsCount) {
			if (activeConnectionConcurreny.compareAndSet(activeConnectionConcurrenyCurrent, activeConnectionsCount)) {
				//System.out.println("[concurrency]:" + activeConnectionsCount);
			}
		}
		//System.out.println("[concurrency]:" + activeConnectionsCount);
		return activeConnectionsCount;
	}
}
