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
package org.eclipse.gyrex.cloud.internal.zk.console;

import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperConsoleCommand;

import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.ZooKeeper;
import org.kohsuke.args4j.Argument;

public abstract class PathBasedCmd extends ZooKeeperConsoleCommand {

	@Argument(index = 0, metaVar = "PATH", usage = "specify a path", required = true)
	String path;

	/**
	 * Creates a new instance.
	 */
	public PathBasedCmd(final String description) {
		super("<PATH> " + description);
	}

	@Override
	protected final void doExecute(final ZooKeeper zk) throws Exception {
		if (!StringUtils.equals(path, "/")) {
			path = StringUtils.removeEnd(path, "/");
		}
		doExecute(zk, path);
	}

	protected abstract void doExecute(final ZooKeeper zk, String path) throws Exception;
}
