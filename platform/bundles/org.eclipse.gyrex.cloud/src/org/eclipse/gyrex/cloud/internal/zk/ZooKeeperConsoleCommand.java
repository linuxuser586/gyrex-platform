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
package org.eclipse.gyrex.cloud.internal.zk;

import org.eclipse.gyrex.common.console.Command;

import org.apache.zookeeper.ZooKeeper;

/**
 * Base class for console commands which require direct access to ZooKeeper
 */
public abstract class ZooKeeperConsoleCommand extends Command {

	/**
	 * Creates a new instance.
	 * 
	 * @param description
	 */
	public ZooKeeperConsoleCommand(final String description) {
		super(description);
	}

	@Override
	protected final void doExecute() throws Exception {
		ZooKeeper keeper = null;
		try {
			keeper = ZooKeeperGate.get().ensureConnected();
		} catch (final Exception e) {
			printf("ERROR: ZooKeeper not connected! %s", e.getMessage());
			return;
		}

		doExecute(keeper);
	}

	protected abstract void doExecute(ZooKeeper zk) throws Exception;
}
