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
package org.eclipse.gyrex.cloud.internal.locking;

import java.util.concurrent.TimeoutException;

import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.services.locking.IDurableLock;
import org.eclipse.gyrex.cloud.services.locking.ILockMonitor;

/**
 * Durable lock implementation.
 */
public class DurableLockImpl extends ZooKeeperLock<IDurableLock> implements IDurableLock {

	/**
	 * Creates a new instance.
	 * 
	 * @param lockId
	 * @param lockMonitor
	 * @noreference This constructor is not intended to be referenced by
	 *              clients.
	 */
	public DurableLockImpl(final String lockId, final ILockMonitor<IDurableLock> lockMonitor) {
		super(lockId, lockMonitor, IZooKeeperLayout.PATH_LOCKS_DURABLE, false, true);
	}

	/**
	 * Tries to acquire the lock.
	 * <p>
	 * Note, this method <strong>must not</strong> be called concurrently be
	 * multiple threads!
	 * </p>
	 * 
	 * @param timeout
	 * @return the acquired lock
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public IDurableLock acquire(final long timeout) throws InterruptedException, TimeoutException {
		return acquire(timeout, false, null);
	}

	@Override
	public String getRecoveryKey() {
		final String recoveryKey = myRecoveryKey;
		if (null == recoveryKey) {
			throw new IllegalStateException("recovery key not available; lock must be acquired");
		}
		return recoveryKey;
	}

	/**
	 * Tries to recover the lock.
	 * 
	 * @param recoveryKey
	 * @return
	 * @throws TimeoutException
	 * @throws InterruptedException
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public IDurableLock recover(final String recoveryKey) {
		try {
			// we recover using a relatively small timeout
			// this is important because recovery should not
			// take forever and is expected to succeed during the first call
			return acquire(100, true, recoveryKey);
		} catch (final InterruptedException e) {
			throw new LockAcquirationFailedException(getId(), "Recovery operation has been interrupted.");
		} catch (final TimeoutException e) {
			throw new LockAcquirationFailedException(getId(), "Recovery operation has been timed out.");
		}
	}

}
