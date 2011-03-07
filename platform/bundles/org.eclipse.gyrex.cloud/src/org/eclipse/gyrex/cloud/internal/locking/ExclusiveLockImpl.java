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

import org.eclipse.gyrex.cloud.services.locking.IExclusiveLock;

/**
 * Exclusive lock implementation.
 * <p>
 * This implementation is based on ZooKeeper globally synchronous lock recipe.
 * </p>
 */
public class ExclusiveLockImpl implements IExclusiveLock {

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.cloud.services.locking.IDistributedLock#getId()
	 */
	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.cloud.services.locking.IDistributedLock#hasLock()
	 */
	@Override
	public boolean hasLock() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.cloud.services.locking.IDistributedLock#release()
	 */
	@Override
	public void release() {
		// TODO Auto-generated method stub

	}

}
