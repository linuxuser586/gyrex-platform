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

import org.eclipse.gyrex.cloud.internal.CloudDebug;
import org.eclipse.gyrex.cloud.internal.locking.DurableLockImpl;
import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGateListener;
import org.eclipse.gyrex.cloud.services.locking.IDurableLock;
import org.eclipse.gyrex.cloud.services.locking.ILockMonitor;

import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class DurableLockTests {

	final class LockMonitorTestHelper implements ILockMonitor<IDurableLock> {

		final CountDownLatch lockReleasedLatch = new CountDownLatch(1);
		final CountDownLatch lockLostLatch = new CountDownLatch(1);
		final CountDownLatch lockAcquiredLatch = new CountDownLatch(1);

		@Override
		public void lockAcquired(final IDurableLock lock) {
			lockAcquiredLatch.countDown();
		}

		@Override
		public void lockLost(final IDurableLock lock) {
			lockLostLatch.countDown();
		}

		@Override
		public void lockReleased(final IDurableLock lock) {
			lockReleasedLatch.countDown();
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(DurableLockTests.class);

	private ScheduledExecutorService executorService;

	private Callable<DurableLockImpl> newAcquireLockCall(final DurableLockImpl lock, final long timeout) {
		return new Callable<DurableLockImpl>() {
			@Override
			public DurableLockImpl call() throws Exception {
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

		// configure debug output
		CloudDebug.debug = true;
		CloudDebug.zooKeeperLockService = true;
		CloudDebug.zooKeeperGateLifecycle = false;
		CloudDebug.zooKeeperServer = false;
		CloudDebug.nodeMetrics = false;
		CloudDebug.cloudState = false;
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
		final Future<DurableLockImpl> lock1 = executorService.submit(newAcquireLockCall(new DurableLockImpl(lockId, null), 0));

		final DurableLockImpl lock = lock1.get(15, TimeUnit.SECONDS);
		assertNotNull(lock);
		assertTrue(lock.isValid());

		lock.release();
		assertFalse(lock.isValid());
	}

	@Test
	public void testAcquire002() throws Exception {
		final String lockId = "test." + ZooKeeperGate.get().getSessionId() + "." + System.currentTimeMillis();

		final DurableLockImpl lock1 = new DurableLockImpl(lockId, null);
		final Future<DurableLockImpl> lock1f = executorService.submit(newAcquireLockCall(lock1, 1000));

		final DurableLockImpl lock1lock = lock1f.get(15, TimeUnit.SECONDS);
		assertNotNull(lock1lock);
		assertNotNull("lock1 must have a name at this point", lock1.getMyLockName());
		assertTrue(lock1lock.isValid());

		// check that's impossible to acquire a second log
		final DurableLockImpl lock2 = new DurableLockImpl(lockId, null);
		final Future<DurableLockImpl> lock2f = executorService.submit(newAcquireLockCall(lock2, 0));
		try {
			lock2f.get(10, TimeUnit.SECONDS);
			fail("timeout expected, call should never succeed");
		} catch (final TimeoutException e) {
			// good
		}
		assertNotNull("lock2 is still waiting so it must have a name", lock2.getMyLockName());

		// check that acquire timeouts work
		final DurableLockImpl lock3 = new DurableLockImpl(lockId, null);
		final Future<DurableLockImpl> lock3f = executorService.submit(newAcquireLockCall(lock3, 2000));
		try {
			lock3f.get(10, TimeUnit.SECONDS);
			fail("timeout expected, call should never succeed");
		} catch (final ExecutionException e) {
			// check exception
			assertTrue("timeout expected but wrong exception thrown", e.getCause() instanceof TimeoutException);
			// also check that no lock is left in ZooKeeper
			final Collection<String> childrenNames = ZooKeeperGate.get().readChildrenNames(IZooKeeperLayout.PATH_LOCKS_DURABLE.append(lockId), null);
			assertEquals("only two children are allowed for lock node", 2, childrenNames.size());
			assertTrue("lock2 must exist", childrenNames.contains(lock1.getMyLockName()));
			assertTrue("lock2 must exist", childrenNames.contains(lock2.getMyLockName()));
			assertNull("lock3 should not have a lock name anymore", lock3.getMyLockName());
		}

		// release lock 1
		lock1lock.release();
		assertFalse(lock1lock.isValid());

		// check lock 2 is now available
		final DurableLockImpl lock2lock = lock2f.get(10, TimeUnit.SECONDS);
		assertNotNull(lock2lock);
		assertTrue(lock2lock.isValid());

		// release lock 2
		lock2lock.release();
		assertFalse(lock2lock.isValid());
	}

	@Test
	public void testDisconnectAndRecover001() throws Exception {
		final String lockId = "test." + ZooKeeperGate.get().getSessionId() + "." + System.currentTimeMillis();
		LOG.info("Durable lock recovery test. Lock: {}", lockId);

		final Future<DurableLockImpl> lock1 = executorService.submit(newAcquireLockCall(new DurableLockImpl(lockId, null), 0));
		final DurableLockImpl lock = lock1.get(15, TimeUnit.SECONDS);
		assertNotNull(lock);
		assertTrue(lock.isValid());

		LOG.info("Acquired lock 1: {}", lock);

		// check for recovery key
		final String recoveryKey = lock.getRecoveryKey();
		assertNotNull(recoveryKey);

		// now kill the ZooKeeper connection
		// note, we must set a latch to "2" because connected is called twice
		// - first time when we register the monitor
		// - second time when the reconnected happened actually
		final CountDownLatch reconnected = new CountDownLatch(2);
		ZooKeeperGate.addConnectionMonitor(new ZooKeeperGateListener() {

			@Override
			public void gateDown(final ZooKeeperGate gate) {
				// empty
			}

			@Override
			public void gateRecovering(final ZooKeeperGate gate) {
				// TODO Auto-generated method stub

			}

			@Override
			public void gateUp(final ZooKeeperGate gate) {
				reconnected.countDown();
			}
		});

		LOG.info("Shutting down ZooKeeper gate");
		// TODO: we need to rework the test to test disconnect as well as session expired
		ZooKeeperGate.get().testShutdown();

		// lock must be invalid now
		assertFalse(lock.isValid());

		// wait reconnect
		reconnected.await(20, TimeUnit.SECONDS);
		LOG.info("Reconnected ZooKeeper gate");

		// must still be invalid
		assertFalse(lock.isValid());

		// attempt recovery
		LOG.info("Recovering lock: {}", lockId);
		final DurableLockImpl recoveredLock = new DurableLockImpl(lockId, null);
		recoveredLock.recover(recoveryKey);
		LOG.info("Durable lock recoverd: {}", recoveredLock);

		assertTrue(recoveredLock.isValid());
		assertNotNull(recoveredLock.getRecoveryKey());
		assertFalse("recovery keys should be different", StringUtils.equals(recoveryKey, recoveredLock.getRecoveryKey()));

	}

	@Test
	public void testKill001() throws Exception {
		final String lockId = "test." + ZooKeeperGate.get().getSessionId() + "." + System.currentTimeMillis();
		LOG.info("Durable lock recovery test. Lock: {}", lockId);

		final LockMonitorTestHelper lockMonitor = new LockMonitorTestHelper();
		final Future<DurableLockImpl> lock1 = executorService.submit(newAcquireLockCall(new DurableLockImpl(lockId, lockMonitor), 0));
		final DurableLockImpl lock = lock1.get(15, TimeUnit.SECONDS);
		assertNotNull(lock);
		assertTrue(lock.isValid());

		LOG.info("Acquired lock: {}", lock);

		// check for recovery key
		final String recoveryKey = lock.getRecoveryKey();
		assertNotNull(recoveryKey);

		// now kill the lock using ZooKeeper
		LOG.info("Deleting lock path in ZooKeeper");
		ZooKeeperGate.get().deletePath(IZooKeeperLayout.PATH_LOCKS_DURABLE.append(lockId).append(lock.getMyLockName()));

		// wait for event propagation
		// (the timing is a guess)
		try {
			lockMonitor.lockLostLatch.await(1, TimeUnit.MINUTES);
		} catch (final Exception e1) {
			// ignore
		}

		// lock must be invalid now
		assertFalse("lock must be invalid after remote kill", lock.isValid());

		// attempt recovery
		LOG.info("Testing tecovering lock: {}", lockId);
		final IDurableLock recoveredLock = new DurableLockImpl(lockId, null).recover(recoveryKey);
		assertNull("Should not be possible to recover a killed lock", recoveredLock);
	}
}
