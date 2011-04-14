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
package org.eclipse.gyrex.cloud.services.locking;

import java.util.concurrent.TimeoutException;

/**
 * A simple locking service.
 * <p>
 * A locking service allows to create distributed locks which guarantee that at
 * any point into time no two clients hold the same lock. It provides different
 * type of locks for various use cases. However, it does not define any strong
 * guarantees about typical lock characteristics (eg., fairness) but an
 * underlying implementation may do so.
 * </p>
 * <p>
 * It is very important that acquired locks eventually get released. Calls to
 * release should be done in a finally block to ensure they execute.
 * </p>
 * 
 * <pre>
 * ILockService lockService = ...;
 * ILock lock = lockService.acquire...(...);
 * try {
 *   // ... do work here ...
 * } finally {
 *   lock.release();
 * }
 * </pre>
 * <p>
 * This interface is typically not implemented by clients but by service
 * providers. As such it is considered part of a service provider API which may
 * evolve faster than the general API. Please get in touch with the development
 * team through the prefered channels listed on <a
 * href="http://www.eclipse.org/gyrex">the Gyrex website</a> to stay up-to-date
 * of possible changes for implementors.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ILockService {

	/**
	 * Acquires the specified durable lock.
	 * <p>
	 * If the specified lock is in use and the specified timeout is greater than
	 * zero, the calling thread will block until one of the following happens:
	 * <ul>
	 * <li>This lock is available</li>
	 * <li>The thread is interrupted</li>
	 * <li>The specified timeout has elapsed</li>
	 * </ul>
	 * If the specified lock is in use and the specified timeout is equal to or
	 * less than zero, the calling thread will block until the lock becomes
	 * available.
	 * </p>
	 * 
	 * @param lockId
	 *            the lock identifier
	 * @param callback
	 *            a callback interface which will receive important lock
	 *            lifecycle notifications
	 * @param timeout
	 *            the maximum wait time in milliseconds for acquiring the lock
	 * @return the acquired lock
	 * @throws InterruptedException
	 *             if the current thread has been interrupted while waiting for
	 *             the lock
	 * @throws TimeoutException
	 *             if the wait timed out
	 */
	IDurableLock acquireDurableLock(String lockId, ILockMonitor<IDurableLock> callback, long timeout) throws InterruptedException, TimeoutException;

	/**
	 * Acquires the specified exclusive lock.
	 * <p>
	 * If the specified lock is in use and the specified timeout is greater than
	 * zero, the calling thread will block until one of the following happens:
	 * <ul>
	 * <li>This lock is available</li>
	 * <li>The thread is interrupted</li>
	 * <li>The specified timeout has elapsed</li>
	 * </ul>
	 * If the specified lock is in use and the specified timeout is equal to or
	 * less than zero, the calling thread will block until the lock becomes
	 * available.
	 * </p>
	 * 
	 * @param lockId
	 *            the lock identifier
	 * @param callback
	 *            a callback interface which will receive important lock
	 *            lifecycle notifications
	 * @param timeout
	 *            the maximum wait time in milliseconds for acquiring the lock
	 * @return the acquired lock
	 * @throws InterruptedException
	 *             if the current thread has been interrupted while waiting for
	 *             the lock
	 * @throws TimeoutException
	 *             if the wait timed out
	 */
	IExclusiveLock acquireExclusiveLock(String lockId, ILockMonitor<IExclusiveLock> callback, long timeout) throws InterruptedException, TimeoutException;

	/**
	 * Attempts recovery of the specified durable lock.
	 * <p>
	 * This method may be used to recover a
	 * {@link #acquireDurableLock(String, ILockMonitor, long) previously
	 * acquired} durable lock. When this method succeeds the lock will be
	 * recovered to the caller. If the lock is still in use it will be taken
	 * away from the existing client.
	 * </p>
	 * <p>
	 * If the lock does not exists anymore (eg., deleted by an administrator),
	 * <code>null</code> will be returned.
	 * </p>
	 * <p>
	 * If the lock exists but the recovery key does not match an
	 * {@link IllegalStateException} will be thrown.
	 * </p>
	 * 
	 * @param lockId
	 *            the lock identifier
	 * @param callback
	 *            a callback interface which will receive important lock
	 *            lifecycle notifications
	 * @param recoveryKey
	 *            the recovery key for the lock (obtained previously form
	 *            {@link IDurableLock#getRecoveryKey()})
	 * @return the recovered durable lock (may be <code>null</code> if the lock
	 *         does not exist)
	 */
	IDurableLock recoverDurableLock(String lockId, ILockMonitor<IDurableLock> callback, String recoveryKey);

}
