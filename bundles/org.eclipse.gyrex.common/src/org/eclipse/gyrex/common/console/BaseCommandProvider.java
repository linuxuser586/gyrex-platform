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
		final String command = ci.nextArgument();
		if (StringUtils.isBlank(command)) {
			ci.println("ERROR: Missing command name!");
			ci.println(getHelp());
			return;
		} else if (CommandUtil.isHelpOption(command)) {
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

		boolean printHelp = false;
		final List<String> args = new ArrayList<String>();
		for (String arg = ci.nextArgument(); arg != null; arg = ci.nextArgument()) {
			if (CommandUtil.isHelpOption(arg)) {
				printHelp = true;
				break;
			}
			args.add(arg);
		}

		final CmdLineParser parser = new CmdLineParser(cmd);
		if (printHelp) {
			CommandUtil.printCommandHelp(ci, getCommandName(), command, parser);
			return;
		}

		try {
			parser.parseArgument(args.toArray(new String[args.size()]));
		} catch (final CmdLineException e) {
			ci.println("ERROR: " + e.getMessage());
			CommandUtil.printCommandHelp(ci, getCommandName(), command, parser);
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
		final String commandName = getCommandName();
		CommandUtil.appendCommandInfo(help, commandName, commands, "\t");
		return help.toString();
	}

	/**
	 * Registers a sub-command
	 * 
	 * @param commandClass
	 *            the command implementation class
	 */
	protected final void registerCommand(final Class<? extends SubCommand> commandClass) {
		try {
			commands.put(commandClass.newInstance().getCommandName(), commandClass);
		} catch (final Exception e) {
			throw new IllegalStateException(String.format("Unable to register command class '%s'. %s", commandClass.getSimpleName(), e.getMessage()), e);
		}
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
