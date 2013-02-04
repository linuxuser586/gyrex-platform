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
package org.eclipse.gyrex.cloud.internal.zk;

import java.util.List;

import org.eclipse.core.runtime.IPath;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.BadVersionException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

/**
 * Class with static helpers when working with ZooKeeper.
 */
public class ZooKeeperHelper {

	/**
	 * Creates all parents of the given path if they do not exist.
	 * <p>
	 * Does not throw any {@link NodeExistsException} while creating parents.
	 * </p>
	 * 
	 * @param keeper
	 * @param path
	 * @throws InterruptedException
	 * @throws KeeperException
	 */
	public static void createParents(final ZooKeeper keeper, final IPath path) throws InterruptedException, KeeperException {
		// start by removing all segments and decrease removed segments for every loop.
		for (int i = path.segmentCount() - 1; i > 0; i--) {
			final IPath parentPath = path.removeLastSegments(i);
			try {
				// check for exists first in order to avoid unneccessary exceptions in ZooKeeper
				if (null == keeper.exists(parentPath.toString(), null)) {
					keeper.create(parentPath.toString(), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				}
			} catch (final NodeExistsException e) {
				// created concurrently ... continue
			}
		}
	}

	/**
	 * Deletes the path including all its children.
	 * <p>
	 * Does not throw any {@link NoNodeException} if the path has been removed
	 * concurrently. However, it may throw a {@link BadVersionException} if the
	 * versions do not match anymore.
	 * </p>
	 * 
	 * @param keeper
	 * @param path
	 * @param version
	 * @param cversion
	 * @throws InterruptedException
	 * @throws KeeperException
	 */
	public static void deleteTree(final ZooKeeper keeper, final IPath path, final int version, final int cversion) throws InterruptedException, KeeperException {
		// read stats
		final Stat stat = new Stat();

		// read children
		List<String> children = keeper.getChildren(path.toString(), false, stat);

		// abort if versions don't match
		if (((version > -1) && (stat.getVersion() != version)) || ((cversion > -1) && (cversion != stat.getCversion())))
			throw new BadVersionException(path.toString());

		// delete all children
		while (!children.isEmpty()) {
			try {
				for (final String child : children) {
					deleteTree(keeper, path.append(child), -1, -1);
				}
			} catch (final NoNodeException e) {
				// ignore and try again
				// (we must not allow NoNodeException from children to propagate)
			}
			children = keeper.getChildren(path.toString(), false);
		}

		// delete node itself
		try {
			keeper.delete(path.toString(), version);
		} catch (final NoNodeException e) {
			// consider deletion successful
		}
	}

	private ZooKeeperHelper() {
		// empty
	}

}
