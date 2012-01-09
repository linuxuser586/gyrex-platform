/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
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

import org.eclipse.gyrex.common.console.Command;

import org.eclipse.osgi.service.debug.DebugOptions;

import org.kohsuke.args4j.Argument;

public class SetCmd extends Command {

	@Argument(index = 0, usage = "specify an option name", metaVar = "OPTION", required = true)
	String name;
	@Argument(index = 1, usage = "specify an option value to set (or none to unset)", metaVar = "VALUE", required = false)
	String value;

	/**
	 * Creates a new instance.
	 */
	public SetCmd() {
		super("<NAME> <VALUE> - sets a debug option");
	}

	@Override
	protected void doExecute() throws Exception {
		final DebugOptions debugOptions = DebugConsoleCommands.getDebugOptions();
		if (null == value) {
			debugOptions.removeOption(name);
		} else {
			debugOptions.setOption(name, value);
		}
	}
}