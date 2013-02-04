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

import org.apache.zookeeper.ZooKeeper;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class SetCmd extends PathBasedCmd {

	@Option(name = "--charset-name", usage = "name of the character set to use when writing the input string as byte array")
	String charsetName;

	@Argument(index = 1, metaVar = "DATA", usage = "input string to set", required = true)
	String data;

	/**
	 * Creates a new instance.
	 */
	public SetCmd() {
		super("- sets the content of the node at the specified path as a string");
	}

	@Override
	protected void doExecute(final ZooKeeper zk, final String path) throws Exception {
		if (null != charsetName) {
			zk.setData(path, data.getBytes(charsetName), -1);
		} else {
			zk.setData(path, data.getBytes(), -1);
		}
		ci.println("Updated " + path);
	}
}
