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
package org.eclipse.gyrex.cloud.internal.queue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperMonitor;
import org.eclipse.gyrex.cloud.services.queue.IMessage;
import org.eclipse.gyrex.cloud.services.queue.IQueue;

import org.eclipse.core.runtime.IPath;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ZooKeeper based queue.
 */
public class ZooKeeperQueue implements IQueue {

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperQueue.class);

	private static final String PREFIX = "msg";

	final String id;
	final IPath queuePath;

	/**
	 * Creates a new instance.
	 * 
	 * @param id
	 */
	public ZooKeeperQueue(final String id) {
		this.id = id;
		queuePath = IZooKeeperLayout.PATH_QUEUES_ROOT.append(id);
	}

	@Override
	public IMessage consumeMessage(final long timeout, final TimeUnit unit) throws IllegalArgumentException, IllegalStateException, SecurityException, InterruptedException {
		/*
		 * We want to get the node with the smallest sequence number. But
		 * other clients may remove and add nodes concurrently. Thus, we
		 * need to further check if the node can be returned. It might be
		 * gone by the time we check. If that happens we just continue with
		 * the next node.
		 */
		if ((timeout > 0) && (unit == null)) {
			throw new IllegalArgumentException("unit must not be null when timeout is specified");
		}
		final long abortTime = timeout > 0 ? unit.toMillis(timeout) + System.currentTimeMillis() : 0;
		TreeMap<Long, String> queueChildren;
		while (true) {
			try {
				queueChildren = readQueueChildren(null);
			} catch (final Exception e) {
				if (e instanceof KeeperException.NoNodeException) {
					throw new IllegalStateException(String.format("queue '%s' does not exist", id));
				}
				throw new QueueOperationFailedException(id, "CONSUME_MESSAGES", e);
			}

			// iterate over all children
			if (queueChildren.size() > 0) {
				for (final String childName : queueChildren.values()) {
					if (childName != null) {
						// read message
						final Message message = readQueueMessage(childName);
						// check if we have a valid message
						if ((message == null) || message.isHidden()) {
							continue;
						}
						// try to consume the message
						if (!message.consume(false)) {
							continue;
						}
						return message;
					}
				}
			}

			// at this point no children are available
			if (abortTime <= 0) {
				// abort
				return null;
			}

			// wait for the timeout
			final long diff = abortTime - System.currentTimeMillis();
			if (diff > 0) {
				Thread.sleep(Math.max(diff / 2, 250));
			} else {
				// wait time elapsed
				return null;
			}
		}
	}

	@Override
	public boolean deleteMessage(final IMessage message) throws IllegalArgumentException, IllegalStateException, SecurityException, NoSuchElementException {
		// the expectation is that we received the message
		if (!(message instanceof Message) || !StringUtils.equals(id, message.getQueueId())) {
			throw new IllegalArgumentException(String.format("Message '%s' was not received from this queue.", String.valueOf(message)));
		}

		// try to remove the message
		return ((Message) message).delete(true);
	}

	/**
	 * Returns a sorted map of the queue node children.
	 * 
	 * @param monitor
	 *            optional watcher
	 * @return map with key sequence number and value child name
	 * @throws KeeperException
	 * @throws IllegalStateException
	 */
	private TreeMap<Long, String> readQueueChildren(final ZooKeeperMonitor monitor) throws InterruptedException, IllegalStateException, KeeperException {
		final TreeMap<Long, String> childrenBySequenceNumber = new TreeMap<Long, String>();

		final Collection<String> childNames = ZooKeeperGate.get().readChildrenNames(queuePath, monitor, null);

		for (final String childName : childNames) {
			if (!StringUtils.startsWith(childName, PREFIX)) {
				LOG.warn("Incorrect child name {} in queue {}.", new Object[] { childName, id });
				continue;
			}
			final long sequenceNumber = NumberUtils.toLong(StringUtils.substring(childName, PREFIX.length()));
			if (sequenceNumber <= 0) {
				LOG.warn("Incorrect sequence number in child name {} in queue {}.", new Object[] { childName, id });
				continue;
			}
			childrenBySequenceNumber.put(sequenceNumber, childName);
		}

		return childrenBySequenceNumber;
	}

	private Message readQueueMessage(final String messageId) {
		try {
			final Stat stat = new Stat();
			final byte[] record = ZooKeeperGate.get().readRecord(queuePath.append(messageId), stat);
			if (record == null) {
				return null;
			}
			return new Message(messageId, this, record, stat);
		} catch (final Exception e) {
			throw new QueueOperationFailedException(id, String.format("MESSAGE_READ(%s)", messageId), e);
		}
	}

	@Override
	public List<IMessage> receiveMessages(final int maxNumberOfMessages, final Map<String, ?> hints) throws IllegalArgumentException, IllegalStateException, SecurityException {
		/*
		 * We want to get the node with the smallest sequence number. But
		 * other clients may remove and add nodes concurrently. Thus, we
		 * need to further check if the node can be returned. It might be
		 * gone by the time we check. If that happens we just continue with
		 * the next node.
		 */
		if (maxNumberOfMessages < 0) {
			throw new IllegalArgumentException("maxNumberOfMessages must be greate than zero");
		}
		final List<IMessage> messages = new ArrayList<IMessage>(maxNumberOfMessages);

		try {
			// iterate over all children
			final TreeMap<Long, String> queueChildren = readQueueChildren(null);
			for (final String childName : queueChildren.values()) {
				if (childName != null) {
					// read message
					final Message message = readQueueMessage(childName);
					// check if we have a valid message
					if ((message == null) || message.isHidden()) {
						continue;
					}
					// try to receive the message
					if (!message.receive(30000, false)) {
						continue;
					}
					messages.add(message);
				}
			}
		} catch (final Exception e) {
			throw new QueueOperationFailedException(id, "RECEIVE_MESSAGES", e);
		}

		return messages;
	}

	@Override
	public void sendMessage(final byte[] messageBody) throws IllegalArgumentException, IllegalStateException, SecurityException {
		try {
			ZooKeeperGate.get().createPath(queuePath.append(PREFIX), CreateMode.PERSISTENT_SEQUENTIAL, new Message(id, messageBody).toByteArray());
		} catch (final Exception e) {
			if (e instanceof KeeperException.NoNodeException) {
				throw new IllegalStateException(String.format("queue '%s' does not exist", id));
			}
			throw new QueueOperationFailedException(id, "SEND_MESSAGES", e);
		}
	}

}
