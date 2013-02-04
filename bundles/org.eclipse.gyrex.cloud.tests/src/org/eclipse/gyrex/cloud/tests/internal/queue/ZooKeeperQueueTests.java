/**
 * Copyright (c) 2011, 2012 Gunnar Wagenknecht and others.
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
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.cloud.internal.queue.ZooKeeperQueue;
import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.services.queue.IMessage;
import org.eclipse.gyrex.cloud.services.queue.IQueueServiceProperties;
import org.eclipse.gyrex.junit.GyrexServerResource;

import org.eclipse.core.runtime.IPath;

import org.apache.zookeeper.CreateMode;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public class ZooKeeperQueueTests {

	@ClassRule
	public static final GyrexServerResource server = new GyrexServerResource();

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
				if (count <= 0)
					throw e;
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

	@Test
	public void test001SendMessage() throws Exception {
		final byte[] message = "Hallo ".concat(String.valueOf(System.currentTimeMillis())).getBytes();

		// send message
		queue.sendMessage(message);

		// check that queue is not empty
		final Collection<String> names = ZooKeeperGate.get().readChildrenNames(queuePath, null);
		assertEquals("queue size", 1, names.size());
	}

	@Test
	public void test002ConsumeMessage() throws Exception {
		final byte[] message = "Hallo ".concat(String.valueOf(System.currentTimeMillis())).getBytes();

		// send message
		queue.sendMessage(message);

		// consume message
		final IMessage consumedMessage = queue.consumeMessage(5, TimeUnit.SECONDS);
		assertNotNull("message must not be null", consumedMessage);
		assertTrue("message content must match", Arrays.equals(message, consumedMessage.getBody()));

		// check that queue is empty
		final Collection<String> names = ZooKeeperGate.get().readChildrenNames(queuePath, null);
		assertTrue("queue should be empty after consume", names.isEmpty());

		// second consume must result null
		assertNull("no message should be in queue", queue.consumeMessage(1, TimeUnit.SECONDS));
	}

	@Test
	public void test003ReceiveMessages() throws Exception {
		final byte[] message = "Hallo ".concat(String.valueOf(System.currentTimeMillis())).getBytes();

		// send message
		queue.sendMessage(message);

		// set message receive timeout to 5 seconds
		final HashMap<String, Object> requestProperties = new HashMap<String, Object>(2);
		requestProperties.put(IQueueServiceProperties.MESSAGE_RECEIVE_TIMEOUT, TimeUnit.SECONDS.toMillis(5));

		// receive message
		final List<IMessage> messages = queue.receiveMessages(1, requestProperties);
		assertNotNull("must return collection", messages);
		assertEquals("message must be in queue", 1, messages.size());
		assertTrue("message content must match", Arrays.equals(message, messages.get(0).getBody()));

		// check that queue is NOT empty
		final Collection<String> names = ZooKeeperGate.get().readChildrenNames(queuePath, null);
		assertEquals("queue should still contain the message", 1, names.size());

		// receive within timeout must lead to empty collection
		assertTrue("no message should be visible during the timeout", queue.receiveMessages(1, requestProperties).isEmpty());

		// wait for timeout
		Thread.sleep(TimeUnit.SECONDS.toMillis(6));

		// receive message again
		final List<IMessage> messages2 = queue.receiveMessages(1, requestProperties);
		assertEquals("message of second receive must be in queue", 1, messages2.size());
		assertTrue("message content of second receive must match", Arrays.equals(message, messages2.get(0).getBody()));
	}

	@Test
	public void test004DeleteMessage() throws Exception {
		final byte[] message = "Hallo ".concat(String.valueOf(System.currentTimeMillis())).getBytes();

		// send message
		queue.sendMessage(message);

		// receive message
		final List<IMessage> messages = queue.receiveMessages(1, null);
		assertNotNull("must return collection", messages);
		assertEquals("message must be in queue", 1, messages.size());
		final IMessage receivedMessage = messages.get(0);
		assertTrue("message content must match", Arrays.equals(message, receivedMessage.getBody()));

		// delete message
		queue.deleteMessage(receivedMessage);

		// check that queue is empty
		final Collection<String> names = ZooKeeperGate.get().readChildrenNames(queuePath, null);
		assertTrue("queue should be empty after delete", names.isEmpty());

		// second consume must result null
		assertNull("no message should be in queue", queue.consumeMessage(1, TimeUnit.SECONDS));
	}

}
