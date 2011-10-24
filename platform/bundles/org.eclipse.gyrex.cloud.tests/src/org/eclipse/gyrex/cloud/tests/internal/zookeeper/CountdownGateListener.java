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

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGateListener;

import org.apache.zookeeper.ZooKeeper;

public final class CountdownGateListener implements ZooKeeperGateListener {

	private CountDownLatch upLatch;

	public CountdownGateListener() {
		reset();
	}

	@Override
	public synchronized void gateDown(final ZooKeeperGate gate) {
	}

	@Override
	public synchronized void gateRecovering(final ZooKeeperGate gate) {
	}

	@Override
	public synchronized void gateUp(final ZooKeeperGate gate) {
		upLatch.countDown();
	}

	private ZooKeeper getZooKeeperFromGate() throws IllegalStateException {
		try {
			final Method ensureConnected = ZooKeeperGate.class.getDeclaredMethod("ensureConnected");
			if (!ensureConnected.isAccessible()) {
				ensureConnected.setAccessible(true);
			}
			return (ZooKeeper) ensureConnected.invoke(ZooKeeperGate.get());
		} catch (final IllegalStateException e) {
			throw e;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void reset() {
		upLatch = new CountDownLatch(1);
	}

	public void waitForUp(final int timeout) throws InterruptedException, TimeoutException {
		ZooKeeperGate.addConnectionMonitor(this);
		try {
			getZooKeeperFromGate();
		} catch (final IllegalStateException e) {
			if (!upLatch.await(timeout, TimeUnit.MILLISECONDS)) {
				throw new TimeoutException("timeout waiting for gate to come up");
			}
		} finally {
			ZooKeeperGate.removeConnectionMonitor(this);
		}
	}

}