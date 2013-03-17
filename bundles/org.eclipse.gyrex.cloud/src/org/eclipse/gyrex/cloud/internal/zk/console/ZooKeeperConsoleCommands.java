/*******************************************************************************
 * Copyright (c) 2010, 2013 AGETO Service GmbH and others.
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

import org.eclipse.gyrex.cloud.internal.CloudDebug;
import org.eclipse.gyrex.cloud.internal.zk.GateStatusCmd;
import org.eclipse.gyrex.common.console.BaseCommandProvider;

import org.eclipse.osgi.framework.console.CommandInterpreter;

public class ZooKeeperConsoleCommands extends BaseCommandProvider {

	/**
	 * Creates a new instance.
	 */
	public ZooKeeperConsoleCommands() {
		registerCommand("ls", ListCmd.class);
		registerCommand("create", CreateCmd.class);
		registerCommand("rm", RmPathCmd.class);
		registerCommand("get", GetCmd.class);
		registerCommand("set", SetCmd.class);
		registerCommand("stat", StatCmd.class);
		registerCommand("sync", SyncCmd.class);
		registerCommand("reconnect", ReconnectGateCmd.class);
		registerCommand("status", GateStatusCmd.class);
	}

	public void _zk(final CommandInterpreter ci) throws Exception {
		printStackTraces = CloudDebug.debug;
		execute(ci);
	}

	@Override
	protected String getCommandName() {
		return "zk";
	}

}
