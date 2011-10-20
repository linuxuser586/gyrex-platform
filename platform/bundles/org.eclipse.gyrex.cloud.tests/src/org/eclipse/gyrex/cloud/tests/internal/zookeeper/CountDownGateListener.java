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
package org.eclipse.gyrex.cloud.tests.internal.zookeeper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGateListener;

public final class CountDownGateListener implements ZooKeeperGateListener {

	private CountDownLatch upLatch;
	private CountDownLatch downLatch;
	private CountDownLatch recoveringLatch;

	public CountDownGateListener() {
		reset();
	}

	public boolean awaitDown(final int timeout) throws InterruptedException {
		return downLatch.await(timeout, TimeUnit.MILLISECONDS);
	}

	public boolean awaitRecovering(final int timeout) throws InterruptedException {
		return recoveringLatch.await(timeout, TimeUnit.MILLISECONDS);
	}

	public boolean awaitUp(final int timeout) throws InterruptedException {
		return upLatch.await(timeout, TimeUnit.MILLISECONDS);
	}

	@Override
	public synchronized void gateDown(final ZooKeeperGate gate) {
		downLatch.countDown();
	}

	@Override
	public synchronized void gateRecovering(final ZooKeeperGate gate) {
		recoveringLatch.countDown();
	}

	@Override
	public synchronized void gateUp(final ZooKeeperGate gate) {
		upLatch.countDown();
	}

	public synchronized void reset() {
		upLatch = new CountDownLatch(1);
		downLatch = new CountDownLatch(1);
		recoveringLatch = new CountDownLatch(1);
	}
}