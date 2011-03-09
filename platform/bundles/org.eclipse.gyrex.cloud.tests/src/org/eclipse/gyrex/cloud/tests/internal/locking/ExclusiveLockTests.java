/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.tests.internal.locking;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.gyrex.cloud.internal.locking.ExclusiveLockImpl;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class ExclusiveLockTests {

	private ScheduledExecutorService executorService;

	private Callable<ExclusiveLockImpl> newAcquireLockCall(final String lockId, final long timeout) {
		return new Callable<ExclusiveLockImpl>() {
			@Override
			public ExclusiveLockImpl call() throws Exception {
				final ExclusiveLockImpl lock = new ExclusiveLockImpl(lockId, null);
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
		final Future<ExclusiveLockImpl> lock1 = executorService.submit(newAcquireLockCall(lockId, 0));

		final ExclusiveLockImpl lock = lock1.get(15, TimeUnit.SECONDS);
		assertNotNull(lock);
		assertTrue(lock.isValid());

		lock.release();
		assertFalse(lock.isValid());
	}

	@Test
	public void testAcquire002() throws Exception {
		final String lockId = "test." + ZooKeeperGate.get().getSessionId() + "." + System.currentTimeMillis();
		final Future<ExclusiveLockImpl> lock1 = executorService.submit(newAcquireLockCall(lockId, 1000));

		final ExclusiveLockImpl lock1lock = lock1.get(15, TimeUnit.SECONDS);
		assertNotNull(lock1lock);
		assertTrue(lock1lock.isValid());

		// check that's impossible to acquire a second log
		final Future<ExclusiveLockImpl> lock2 = executorService.submit(newAcquireLockCall(lockId, 0));
		try {
			lock2.get(10, TimeUnit.SECONDS);
			fail("timeout expected, call should never succeed");
		} catch (final TimeoutException e) {
			// good
		}

		// check that acquire timeouts work
		final Future<ExclusiveLockImpl> lock3 = executorService.submit(newAcquireLockCall(lockId, 2000));
		try {
			lock3.get(10, TimeUnit.SECONDS);
			fail("timeout expected, call should never succeed");
		} catch (final ExecutionException e) {
			assertTrue("timeout expected but wrong exception thrown", e.getCause() instanceof TimeoutException);
		}

		// release lock 1
		lock1lock.release();
		assertFalse(lock1lock.isValid());

		// check lock 2 is now available
		final ExclusiveLockImpl lock2lock = lock2.get(10, TimeUnit.SECONDS);
		assertNotNull(lock2lock);
		assertTrue(lock2lock.isValid());

		// release lock 2
		lock2lock.release();
		assertFalse(lock2lock.isValid());
	}
}
