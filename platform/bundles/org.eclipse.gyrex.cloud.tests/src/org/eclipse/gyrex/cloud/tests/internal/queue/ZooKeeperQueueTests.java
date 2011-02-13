/**
 * Copyright (c) 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.cloud.tests.internal.queue;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collection;

import org.eclipse.gyrex.cloud.internal.queue.ZooKeeperQueue;
import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;

import org.eclipse.core.runtime.IPath;

import org.apache.zookeeper.CreateMode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class ZooKeeperQueueTests {

	/** TEST */
	private static final String QUEUE_ID = "test";
	private ZooKeeperQueue queue;
	private IPath queuePath;

	@Before
	public void setUp() throws Exception {
		queuePath = IZooKeeperLayout.PATH_QUEUES_ROOT.append(QUEUE_ID);

		// cleanup old data
		ZooKeeperGate zk = null;
		int count = 10;
		while (zk == null) {
			try {
				zk = ZooKeeperGate.get();
			} catch (final IllegalStateException e) {
				if (count <= 0) {
					throw e;
				}
				count--;
				Thread.sleep(250);
			}
		}
		if (zk.exists(queuePath)) {
			zk.deletePath(queuePath);
		}

		// create new queue
		zk.createPath(queuePath, CreateMode.PERSISTENT, "Test Queue");

		queue = new ZooKeeperQueue(QUEUE_ID);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		ZooKeeperGate.get().deletePath(IZooKeeperLayout.PATH_QUEUES_ROOT.append(QUEUE_ID));
	}

	/**
	 * Test method for
	 * {@link org.eclipse.gyrex.cloud.internal.queue.ZooKeeperQueue#consumeMessage(long, java.util.concurrent.TimeUnit)}
	 * .
	 */
	@Test
	public void testConsumeMessage() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.eclipse.gyrex.cloud.internal.queue.ZooKeeperQueue#deleteMessage(org.eclipse.gyrex.cloud.services.queue.IMessage)}
	 * .
	 */
	@Test
	public void testDeleteMessage() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.eclipse.gyrex.cloud.internal.queue.ZooKeeperQueue#receiveMessages(int, java.util.Map)}
	 * .
	 */
	@Test
	public void testReceiveMessages() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.eclipse.gyrex.cloud.internal.queue.ZooKeeperQueue#sendMessage(byte[])}
	 * .
	 */
	@Test
	public void testSendMessage() throws Exception {

		// send message
		queue.sendMessage("Hallo".getBytes());

		final Collection<String> names = ZooKeeperGate.get().readChildrenNames(queuePath, null);
		assertEquals("queue size", 1, names.size());
	}

}
