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

import java.util.Date;

import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

/**
 *
 */
public class StatCmd extends PathBasedCmd {

	/**
	 * Creates a new instance.
	 */
	public StatCmd() {
		super("- prints ZK stats of the specified path");
	}

	@Override
	protected void doExecute(final ZooKeeper zk, final String path) throws Exception {
		final Stat stat = zk.exists(path, false);
		ci.println("cZxid: 0x" + Long.toHexString(stat.getCzxid()));
		ci.println("ctime: " + new Date(stat.getCtime()).toString());
		ci.println("mZxid: 0x" + Long.toHexString(stat.getMzxid()));
		ci.println("mtime: " + new Date(stat.getMtime()).toString());
		ci.println("pZxid: 0x" + Long.toHexString(stat.getPzxid()));
		ci.println("cversion: " + stat.getCversion());
		ci.println("dataVersion: " + stat.getVersion());
		ci.println("aclVersion: " + stat.getAversion());
		ci.println("ephemeralOwner: 0x" + Long.toHexString(stat.getEphemeralOwner()));
		ci.println("dataLength: " + stat.getDataLength());
		ci.println("numChildren: " + stat.getNumChildren());
	}
}
