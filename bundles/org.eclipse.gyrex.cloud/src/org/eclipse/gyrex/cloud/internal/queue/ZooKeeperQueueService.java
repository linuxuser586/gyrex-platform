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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.services.queue.DuplicateQueueException;
import org.eclipse.gyrex.cloud.services.queue.IQueue;
import org.eclipse.gyrex.cloud.services.queue.IQueueService;
import org.eclipse.gyrex.cloud.services.queue.IQueueServiceProperties;
import org.eclipse.gyrex.common.identifiers.IdHelper;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;

/**
 * Queue service based on ZooKeeper.
 */
public class ZooKeeperQueueService implements IQueueService {

	@Override
	public IQueue createQueue(final String id, final Map<String, ?> properties) throws IllegalArgumentException, IllegalStateException, SecurityException, DuplicateQueueException {
		if (!IdHelper.isValidId(id)) {
			throw new IllegalArgumentException("invalid id");
		}
		try {
			ZooKeeperGate.get().createPath(IZooKeeperLayout.PATH_QUEUES_ROOT.append(id), CreateMode.PERSISTENT, getQueueData(properties));
		} catch (final Exception e) {
			if (e instanceof KeeperException.NodeExistsException) {
				throw new DuplicateQueueException(String.format("queue '%s' already exists", id));
			}
			throw new QueueOperationFailedException(id, "CREATE_QUEUE", e);
		}
		return new ZooKeeperQueue(id);
	}

	@Override
	public void deleteQueue(final String id, final Map<String, ?> properties) throws IllegalArgumentException, IllegalStateException, SecurityException, NoSuchElementException {
		if (!IdHelper.isValidId(id)) {
			throw new IllegalArgumentException("invalid id");
		}
		try {
			ZooKeeperGate.get().deletePath(IZooKeeperLayout.PATH_QUEUES_ROOT.append(id));
		} catch (final Exception e) {
			if (e instanceof KeeperException.NoNodeException) {
				throw new NoSuchElementException(String.format("queue '%s' does not exist", id));
			}
			throw new QueueOperationFailedException(id, "DELETE_QUEUE", e);
		}
	}

	@Override
	public ZooKeeperQueue getQueue(final String id, final Map<String, ?> properties) throws IllegalArgumentException, IllegalStateException, SecurityException {
		if (!IdHelper.isValidId(id)) {
			throw new IllegalArgumentException("invalid id");
		}
		try {
			if (ZooKeeperGate.get().exists(IZooKeeperLayout.PATH_QUEUES_ROOT.append(id))) {
				return new ZooKeeperQueue(id);
			}
			return null;
		} catch (final Exception e) {
			throw new QueueOperationFailedException(id, "GET_QUEUE", e);
		}
	}

	private byte[] getQueueData(final Map<String, ?> properties) throws IOException {
		final ByteArrayOutputStream queueData = new ByteArrayOutputStream();
		if (properties != null) {
			final Properties p = new Properties();
			final Object timeout = properties.get(IQueueServiceProperties.MESSAGE_RECEIVE_TIMEOUT);
			if (timeout != null) {
				p.setProperty(IQueueServiceProperties.MESSAGE_RECEIVE_TIMEOUT, String.valueOf(timeout));
			}
			if (!p.isEmpty()) {
				p.store(queueData, null);
			}
		}
		return queueData.toByteArray();
	}

	public Collection<String> getQueues() {
		try {
			return ZooKeeperGate.get().readChildrenNames(IZooKeeperLayout.PATH_QUEUES_ROOT, null);
		} catch (final NoNodeException e) {
			return Collections.emptyList();
		} catch (final Exception e) {
			throw new IllegalStateException("Error reading queues. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	@Override
	public IQueue updateQueue(final String id, final Map<String, ?> properties) throws IllegalArgumentException, IllegalStateException, SecurityException, NoSuchElementException {
		try {
			ZooKeeperGate.get().writeRecord(IZooKeeperLayout.PATH_QUEUES_ROOT.append(id), getQueueData(properties), -1);
			return new ZooKeeperQueue(id);
		} catch (final Exception e) {
			if (e instanceof KeeperException.NoNodeException) {
				throw new NoSuchElementException(String.format("queue '%s' does not exist", id));
			}
			throw new QueueOperationFailedException(id, "UPDATE_QUEUE", e);
		}
	}

}
