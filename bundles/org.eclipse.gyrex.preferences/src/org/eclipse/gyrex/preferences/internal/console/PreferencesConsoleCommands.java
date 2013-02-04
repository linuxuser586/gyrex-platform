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
package org.eclipse.gyrex.preferences.internal.console;

import org.eclipse.gyrex.common.console.BaseCommandProvider;
import org.eclipse.gyrex.preferences.internal.PreferencesDebug;

import org.eclipse.osgi.framework.console.CommandInterpreter;

/**
 * Commands for preferences
 */
public class PreferencesConsoleCommands extends BaseCommandProvider {

	/**
	 * Creates a new instance.
	 */
	public PreferencesConsoleCommands() {
		registerCommand("ls", LsCmd.class);
		registerCommand("export", ExportCmd.class);
		registerCommand("import", ImportCmd.class);
		registerCommand("set", SetCmd.class);
		registerCommand("unset", UnsetCmd.class);
		registerCommand("rm", RemoveCmd.class);
		registerCommand("sync", SyncCmd.class);
		registerCommand("flush", FlushCmd.class);
		registerCommand("dump", DumpCmd.class);
	}

	public void _prefs(final CommandInterpreter ci) throws Exception {
		printStackTraces = PreferencesDebug.debug;
		execute(ci);
	}

	@Override
	protected String getCommandName() {
		return "prefs";
	}

}
