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

import org.eclipse.osgi.framework.console.CommandInterpreter;

import org.apache.zookeeper.AsyncCallback.VoidCallback;
import org.apache.zookeeper.ZooKeeper;

/**
 *
 */
public class SyncCmd extends PathBasedCmd {

	/**
	 * Creates a new instance.
	 */
	public SyncCmd() {
		super("- performs ZK sync on the specified path");
	}

	@Override
	protected void doExecute(final ZooKeeper zk, final String path) throws Exception {
		zk.sync(path, new VoidCallback() {
			@Override
			public void processResult(final int rc, final String path, final Object ctx) {
				((CommandInterpreter) ctx).println("Synched " + path + " (rc:" + rc + ")");
			}
		}, ci);
	}
}
