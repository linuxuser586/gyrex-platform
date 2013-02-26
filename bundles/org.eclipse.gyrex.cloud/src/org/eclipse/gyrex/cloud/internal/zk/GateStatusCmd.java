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

import org.eclipse.gyrex.common.console.Command;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.zookeeper.ZooKeeper;

public class GateStatusCmd extends Command {

	/**
	 * Creates a new instance.
	 */
	public GateStatusCmd() {
		super("prints the ZooKeeper gate status");
	}

	@Override
	protected void doExecute() throws Exception {
		try {
			final ZooKeeperGate gate = ZooKeeperGate.get();
			final ZooKeeper zk = gate.getZooKeeper();
			printf("Connect String: %s", gate.getConnectString());
			printf("       Timeout: %dms", gate.getSessionTimeout());
			if (gate.getLastStateChangeTimestamp() > 0L) {
				printf("         State: %s (since %s)", zk.getState(), DateFormatUtils.SMTP_DATETIME_FORMAT.format(gate.getLastStateChangeTimestamp()));
			} else {
				printf("         State: %s (initial state)", zk.getState());
			}
			printf("       Session: 0x%s", Long.toHexString(zk.getSessionId()));
			printf("    Connection: %s", gate.getConnectedServerInfo());
		} catch (final GateDownException e) {
			printf("Gate is down.");
		}
	}
}
