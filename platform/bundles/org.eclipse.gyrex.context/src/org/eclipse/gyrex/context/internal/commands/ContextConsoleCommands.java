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
 */
package org.eclipse.gyrex.context.internal.commands;

import org.eclipse.gyrex.common.console.BaseCommandProvider;
import org.eclipse.gyrex.context.internal.ContextDebug;

import org.eclipse.osgi.framework.console.CommandInterpreter;

/**
 * Commands for software installations
 */
public class ContextConsoleCommands extends BaseCommandProvider {

	/**
	 * Creates a new instance.
	 */
	public ContextConsoleCommands() {
		registerCommand("ls", ListContextsCmd.class);
		registerCommand("define", DefineContextCmd.class);
		registerCommand("rm", RemoveContextCmd.class);
		registerCommand("flush", FlushContextCmd.class);
	}

	public void _context(final CommandInterpreter ci) throws Exception {
		printStackTraces = ContextDebug.debug;
		execute(ci);
	}

	@Override
	protected String getCommandName() {
		return "context";
	}

}
