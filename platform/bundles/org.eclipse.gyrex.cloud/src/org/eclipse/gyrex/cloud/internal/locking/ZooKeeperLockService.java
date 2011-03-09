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
package org.eclipse.gyrex.cloud.internal.locking;

import java.util.concurrent.TimeoutException;

import org.eclipse.gyrex.cloud.services.locking.IDurableLock;
import org.eclipse.gyrex.cloud.services.locking.IExclusiveLock;
import org.eclipse.gyrex.cloud.services.locking.ILockMonitor;
import org.eclipse.gyrex.cloud.services.locking.ILockService;

import org.apache.commons.lang.NotImplementedException;

/**
 * ZooKeeper based {@link ILockService} implementation.
 */
public class ZooKeeperLockService implements ILockService {

	@Override
	public IDurableLock acquireDurableLock(final String lockId, final ILockMonitor<IDurableLock> callback, final long timeout) throws InterruptedException, TimeoutException {
		throw new NotImplementedException();
	}

	@Override
	public IExclusiveLock acquireExclusiveLock(final String lockId, final ILockMonitor<IExclusiveLock> callback, final long timeout) throws InterruptedException, TimeoutException {
		return new ExclusiveLockImpl(lockId, callback).acquire(timeout);
	}

	@Override
	public IDurableLock recoverDurableLock(final String lockId, final ILockMonitor<IDurableLock> callback, final String recoveryKey) throws IllegalArgumentException {
		throw new NotImplementedException();
	}

}
