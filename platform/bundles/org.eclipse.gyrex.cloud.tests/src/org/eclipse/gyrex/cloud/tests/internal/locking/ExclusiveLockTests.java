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
package org.eclipse.gyrex.cloud.tests.internal.locking;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.gyrex.cloud.internal.locking.ExclusiveLockImpl;
import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate.IConnectionMonitor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class ExclusiveLockTests {

	private ScheduledExecutorService executorService;

	private Callable<ExclusiveLockImpl> newAcquireLockCall(final ExclusiveLockImpl lock, final long timeout) {
		return new Callable<ExclusiveLockImpl>() {
			@Override
			public ExclusiveLockImpl call() throws Exception {
				lock.acquire(timeout);
				return lock;
			}
		};
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		executorService = Executors.newScheduledThreadPool(4);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		executorService.shutdownNow();
	}

	@Test
	public void testAcquire001() throws Exception {
		final String lockId = "test." + ZooKeeperGate.get().getSessionId() + "." + System.currentTimeMillis();
		final Future<ExclusiveLockImpl> lock1 = executorService.submit(newAcquireLockCall(new ExclusiveLockImpl(lockId, null), 0));

		final ExclusiveLockImpl lock = lock1.get(15, TimeUnit.SECONDS);
		assertNotNull(lock);
		assertTrue(lock.isValid());

		lock.release();
		assertFalse(lock.isValid());
	}

	@Test
	public void testAcquire002() throws Exception {
		final String lockId = "test." + ZooKeeperGate.get().getSessionId() + "." + System.currentTimeMillis();

		final ExclusiveLockImpl lock1 = new ExclusiveLockImpl(lockId, null);
		final Future<ExclusiveLockImpl> lock1f = executorService.submit(newAcquireLockCall(lock1, 1000));

		final ExclusiveLockImpl lock1lock = lock1f.get(15, TimeUnit.SECONDS);
		assertNotNull(lock1lock);
		assertNotNull("lock1 must have a name at this point", lock1.getMyLockName());
		assertTrue(lock1lock.isValid());

		// check that's impossible to acquire a second log
		final ExclusiveLockImpl lock2 = new ExclusiveLockImpl(lockId, null);
		final Future<ExclusiveLockImpl> lock2f = executorService.submit(newAcquireLockCall(lock2, 0));
		try {
			lock2f.get(10, TimeUnit.SECONDS);
			fail("timeout expected, call should never succeed");
		} catch (final TimeoutException e) {
			// good
		}
		assertNotNull("lock2 is still waiting so it must have a name", lock2.getMyLockName());

		// check that acquire timeouts work
		final ExclusiveLockImpl lock3 = new ExclusiveLockImpl(lockId, null);
		final Future<ExclusiveLockImpl> lock3f = executorService.submit(newAcquireLockCall(lock3, 2000));
		try {
			lock3f.get(10, TimeUnit.SECONDS);
			fail("timeout expected, call should never succeed");
		} catch (final ExecutionException e) {
			// check exception
			assertTrue("timeout expected but wrong exception thrown", e.getCause() instanceof TimeoutException);
			// also check that no lock is left in ZooKeeper
			final Collection<String> childrenNames = ZooKeeperGate.get().readChildrenNames(IZooKeeperLayout.PATH_LOCKS_EXCLUSIVE.append(lockId), null);
			assertEquals("only two children are allowed for lock node", 2, childrenNames.size());
			assertTrue("lock2 must exist", childrenNames.contains(lock1.getMyLockName()));
			assertTrue("lock2 must exist", childrenNames.contains(lock2.getMyLockName()));
			assertNull("lock3 should not have a lock name anymore", lock3.getMyLockName());
		}

		// release lock 1
		lock1lock.release();
		assertFalse(lock1lock.isValid());

		// check lock 2 is now available
		final ExclusiveLockImpl lock2lock = lock2f.get(10, TimeUnit.SECONDS);
		assertNotNull(lock2lock);
		assertTrue(lock2lock.isValid());

		// release lock 2
		lock2lock.release();
		assertFalse(lock2lock.isValid());
	}

	@Test
	public void testDisconnect001() throws Exception {
		final String lockId = "test." + ZooKeeperGate.get().getSessionId() + "." + System.currentTimeMillis();
		final Future<ExclusiveLockImpl> lock1 = executorService.submit(newAcquireLockCall(new ExclusiveLockImpl(lockId, null), 0));

		final ExclusiveLockImpl lock = lock1.get(15, TimeUnit.SECONDS);
		assertNotNull(lock);
		assertTrue(lock.isValid());

		// now kill the ZooKeeper connection
		// note, we must set a latch to "2" because connected is called twice
		// - first time when we register the monitor
		// - second time when the reconnected happened actually
		final CountDownLatch reconnected = new CountDownLatch(2);
		ZooKeeperGate.addConnectionMonitor(new IConnectionMonitor() {

			@Override
			public void connected(final ZooKeeperGate gate) {
				reconnected.countDown();
			}

			@Override
			public void disconnected(final ZooKeeperGate gate) {
				// empty
			}
		});
		ZooKeeperGate.get().testShutdown();

		// lock must be invalid now
		assertFalse(lock.isValid());

		// wait reconnect
		reconnected.await(20, TimeUnit.SECONDS);

		// must still be invalid
		assertFalse(lock.isValid());
	}
}
