/**
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *     Mike Tschierschke - improvements due working on https://bugs.eclipse.org/bugs/show_bug.cgi?id=344467
 */
package org.eclipse.gyrex.jobs.internal.commands;

import org.eclipse.gyrex.common.console.BaseCommandProvider;

import org.eclipse.osgi.framework.console.CommandInterpreter;

/**
 * Commands for software installations
 */
public class JobsConsoleCommands extends BaseCommandProvider {

	/**
	 * Creates a new instance.
	 */
	public JobsConsoleCommands() {
		registerCommand("ls", LsCmd.class);

		registerCommand("createSchedule", CreateScheduleCmd.class);
		registerCommand("removeSchedule", RemoveScheduleCmd.class);

		registerCommand("enableSchedule", EnableScheduleCmd.class);
		registerCommand("disableSchedule", DisableScheduleCmd.class);

		registerCommand("addEntryToSchedule", AddEntryToScheduleCmd.class);
		registerCommand("setEntryParam", UpdateEntryToScheduleCmd.class);

		registerCommand("cleanup", CleanupCmd.class);

		registerCommand("suspend", SuspendWorkerCmd.class);
		registerCommand("resume", ResumeWorkerCmd.class);

		registerCommand("cancel", CancelJobCmd.class);
	}

	public void _jobs(final CommandInterpreter ci) throws Exception {
		execute(ci);
	}

	@Override
	protected String getCommandName() {
		return "jobs";
	}
}
