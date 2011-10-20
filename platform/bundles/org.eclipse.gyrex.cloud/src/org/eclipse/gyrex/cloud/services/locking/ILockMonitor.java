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

/**
 * A monitor which receives lock lifecycle notifications.
 * <p>
 * This interface may be implemented by clients.
 * </p>
 * 
 * @param <L>
 *            the lock type
 */
public interface ILockMonitor<L extends IDistributedLock> {

	/**
	 * Called when the lock has been acquired successfully.
	 * 
	 * @param lock
	 *            the lock
	 */
	void lockAcquired(L lock);

	/**
	 * Called when the lock has been lost.
	 * <p>
	 * In a distributed system this may typically happen in case of network
	 * interruptions or other lock service death. In any case clients should
	 * prepare for immediate cancellation of their activities which require the
	 * lock.
	 * </p>
	 * 
	 * @param lock
	 *            the lock
	 */
	void lockLost(L lock);

	/**
	 * Called when the lock has been released successfully.
	 * 
	 * @param lock
	 *            the lock
	 */
	void lockReleased(L lock);

	/**
	 * Called when the lock has been suspended (likely due to a connection
	 * interruption).
	 * <p>
	 * Suspended locks are in an on-hold state. While suspended no other system
	 * might get a lock, i.e. it might still be held. However, clients may also
	 * want to suspend their work until it is either
	 * {@link #lockAcquired(IDistributedLock) resumed} or
	 * {@link #lockLost(IDistributedLock) lost}.
	 * </p>
	 * 
	 * @param lock
	 *            the lock
	 */
	void lockSuspended(L lock);
}
