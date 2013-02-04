/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
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
import org.eclipse.gyrex.cloud.services.locking.IExclusiveLock;
import org.eclipse.gyrex.cloud.services.locking.ILockMonitor;

/**
 * Exclusive lock implementation.
 */
public class ExclusiveLockImpl extends ZooKeeperLock<IExclusiveLock> implements IExclusiveLock {

	/**
	 * Creates a new instance.
	 * 
	 * @param lockId
	 * @param lockMonitor
	 * @noreference This constructor is not intended to be referenced by
	 *              clients.
	 */
	public ExclusiveLockImpl(final String lockId, final ILockMonitor<IExclusiveLock> lockMonitor) {
		super(lockId, lockMonitor, IZooKeeperLayout.PATH_LOCKS_EXCLUSIVE, true, false);
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
	public IExclusiveLock acquire(final long timeout) throws InterruptedException, TimeoutException {
		return acquire(timeout, false, null);
	}

}
