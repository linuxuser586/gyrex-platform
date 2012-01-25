/**
 * Copyright (c) 2011, 2012 Gunnar Wagenknecht and others.
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
 * <p>
 * This class must be sub-classed by clients providing console commands using
 * {@link BaseCommandProvider}. Commands must be registered with a
 * {@link BaseCommandProvider}. ARGS4J will be used for populating command
 * objects with argument. As such, sub-classes are required to provide a public
 * no-arg constructor.
 * </p>
 * <p>
 * Note, this API depends on the ARGS4J API. Thus, it is bound to the evolution
 * of external API which might not follow the Gyrex <a
 * href="http://wiki.eclipse.org/Evolving_Java-based_APIs" target="_blank">API
 * evolution</a> and <a href="http://wiki.eclipse.org/Version_Numbering"
 * target="_blank">versioning</a> guidelines.
 * </p>
 * 
 * @see <a href="http://args4j.kohsuke.org/">ARGS4J</a>
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