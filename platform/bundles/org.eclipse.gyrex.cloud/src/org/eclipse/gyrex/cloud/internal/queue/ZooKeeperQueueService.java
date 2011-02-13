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

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.services.queue.DuplicateQueueException;
import org.eclipse.gyrex.cloud.services.queue.IQueue;
import org.eclipse.gyrex.cloud.services.queue.IQueueService;
import org.eclipse.gyrex.common.identifiers.IdHelper;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;

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
			ZooKeeperGate.get().createPath(IZooKeeperLayout.PATH_QUEUES_ROOT.append(id), CreateMode.PERSISTENT, id);
		} catch (final KeeperException e) {
			if (e.code() == Code.NODEEXISTS) {
				throw new DuplicateQueueException(String.format("queue '%s' already exists", id));
			}
			throw new IllegalStateException(String.format("Error creating queue '%s'. %s", id, e.getMessage()), e);
		} catch (final InterruptedException e) {
			throw new IllegalStateException(String.format("Error creating queue '%s'. %s", id, e.getMessage()), e);
		} catch (final IOException e) {
			throw new IllegalStateException(String.format("Error creating queue '%s'. %s", id, e.getMessage()), e);
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
		} catch (final KeeperException e) {
			if (e.code() == Code.NONODE) {
				throw new NoSuchElementException(String.format("queue '%s' does not exist", id));
			}
			throw new IllegalStateException(String.format("Error deleting queue '%s'. %s", id, e.getMessage()), e);
		} catch (final InterruptedException e) {
			throw new IllegalStateException(String.format("Error deleting queue '%s'. %s", id, e.getMessage()), e);
		} catch (final IOException e) {
			throw new IllegalStateException(String.format("Error deleting queue '%s'. %s", id, e.getMessage()), e);
		}
	}

	@Override
	public IQueue getQueue(final String id, final Map<String, ?> properties) throws IllegalArgumentException, IllegalStateException, SecurityException {
		if (!IdHelper.isValidId(id)) {
			throw new IllegalArgumentException("invalid id");
		}
		try {
			if (ZooKeeperGate.get().exists(IZooKeeperLayout.PATH_QUEUES_ROOT.append(id))) {
				return new ZooKeeperQueue(id);
			}
			return null;
		} catch (final KeeperException e) {
			throw new IllegalStateException(String.format("Error deleting queue '%s'. %s", id, e.getMessage()), e);
		} catch (final InterruptedException e) {
			throw new IllegalStateException(String.format("Error deleting queue '%s'. %s", id, e.getMessage()), e);
		}
	}

	@Override
	public IQueue updateQueue(final String id, final Map<String, ?> properties) throws IllegalArgumentException, IllegalStateException, SecurityException, NoSuchElementException {
		// no-op
		final IQueue queue = getQueue(id, properties);
		if (queue == null) {
			throw new NoSuchElementException(String.format("queue '%s' does not exist", id));
		}
		return queue;
	}

}
