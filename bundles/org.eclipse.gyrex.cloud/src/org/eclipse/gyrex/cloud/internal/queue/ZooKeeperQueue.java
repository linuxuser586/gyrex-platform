/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperMonitor;
import org.eclipse.gyrex.cloud.services.queue.IMessage;
import org.eclipse.gyrex.cloud.services.queue.IQueue;
import org.eclipse.gyrex.cloud.services.queue.IQueueServiceProperties;

import org.eclipse.core.runtime.IPath;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ZooKeeper based queue.
 */
// TODO: investigate extending ZooKeeperBasedService for re-try operations
public class ZooKeeperQueue implements IQueue {

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperQueue.class);
	private static final String PREFIX = "msg-";

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

	@Override
	public String getId() {
		return id;
	}

	/**
	 * Returns the ordered list of messages.
	 * <p>
	 * Note, this represents a snapshot of the queue at the time of invoking the
	 * method.
	 * </p>
	 * 
	 * @return ordered list of messages
	 */
	public List<Message> getMessages() {
		try {
			final TreeMap<Long, String> queueChildren = readQueueChildren(null);
			final List<Message> messages = new ArrayList<Message>(queueChildren.size());
			for (final String messageId : queueChildren.values()) {
				final Message message = readQueueMessage(messageId);
				if (null != message) {
					messages.add(message);
				}
			}
			return messages;
		} catch (final NoNodeException e) {
			// don't fail just return null
			return Collections.emptyList();
		} catch (final Exception e) {
			throw new QueueOperationFailedException(id, "READ_MESSAGES", e);
		}
	}

	/**
	 * Returns the receive message timeout either from the specified properties
	 * or the queue default
	 * 
	 * @param properties
	 * @return
	 */
	private long getReceiveMessageTimeout(final Map<String, ?> properties) {
		// check properties
		if (properties != null) {
			final Object timeout = properties.get(IQueueServiceProperties.MESSAGE_RECEIVE_TIMEOUT);
			if (timeout != null) {
				if (!Long.class.isAssignableFrom(timeout.getClass())) {
					throw new IllegalArgumentException(String.format("Property %s must be of type Long or long.", IQueueServiceProperties.MESSAGE_RECEIVE_TIMEOUT));
				}
				return (Long) timeout;
			}
		}

		// check queue value
		final Properties queueData = readQueueData();
		final String queueTimeout = queueData.getProperty(IQueueServiceProperties.MESSAGE_RECEIVE_TIMEOUT, null);

		return NumberUtils.toLong(queueTimeout, 30000);
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
			final long sequenceNumber = NumberUtils.toLong(StringUtils.substring(childName, PREFIX.length()), -1);
			if (sequenceNumber < 0) {
				LOG.warn("Incorrect sequence number in child name {} in queue {}.", new Object[] { childName, id });
				continue;
			}
			childrenBySequenceNumber.put(sequenceNumber, childName);
		}

		return childrenBySequenceNumber;
	}

	private Properties readQueueData() {
		final Properties queueData = new Properties();
		try {
			final Stat stat = new Stat();
			final byte[] record = ZooKeeperGate.get().readRecord(queuePath, stat);
			if (record == null) {
				return queueData;
			}
			queueData.load(new ByteArrayInputStream(record));
			return queueData;
		} catch (final Exception e) {
			if (e instanceof KeeperException.NoNodeException) {
				throw new IllegalStateException(String.format("Queue '%s' has been removed!", id));
			}
			throw new QueueOperationFailedException(id, "READ_QUEUE_DATA", e);
		}
	}

	private Message readQueueMessage(final String messageId) {
		try {
			final Stat stat = new Stat();
			final byte[] record = ZooKeeperGate.get().readRecord(queuePath.append(messageId), stat);
			if (record == null) {
				return null;
			}
			return new Message(messageId, this, record, stat);
		} catch (final NoNodeException e) {
			// don't fail just return null
			return null;
		} catch (final Exception e) {
			throw new QueueOperationFailedException(id, String.format("MESSAGE_READ(%s)", messageId), e);
		}
	}

	@Override
	public List<IMessage> receiveMessages(final int maxNumberOfMessages, final Map<String, ?> properties) throws IllegalArgumentException, IllegalStateException, SecurityException {
		/*
		 * We want to get the node with the smallest sequence number. But
		 * other clients may remove and add nodes concurrently. Thus, we
		 * need to further check if the node can be returned. It might be
		 * gone by the time we check. If that happens we just continue with
		 * the next node.
		 */
		if (maxNumberOfMessages <= 0) {
			throw new IllegalArgumentException("maxNumberOfMessages must be greate than zero");
		}
		final List<IMessage> messages = new ArrayList<IMessage>(maxNumberOfMessages);

		try {
			// get timeout
			final long receiveMessageTimeout = getReceiveMessageTimeout(properties);

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
					if (!message.receive(receiveMessageTimeout, false)) {
						continue;
					}

					// message received
					messages.add(message);

					// stop if enough
					if (messages.size() >= maxNumberOfMessages) {
						return messages;
					}
				}
			}
		} catch (final Exception e) {
			if (e instanceof KeeperException.NoNodeException) {
				throw new IllegalStateException(String.format("queue '%s' does not exist", id));
			}
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

	public int size() {
		try {
			return ZooKeeperGate.get().readChildrenNames(queuePath, null).size();
		} catch (final NoNodeException e) {
			// don't fail just return null
			return 0;
		} catch (final Exception e) {
			throw new QueueOperationFailedException(id, "READ_SIZE", e);
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ZooKeeperQueue [").append(id).append(" @ ").append(queuePath).append("]");
		return builder.toString();
	}

}
