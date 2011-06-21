/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal.zk.console;

import org.apache.zookeeper.ZooKeeper;
import org.kohsuke.args4j.Option;

public class GetCmd extends PathBasedCmd {

	@Option(name = "--charset-name", usage = "name of the character set to use when reading the content as string")
	String charsetName;

	/**
	 * Creates a new instance.
	 */
	public GetCmd() {
		super("- prints the content of the node at the specified path as a string");
	}

	@Override
	protected void doExecute(final ZooKeeper zk, final String path) throws Exception {
		byte data[] = zk.getData(path, false, null);
		data = (data == null) ? "null".getBytes() : data;
		if (null != charsetName) {
			ci.println(new String(data, charsetName));
		} else {
			ci.println(new String(data));
		}
	}
}
