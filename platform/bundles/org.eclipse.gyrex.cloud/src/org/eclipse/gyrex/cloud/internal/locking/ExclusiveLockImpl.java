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

import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate.IConnectionMonitor;
import org.eclipse.gyrex.cloud.services.locking.IExclusiveLock;

/**
 * Exclusive lock implementation.
 * <p>
 * This implementation is based on ZooKeeper globally synchronous lock recipe.
 * This recipe also ensures that only one thread can hold a lock at any point in
 * time. The protocol is as follows.
 * <ol>
 * <li>Call create( ) with a pathname of "_locknode_/lock-" and the sequence and
 * ephemeral flags set.</li>
 * <li>Call getChildren( ) on the lock node without setting the watch flag (this
 * is important to avoid the herd effect).</li>
 * <li>If the pathname created in step 1 has the lowest sequence number suffix,
 * the client has the lock and the client exits the protocol.</li>
 * <li>The client calls exists( ) with the watch flag set on the path in the
 * lock directory with the next lowest sequence number.</li>
 * <li>if exists( ) returns false, go to step 2. Otherwise, wait for a
 * notification for the pathname from the previous step before going to step 2.</li>
 * </ol>
 * <p>
 * The unlock protocol is very simple: clients wishing to release a lock simply
 * delete the node they created in step 1.
 * </p>
 * <p>
 * Here are a few things to notice:
 * <ul>
 * <li>The removal of a node will only cause one client to wake up since each
 * node is watched by exactly one client. In this way, you avoid the herd
 * effect.</li>
 * <li>There is no polling or timeouts.</li>
 * <li>Because of the way you implement locking, it is easy to see the amount of
 * lock contention, break locks, debug locking problems, etc.</li>
 * </ul>
 * </p>
 */
public class ExclusiveLockImpl implements IExclusiveLock, IConnectionMonitor {

	@Override
	public void connected(final ZooKeeperGate gate) {
		// TODO Auto-generated method stub

	}

	@Override
	public void disconnected(final ZooKeeperGate gate) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub

	}

}
