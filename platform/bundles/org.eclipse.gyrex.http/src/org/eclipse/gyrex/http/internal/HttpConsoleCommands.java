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
package org.eclipse.gyrex.http.internal;

import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.gyrex.http.internal.application.manager.ApplicationManager;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationProviderRegistration;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationProviderRegistry;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.text.StrBuilder;

/**
 * Commands for software installations
 */
public class HttpConsoleCommands implements CommandProvider {

	static abstract class Command {
		private final String help;

		/**
		 * Creates a new instance.
		 */
		public Command(final String help) {
			this.help = help;
		}

		public abstract void execute(ApplicationProviderRegistry registry, ApplicationManager manager, CommandInterpreter ci) throws Exception;

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
		commands.put("ls", new Command("<applications|providers> - lists registered applications or providers") {
			@Override
			public void execute(final ApplicationProviderRegistry registry, final ApplicationManager manager, final CommandInterpreter ci) throws Exception {
				final String what = ci.nextArgument();
				if (StringUtils.isBlank(what)) {
					ci.println("ERROR: please specify what to list");
					ci.println(getHelp());
					return;
				}

				if (StringUtils.startsWith("applications", what) || StringUtils.startsWith("apps", what)) {
					final SortedSet<String> registeredApplications = new TreeSet<String>(manager.getRegisteredApplications());
					for (final String applicationId : registeredApplications) {
						ci.println(manager.getApplicationRegistration(applicationId));
					}
				} else if (StringUtils.startsWith("providers", what)) {
					final SortedMap<String, ApplicationProviderRegistration> providers = new TreeMap<String, ApplicationProviderRegistration>(registry.getRegisteredProviders());
					for (final ApplicationProviderRegistration provider : providers.values()) {
						ci.println(provider);
					}
				}
			}
		});

		commands.put("defineApp", new Command("<applicationId> <providerId> <contextPath> - defines an application") {
			@Override
			public void execute(final ApplicationProviderRegistry registry, final ApplicationManager manager, final CommandInterpreter ci) throws Exception {
				// TODO Auto-generated method stub

			}
		});

		commands.put("removeApp", new Command("<applicationId> - removes an application definition (will also unmount all URLs)") {
			@Override
			public void execute(final ApplicationProviderRegistry registry, final ApplicationManager manager, final CommandInterpreter ci) throws Exception {
				// TODO Auto-generated method stub

			}
		});

		commands.put("setAppProperty", new Command("<applicationId> <propertyKey> <propertyValue> - sets an application property") {
			@Override
			public void execute(final ApplicationProviderRegistry registry, final ApplicationManager manager, final CommandInterpreter ci) throws Exception {
				// TODO Auto-generated method stub

			}
		});

		commands.put("unsetAppProperty", new Command("<applicationId> <propertyKey> - unsets an application property") {
			@Override
			public void execute(final ApplicationProviderRegistry registry, final ApplicationManager manager, final CommandInterpreter ci) throws Exception {
				// TODO Auto-generated method stub

			}
		});

		commands.put("mount", new Command("<applicationId> <url> - mounts an application at the specified URL") {
			@Override
			public void execute(final ApplicationProviderRegistry registry, final ApplicationManager manager, final CommandInterpreter ci) throws Exception {
				// TODO Auto-generated method stub

			}
		});

		commands.put("unmount", new Command("<url> - unmounts the specified URL") {
			@Override
			public void execute(final ApplicationProviderRegistry registry, final ApplicationManager manager, final CommandInterpreter ci) throws Exception {
				// TODO Auto-generated method stub

			}
		});

		commands.put("start", new Command("<applicationId> - starts an application") {
			@Override
			public void execute(final ApplicationProviderRegistry registry, final ApplicationManager manager, final CommandInterpreter ci) throws Exception {
				// TODO Auto-generated method stub

			}
		});

		commands.put("stop", new Command("<applicationId> - stops an application") {
			@Override
			public void execute(final ApplicationProviderRegistry registry, final ApplicationManager manager, final CommandInterpreter ci) throws Exception {
				// TODO Auto-generated method stub

			}
		});

	}

	static void printHelp(final CommandInterpreter ci) {
		ci.println("http <cmd> [args]");
		for (final String cmd : commands.keySet()) {
			ci.println("\t" + cmd + " " + commands.get(cmd).getHelp());
		}
	}

	public void _http(final CommandInterpreter ci) throws Exception {
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

		final ApplicationManager manager;
		final ApplicationProviderRegistry registry;
		try {
			manager = HttpAppManagerApplication.getInstance().getApplicationManager();
			registry = HttpAppManagerApplication.getInstance().getProviderRegistry();
		} catch (final IllegalStateException e) {
			ci.println("ERROR: Required services not available! " + e.getMessage());
			return;
		}

		try {
			cmd.execute(registry, manager, ci);
		} catch (final Exception e) {
			if (HttpDebug.debug) {
				ci.printStackTrace(e);
			} else {
				ci.println(ExceptionUtils.getRootCauseMessage(e));
			}
		}
	}

	@Override
	public String getHelp() {
		final StrBuilder help = new StrBuilder(512);
		help.appendln("---Http Application Commands---");
		help.appendln("\thttp <cmd> [args]");
		for (final String cmd : commands.keySet()) {
			help.appendln("\t\t" + cmd + " " + commands.get(cmd).getHelp());
		}
		return help.toString();
	}

}
