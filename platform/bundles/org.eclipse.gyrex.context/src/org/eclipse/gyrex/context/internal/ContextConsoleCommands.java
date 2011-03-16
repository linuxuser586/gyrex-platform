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
package org.eclipse.gyrex.context.internal;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.gyrex.context.internal.registry.ContextDefinition;
import org.eclipse.gyrex.context.internal.registry.ContextRegistryImpl;

import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.text.StrBuilder;

/**
 * Commands for software installations
 */
public class ContextConsoleCommands implements CommandProvider {

	static abstract class Command {
		private final String help;

		/**
		 * Creates a new instance.
		 */
		public Command(final String help) {
			this.help = help;
		}

		public abstract void execute(ContextRegistryImpl registry, CommandInterpreter ci) throws Exception;

		public String getHelp() {
			return help;
		}

		protected void printInvalidArgs(final String errorMessage, final CommandInterpreter ci) {
			ci.println("ERROR: invalid arguments: " + errorMessage);
			ci.println("\t" + getHelp());
		}
	}

	static final Map<String, Command> commands = new TreeMap<String, Command>();
	static {
		commands.put("ls", new Command("list defined contexts") {
			@Override
			public void execute(final ContextRegistryImpl registry, final CommandInterpreter ci) throws Exception {
				final Collection<ContextDefinition> contexts = registry.getDefinedContexts();

				final SortedMap<String, ContextDefinition> sortedContexts = new TreeMap<String, ContextDefinition>();
				for (final ContextDefinition contextDefinition : contexts) {
					sortedContexts.put(contextDefinition.getPath().toString(), contextDefinition);
				}

				for (final String key : sortedContexts.keySet()) {
					ci.println(sortedContexts.get(key));
				}
			}
		});
		commands.put("define", new Command("<path> <name> - defines a context") {
			@Override
			public void execute(final ContextRegistryImpl registry, final CommandInterpreter ci) throws Exception {
				final String pathStr = ci.nextArgument();
				if (StringUtils.isBlank(pathStr) || !Path.EMPTY.isValidPath(pathStr)) {
					ci.println("ERROR: invalid path");
					ci.println(getHelp());
					return;
				}

				final String name = ci.nextArgument();
				if (StringUtils.isBlank(name)) {
					ci.println("ERROR: invalid name");
					ci.println(getHelp());
					return;
				}

				final ContextDefinition definition = new ContextDefinition(new Path(pathStr));
				definition.setName(name);
				registry.saveDefinition(definition);
				ci.println("context added");
			}
		});
		commands.put("rm", new Command("<path> - removes a context") {
			@Override
			public void execute(final ContextRegistryImpl registry, final CommandInterpreter ci) throws Exception {
				final String pathStr = ci.nextArgument();
				if (StringUtils.isBlank(pathStr) || !Path.EMPTY.isValidPath(pathStr)) {
					ci.println("ERROR: invalid path");
					ci.println(getHelp());
					return;
				}

				registry.removeDefinition(new ContextDefinition(new Path(pathStr)));
				ci.println("context removed");
			}
		});
		commands.put("flush", new Command("<path> - flushes a context") {
			@Override
			public void execute(final ContextRegistryImpl registry, final CommandInterpreter ci) throws Exception {
				final String pathStr = ci.nextArgument();
				if (StringUtils.isBlank(pathStr) || !Path.EMPTY.isValidPath(pathStr)) {
					ci.println("ERROR: invalid path");
					ci.println(getHelp());
					return;
				}

				final GyrexContextHandle contextHandle = registry.get(new Path(pathStr));
				if (null == contextHandle) {
					ci.println("ERROR: context not found");
					return;
				}

				registry.flushContextHierarchy(contextHandle);
				ci.println("context flushed");
			}
		});
	}

	static void printHelp(final CommandInterpreter ci) {
		ci.println("context <cmd> [args]");
		for (final String cmd : commands.keySet()) {
			ci.println("\t" + cmd + " " + commands.get(cmd).getHelp());
		}
	}

	public void _context(final CommandInterpreter ci) throws Exception {
		final String command = ci.nextArgument();
		if (command == null) {
			printHelp(ci);
			return;
		}

		final Command cmd = commands.get(command);
		if (cmd == null) {
			ci.println("ERROR: unknown command " + command);
			printHelp(ci);
			return;
		}

		ContextRegistryImpl registry = null;
		try {
			registry = ContextActivator.getInstance().getContextRegistryImpl();
		} catch (final IllegalStateException e) {
			ci.println("ERROR: Context registry not available!");
			return;
		}

		try {
			cmd.execute(registry, ci);
		} catch (final Exception e) {
			if (ContextDebug.debug) {
				ci.printStackTrace(e);
			} else {
				ci.println(ExceptionUtils.getRootCauseMessage(e));
			}
		}
	}

	@Override
	public String getHelp() {
		final StrBuilder help = new StrBuilder(512);
		help.appendln("---Runtime Context Commands---");
		help.appendln("\tcontext <cmd> [args]");
		for (final String cmd : commands.keySet()) {
			help.appendln("\t\t" + cmd + " " + commands.get(cmd).getHelp());
		}
		return help.toString();
	}

}
