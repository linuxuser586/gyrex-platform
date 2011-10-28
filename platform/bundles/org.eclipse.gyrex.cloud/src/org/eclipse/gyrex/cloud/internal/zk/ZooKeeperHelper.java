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
package org.eclipse.gyrex.cloud.internal.zk;

import java.util.List;

import org.eclipse.core.runtime.IPath;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.BadVersionException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

/**
 * Class with static helpers when working with ZooKeeper.
 */
public class ZooKeeperHelper {

	public static void createParents(final ZooKeeper keeper, final IPath path) throws InterruptedException, KeeperException {
		for (int i = path.segmentCount() - 1; i > 0; i--) {
			final IPath parentPath = path.removeLastSegments(i);
			try {
				keeper.create(parentPath.toString(), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			} catch (final KeeperException e) {
				if (e.code() != KeeperException.Code.NODEEXISTS) {
					// rethrow
					throw e;
				}
			}
		}
	}

	public static void deleteTree(final ZooKeeper keeper, final IPath path, final int version, final int cversion) throws InterruptedException, KeeperException {
		// read stats
		final Stat stat = new Stat();

		// read children
		List<String> children = keeper.getChildren(path.toString(), false, stat);

		// abort if versions don't match
		if (((version > -1) && (stat.getVersion() != version)) || ((cversion > -1) && (cversion != stat.getCversion()))) {
			throw new BadVersionException(path.toString());
		}

		// delete all children
		while (!children.isEmpty()) {
			try {
				for (final String child : children) {
					deleteTree(keeper, path.append(child), -1, -1);
				}
			} catch (final NoNodeException e) {
				// ignore and try again
				// (we must not allow NoNodeException from children propagate)
			}
			children = keeper.getChildren(path.toString(), false);
		}

		// delete node itself
		keeper.delete(path.toString(), version);
	}

	private ZooKeeperHelper() {
		// empty
	}

}
