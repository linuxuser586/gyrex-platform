/**
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.p2.internal.commands;

import org.eclipse.gyrex.common.console.BaseCommandProvider;
import org.eclipse.gyrex.p2.internal.P2Debug;

import org.eclipse.osgi.framework.console.CommandInterpreter;

/**
 * Commands for software installations
 */
public class SoftwareConsoleCommands extends BaseCommandProvider {

	public SoftwareConsoleCommands() {
		registerCommand("ls", ListCommand.class);

		registerCommand("addRepo", AddRepoCmd.class);
		registerCommand("rmRepo", RemoveRepoCmd.class);

		registerCommand("addPkg", AddPkgCmd.class);
		registerCommand("rmPkg", RemovePkgCmd.class);

		registerCommand("addToPkg", AddArtifactToPkgCmd.class);
		registerCommand("rmFromPkg", RemoveArtifactFromPkgCmd.class);

		registerCommand("rollout", RolloutCmd.class);
		registerCommand("revoke", RevokeCmd.class);

		registerCommand("update", UpdateCmd.class);
		registerCommand("status", StatusCmd.class);
	}

	public void _sw(final CommandInterpreter ci) throws Exception {
		printStackTraces = P2Debug.debug;
		execute(ci);
	}

	@Override
	protected String getCommandName() {
		return "sw";
	}

}
