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
package org.eclipse.gyrex.boot.internal.console;

import org.eclipse.gyrex.boot.internal.BootActivator;
import org.eclipse.gyrex.boot.internal.BootDebug;
import org.eclipse.gyrex.common.console.BaseCommandProvider;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.service.debug.DebugOptions;

/**
 * Console command to control debug/trace
 */
public class DebugConsoleCommands extends BaseCommandProvider {

	static DebugOptions getDebugOptions() {
		return BootActivator.getInstance().getService(DebugOptions.class);
	}

	/**
	 * Creates a new instance.
	 */
	public DebugConsoleCommands() {
		registerCommand("off", OffCmd.class);
		registerCommand("on", OnCmd.class);
		registerCommand("ls", ListCmd.class);
		registerCommand("set", SetCmd.class);
		registerCommand("find", FindCmd.class);
	}

	public void _debug(final CommandInterpreter ci) {
		printStackTraces = BootDebug.debug;
		execute(ci);
	}

	@Override
	protected String getCommandName() {
		return "debug";
	}
}
