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

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;

import junit.framework.AssertionFailedError;

import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;

import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.test.QuorumUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper for setting up a local ZK ensemble to test against.
 */
public class EnsembleHelper {
	private static final Logger LOG = LoggerFactory.getLogger(EnsembleHelper.class);

	private static QuorumUtil qu;

	public static synchronized void assertRunningAndConnected() throws Exception {
		if (qu == null) {
			fail("The ensamble is not started! Please run any ensamble test only from within the suite!");
		}

		findConnectedPeer();
	}

	public static synchronized int findConnectedPeer() throws Exception {
		final ZooKeeper zk = getZooKeeper();

		final Method testableRemoteSocketAddress = ZooKeeper.class.getDeclaredMethod("testableRemoteSocketAddress");
		if (!testableRemoteSocketAddress.isAccessible()) {
			testableRemoteSocketAddress.setAccessible(true);
		}

		final InetSocketAddress address = (InetSocketAddress) testableRemoteSocketAddress.invoke(zk);
		final int port = address.getPort();

		for (int i = 1; i <= qu.ALL; i++) {
			if (qu.getPeer(i).clientPort == port) {
				LOG.info("Killing peer {}", i);
				return i;
			}
		}

		throw new AssertionFailedError("not connected to any of the available peers");
	}

	/**
	 * Returns the qu.
	 * 
	 * @return the qu
	 */
	public static QuorumUtil getQuorumUtil() {
		assertNotNull("quorum not started", qu);
		return qu;
	}

	public static ZooKeeper getZooKeeper() throws IllegalStateException {
		try {
			final Method getZooKeeperMethod = ZooKeeperGate.class.getDeclaredMethod("getZooKeeper");
			if (!getZooKeeperMethod.isAccessible()) {
				getZooKeeperMethod.setAccessible(true);
			}
			return (ZooKeeper) getZooKeeperMethod.invoke(ZooKeeperGate.get());
		} catch (final IllegalStateException e) {
			throw e;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static synchronized void shutdown() {
		if (qu != null) {
			qu.shutdownAll();
			qu = null;
		}
	}

	public static synchronized void shutdownConnectedPeer() throws Exception {
		qu.shutdown(findConnectedPeer());
	}

	public static synchronized void startQuorum() throws Exception {
		if (null != qu) {
			throw new IllegalStateException("Already started!");
		}

		qu = new QuorumUtil(2);
		qu.startAll();
	}
}
