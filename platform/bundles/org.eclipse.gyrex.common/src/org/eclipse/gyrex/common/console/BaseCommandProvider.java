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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

/**
 * Base class for command providers executing {@link Command commands}.
 */
public abstract class BaseCommandProvider implements CommandProvider {

	final private Map<String, Class<? extends Command>> commands = new TreeMap<String, Class<? extends Command>>();

	/**
	 * allows to enable or disable printing of stack traces (default is
	 * <code>false</code>)
	 */
	protected boolean printStackTraces = false;

	/**
	 * Parses the command line to find a command and executes it.
	 * <p>
	 * Prints a list of registered commands including their description if an
	 * invalid or no command was specified.
	 * </p>
	 * <p>
	 * Prints the command usage info if the command fails parsing the command
	 * line.
	 * </p>
	 * 
	 * @param ci
	 *            the Equinox console command interpreter
	 */
	protected final void execute(final CommandInterpreter ci) {
		final List<String> args = new ArrayList<String>();
		final String command = ci.nextArgument();
		if (StringUtils.isBlank(command)) {
			ci.println("ERROR: Missing command name!");
			ci.println(getHelp());
			return;
		}

		final Class<? extends Command> cmdClass = commands.get(command);
		if (cmdClass == null) {
			ci.println("ERROR: Command not found!");
			ci.println(getHelp());
			return;
		}

		Command cmd;
		try {
			cmd = cmdClass.newInstance();
		} catch (final Exception e) {
			ci.println("ERROR: " + ExceptionUtils.getRootCauseMessage(e));
			return;
		}

		for (String arg = ci.nextArgument(); arg != null; arg = ci.nextArgument()) {
			args.add(arg);
		}

		final CmdLineParser parser = new CmdLineParser(cmd);
		try {
			parser.parseArgument(args.toArray(new String[args.size()]));
		} catch (final CmdLineException e) {
			ci.println("ERROR: " + e.getMessage());
			final StringWriter stringWriter = new StringWriter();
			parser.printUsage(stringWriter, null);
			stringWriter.flush();
			ci.println(stringWriter.toString());
			return;
		}

		try {
			cmd.execute(ci);
		} catch (final Exception e) {
			ci.println("ERROR: " + ExceptionUtils.getRootCauseMessage(e));
			if (printStackTraces) {
				ci.printStackTrace(e);
			}
		}
	}

	/**
	 * Returns the command name, i.e. the name of the public method that the
	 * Equinox command interpreter will invoke (without leading '_').
	 * 
	 * @return the command name
	 */
	protected abstract String getCommandName();

	/**
	 * Answer a string (may be as many lines as you like) with help texts that
	 * explain the command.
	 * <p>
	 * The default implementation returns a list of registered commands
	 * including their descriptions).
	 * </p>
	 * 
	 * @see org.eclipse.osgi.framework.console.CommandProvider#getHelp()
	 */
	@Override
	public String getHelp() {
		final StrBuilder help = new StrBuilder(512);
		help.append("---").append(getClass().getSimpleName()).appendln("---");
		help.append("\t").append(getCommandName()).appendln(" <cmd> [args]");
		for (final String name : commands.keySet()) {
			try {
				final Command command = commands.get(name).newInstance();
				final CmdLineParser parser = new CmdLineParser(command);
				help.append("\t\t").append(name).append(" - ").appendln(command.getDescription());
				help.append("\t\t\t");
				parser.printSingleLineUsage(help.asWriter(), null);
				help.appendNewLine();
			} catch (final Exception e) {
				help.append("\t\t").append(name).append(" - ").appendln(ExceptionUtils.getRootCauseMessage(e));
			}
		}
		return help.toString();
	}

	/**
	 * Registers a command
	 * 
	 * @param name
	 *            the command name
	 * @param commandClass
	 *            the command implementation class
	 */
	protected final void registerCommand(final String name, final Class<? extends Command> commandClass) {
		commands.put(name, commandClass);
	}

	/**
	 * Unregisters a command
	 * 
	 * @param name
	 */
	protected final void unregisterCommand(final String name) {
		commands.remove(name);
	}
}
