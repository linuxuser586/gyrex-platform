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
package org.eclipse.gyrex.cloud.internal.zk.console;

import java.util.List;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

/**
 *
 */
public class RmPathCmd extends RecursivePathBasedCmd {

	/**
	 * Creates a new instance.
	 */
	public RmPathCmd() {
		super("- removes the specified path");
	}

	private void deleteTree(final ZooKeeper zk, final String path) throws InterruptedException, KeeperException {
		// delete all children
		final List<String> children = zk.getChildren(path.toString(), false);
		for (final String child : children) {
			deleteTree(zk, path + (path.equals("/") ? "" : "/") + child);
		}
		// delete node itself
		zk.delete(path, -1);
	}

	@Override
	protected void doExecute(final ZooKeeper zk, final String path) throws Exception {
		if (recursive) {
			deleteTree(zk, path);
		} else {
			zk.delete(path, -1);
		}

		printf("Removed %s", path);
	}
}
