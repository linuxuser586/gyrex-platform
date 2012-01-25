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
package org.eclipse.gyrex.common.console;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.osgi.framework.console.CommandInterpreter;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

/**
 * Utility for working with commands
 */
class CommandUtil {

	static void appendCommandDescription(final StrBuilder help, final String name, final Command command, final String ident) {
		help.append(ident).append(name);
		final String description = command.getDescription();
		if (!StringUtils.contains(description, " - ")) {
			help.append(" - ");
		} else if (!StringUtils.startsWith(description, " ")) {
			help.append(' ');
		}
		help.appendln(description);
	}

	static void appendCommandInfo(final StrBuilder help, final String commandName, final Map<String, Class<? extends Command>> commands, String ident) {
		help.append(ident).append(commandName).appendln(" <cmd> [args]");
		ident += "\t";
		for (final String name : commands.keySet()) {
			try {
				final Command command = commands.get(name).newInstance();
				if (command instanceof SubCommand) {
					appendCommandInfo(help, name, ((SubCommand) command).commands, ident);
				} else {
					appendCommandDescription(help, name, command, ident);
				}
			} catch (final Exception e) {
				help.append(ident).append(name).append(" - ").appendln(ExceptionUtils.getRootCauseMessage(e));
			}
		}
	}

	static void executeCommand(final CommandInterpreter ci, final Class<? extends Command> cmdClass, final String command, final String parentCommandName, final boolean printErrorDetails) {
		Command cmd;
		try {
			cmd = cmdClass.newInstance();
		} catch (final Exception e) {
			ci.println("ERROR: " + ExceptionUtils.getRootCauseMessage(e));
			return;
		}

		// check for sub-command
		if (cmd instanceof SubCommand) {
			// sub-command usually don't have arguments, thus we don't consume the CI for them
			((SubCommand) cmd).printErrorDetails = printErrorDetails;
			((SubCommand) cmd).parentCommandName = parentCommandName;
		} else {
			// parse commands using ARG4J
			boolean printHelp = false;
			final List<String> args = new ArrayList<String>();
			for (String arg = ci.nextArgument(); arg != null; arg = ci.nextArgument()) {
				if (isHelpOption(arg)) {
					printHelp = true;
					break;
				}
				args.add(arg);
			}

			final CmdLineParser parser = new CmdLineParser(cmd);
			if (printHelp) {
				printCommandHelp(ci, parentCommandName, command, parser);
				return;
			}

			try {
				parser.parseArgument(args.toArray(new String[args.size()]));
			} catch (final CmdLineException e) {
				ci.println("ERROR: " + e.getMessage());
				printCommandHelp(ci, parentCommandName, command, parser);
				return;
			}
		}

		// execute command
		try {
			cmd.execute(ci);
		} catch (final Exception e) {
			ci.println("ERROR: " + ExceptionUtils.getRootCauseMessage(e));
			if (printErrorDetails) {
				ci.printStackTrace(e);
			}
		}
	}

	static boolean isHelpOption(final String arg) {
		return StringUtils.equals("-h", arg) || StringUtils.equals("--help", arg);
	}

	static void printCommandHelp(final CommandInterpreter ci, final String name, final String command, final CmdLineParser parser) {
		final StringWriter stringWriter = new StringWriter();
		stringWriter.append(String.format("%s %s", name, command));
		parser.printSingleLineUsage(stringWriter, null);
		stringWriter.append(SystemUtils.LINE_SEPARATOR);
		parser.printUsage(stringWriter, null);
		stringWriter.flush();
		ci.println(stringWriter.toString());
	}

	private CommandUtil() {
		// empty
	}
}
