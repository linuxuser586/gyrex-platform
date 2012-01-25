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

import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.text.StrBuilder;

/**
 * Base class for commands which contain other commands.
 */
public abstract class SubCommand extends Command {

	String parentCommandName;
	boolean printErrorDetails;

	final Map<String, Class<? extends Command>> commands = new TreeMap<String, Class<? extends Command>>();

	private final String name;

	/**
	 * Creates a new instance.
	 * 
	 * @param name
	 *            the command name, i.e. the name under which the sub command is
	 *            registered with the provider/parent command and shall be
	 *            invoked from the console
	 */
	public SubCommand(final String name) {
		super("<cmd> [args]");
		this.name = name;
		if (StringUtils.isBlank(name)) {
			throw new IllegalArgumentException("command name must not be null");
		}
	}

	@Override
	protected final void doExecute() throws Exception {
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

		CommandUtil.executeCommand(ci, cmdClass, command, null != parentCommandName ? String.format("%s %s", parentCommandName, getCommandName()) : getCommandName(), printErrorDetails);
	}

	/**
	 * @return the command name
	 */
	protected final String getCommandName() {
		return name;
	}

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
	public String getHelp() {
		final StrBuilder help = new StrBuilder(512);
		if (null == parentCommandName) {
			help.append("---").append(getClass().getSimpleName()).appendln("---");
			help.append("\t").append(getCommandName()).appendln(" <cmd> [args]");
		} else {
			help.append("\t").append(parentCommandName).append(' ').append(getCommandName()).appendln(" <cmd> [args]");
		}
		for (final String name : commands.keySet()) {
			try {
				help.append("\t\t").append(name);
				final Command command = commands.get(name).newInstance();
//				final CmdLineParser parser = new CmdLineParser(command);
				final String description = command.getDescription();
				if (!StringUtils.contains(description, " - ")) {
					help.append(" - ");
				} else if (!StringUtils.startsWith(description, " ")) {
					help.append(' ');
				}
				help.appendln(description);
//				help.append("\t\t\t");
//				parser.printSingleLineUsage(help.asWriter(), null);
//				help.appendNewLine();
			} catch (final Exception e) {
				help.append("\t\t").append(name).append(" - ").appendln(ExceptionUtils.getRootCauseMessage(e));
			}
		}
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
	 * Registers a sub command
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
	 * Unregisters a sub command
	 * 
	 * @param name
	 */
	protected final void unregisterCommand(final String name) {
		commands.remove(name);
	}

}
