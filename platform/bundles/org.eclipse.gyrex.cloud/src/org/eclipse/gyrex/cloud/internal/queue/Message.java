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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.NoSuchElementException;

import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.services.queue.IMessage;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

/**
 * ZooKeeper queue message
 */
public class Message implements IMessage {

	private final String queueId;
	private final byte[] body;
	private int zkNodeDataVersion;
	private long invisibleTimeoutTS;
	private final String messageId;
	private final ZooKeeperQueue zooKeeperQueue;

	/**
	 * Creates a new instance.
	 * 
	 * @param messageBody
	 */
	public Message(final String queueId, final byte[] body) {
		messageId = null;
		zooKeeperQueue = null;
		this.queueId = queueId;
		this.body = body;
		zkNodeDataVersion = -1;
		invisibleTimeoutTS = 0;
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param zooKeeperQueue
	 * @param messageId
	 * @param record
	 * @param stat
	 * @throws IOException
	 */
	public Message(final String messageId, final ZooKeeperQueue zooKeeperQueue, final byte[] record, final Stat stat) throws IOException {
		this.messageId = messageId;
		this.zooKeeperQueue = zooKeeperQueue;
		queueId = zooKeeperQueue.id;
		zkNodeDataVersion = stat.getVersion();

		final DataInputStream din = new DataInputStream(new ByteArrayInputStream(record));

		// serialized format version
		final int formatVersion = din.readInt();
		if (formatVersion != 1) {
			throw new IllegalArgumentException(String.format("invalid record data: version mismatch (expected %d, found %d)", 1, formatVersion));
		}

		// timeout
		invisibleTimeoutTS = din.readLong();

		// body size
		final int length = din.readInt();

		// body
		body = new byte[length];
		final int read = din.read(body);
		if (read != length) {
			throw new IllegalArgumentException(String.format("invalid record data: body size mismatch (expected %d, read %d)", length, read));
		}
	}

	/**
	 * Consumes a message (which will delete it from the queue).
	 * <p>
	 * This is effectively a {@link #receive(long, boolean)} followed by a
	 * {@link #delete(boolean)}.
	 * </p>
	 * 
	 * @param failIfDeleted
	 *            set to <code>true</code> when a {@link NoSuchElementException}
	 *            should be thrown if the message has already been deleted on
	 *            the server, otherwise no exception will be thrown
	 * @return <code>true</code> if successful, <code>false</code> otherwise
	 */
	public boolean consume(final boolean failIfDeleted) throws NoSuchElementException {
		// receive the message with a short timeout
		if (!receive(1000, failIfDeleted)) {
			return false;
		}
		// delete
		return delete(failIfDeleted);
	}

	/**
	 * Removes a message from the queue.
	 * <p>
	 * This will delete a message in the queue but only if it hasn't changed
	 * from the current view.
	 * </p>
	 * 
	 * @param failIfDeleted
	 *            <code>true</code> if a {@link NoSuchElementException} should
	 *            be thrown if the message doesn't exist anymore,
	 *            <code>false</code> otherwise
	 * @return <code>true</code> if the message has been deleted successfully,
	 *         <code>false</code> otherwise
	 * @throws NoSuchElementException
	 *             if the message could not be found and
	 *             <code>failIfDeleted</code> was <code>true</code>
	 */
	public boolean delete(final boolean failIfDeleted) throws NoSuchElementException {
		try {
			// note, we don't check the timeout here
			// we delete the message in any case if the version hasn't change in ZooKeeper
			ZooKeeperGate.get().deletePath(zooKeeperQueue.queuePath.append(messageId), zkNodeDataVersion);

			// the call succeeded
			return true;
		} catch (final Exception e) {
			// don't reset timeout here (delete does not influence it)
			//invisibleTimeoutTS = 0;

			// special handling if node does not exists
			if (e instanceof KeeperException.NoNodeException) {
				if (failIfDeleted) {
					throw new NoSuchElementException("Message does not exists!");
				}
				// the node does not exist and we must not fail
				// therefore, we return success here
				return true;
			} else if (e instanceof KeeperException.BadVersionException) {
				// the node has been updated remotely
				return false;
			}

			// fail operation
			throw new QueueOperationFailedException(queueId, String.format("DELETE_MESSAGE(%s)", messageId), e);
		}
	}

	/**
	 * Returns the body.
	 * 
	 * @return the body
	 */
	public byte[] getBody() {
		return body;
	}

	@Override
	public String getQueueId() {
		return queueId;
	}

	/**
	 * Indicated if a message is hidden, i.e. received by a consumer but not
	 * deleted.
	 * 
	 * @return
	 */
	public boolean isHidden() {
		return invisibleTimeoutTS >= System.currentTimeMillis();
	}

	/**
	 * Receives a message from the queue.
	 * <p>
	 * This will hide a message in the queue for the specified timeout. As long
	 * as the timeout is not elapsed {@link #isHidden()} will return
	 * <code>true</code>.
	 * </p>
	 * 
	 * @param failIfDeleted
	 * @return
	 * @throws NoSuchElementException
	 */
	public boolean receive(final long timeoutInMs, final boolean failIfDeleted) throws NoSuchElementException {
		invisibleTimeoutTS = timeoutInMs + System.currentTimeMillis();
		try {
			// update record
			final Stat stat = ZooKeeperGate.get().writeRecord(zooKeeperQueue.queuePath.append(messageId), toByteArray(), zkNodeDataVersion);

			// remember new version
			zkNodeDataVersion = stat.getVersion();

			// report success
			return true;
		} catch (final Exception e) {
			// reset timeout
			invisibleTimeoutTS = 0;

			// special handling if node does not exists
			if (e instanceof KeeperException.NoNodeException) {
				if (failIfDeleted) {
					throw new NoSuchElementException("Message does not exists!");
				}
				// not received
				return false;
			} else if (e instanceof KeeperException.BadVersionException) {
				// the node has been updated remotely
				return false;
			}

			// fail operation
			throw new QueueOperationFailedException(queueId, String.format("RECEIVE_MESSAGE(%s)", messageId), e);
		}
	}

	public byte[] toByteArray() throws IOException {
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final DataOutputStream dos = new DataOutputStream(bos);
		dos.writeInt(1); // serialized format version
		dos.writeLong(invisibleTimeoutTS); // invisible timeout
		dos.writeInt(body.length); // body size
		dos.write(body); // body
		return bos.toByteArray();
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Message [messageId=").append(messageId).append(", queueId=").append(queueId).append(", invisibleTill=").append(invisibleTimeoutTS > 0 ? DateFormatUtils.ISO_DATE_TIME_ZONE_FORMAT.format(invisibleTimeoutTS) : "0").append(", version=").append(zkNodeDataVersion).append("]");
		return builder.toString();
	}
}
