/**
 * Copyright (c) 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.common.console;

import org.eclipse.osgi.framework.console.CommandInterpreter;

/**
 * Base class for a command.
 */
public abstract class Command {

	private final String description;
	protected CommandInterpreter ci;

	/**
	 * Creates a new command instance.
	 * 
	 * @param description
	 *            the command description
	 */
	public Command(final String description) {
		this.description = description;
	}

	protected abstract void doExecute() throws Exception;

	public void execute(final CommandInterpreter ci) throws Exception {
		this.ci = ci;
		try {
			doExecute();
		} finally {
			this.ci = null;
		}
	}

	public String getDescription() {
		return description;
	}

	protected void printf(final String message, final Object... args) {
		if (ci != null) {
			ci.println(String.format(message, args));
		}
	}

	protected void run(final String cmd) {
		printf("Executing: %s", cmd);
		ci.execute(cmd);
	}

	protected void run(final String commandUsingFormat, final Object... args) {
		run(String.format(commandUsingFormat, args));
	}

}