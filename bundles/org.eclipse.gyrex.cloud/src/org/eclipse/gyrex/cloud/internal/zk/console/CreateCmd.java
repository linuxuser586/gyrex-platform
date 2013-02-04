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

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class CreateCmd extends PathBasedCmd {

	@Option(name = "--charset-name", usage = "name of the character set to use when writing the input string as byte array")
	String charsetName;

	@Argument(index = 1, metaVar = "DATA", usage = "optional input string to set")
	String data;

	@Option(name = "-e", aliases = "--ephemeral", usage = "if the node should be ephemeral")
	boolean ephemeral;

	@Option(name = "-s", aliases = "--sequential", usage = "if the node should be sequential")
	boolean sequential;

	/**
	 * Creates a new instance.
	 */
	public CreateCmd() {
		super("- creates a path in ZK");
	}

	@Override
	protected void doExecute(final ZooKeeper zk, final String path) throws Exception {
		byte[] bytes = null;
		if (null != data) {
			if (null != charsetName) {
				bytes = data.getBytes(charsetName);
			} else {
				bytes = data.getBytes();
			}
		}

		CreateMode flags = CreateMode.PERSISTENT;
		if (ephemeral && sequential) {
			flags = CreateMode.EPHEMERAL_SEQUENTIAL;
		} else if (ephemeral) {
			flags = CreateMode.EPHEMERAL;
		} else if (sequential) {
			flags = CreateMode.PERSISTENT_SEQUENTIAL;
		}

		final String newPath = zk.create(path, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, flags);
		ci.println("Created " + newPath);
	}
}
