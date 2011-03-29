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
package org.eclipse.gyrex.preferences.internal;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.gyrex.preferences.internal.util.EclipsePreferencesUtil;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import org.osgi.service.prefs.Preferences;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.text.StrBuilder;

/**
 * Commands for software installations
 */
public class PreferencesConsoleCommands implements CommandProvider {

	static abstract class Command {
		private final String help;

		/**
		 * Creates a new instance.
		 */
		public Command(final String help) {
			this.help = help;
		}

		public abstract void execute(IPreferencesService preferencesService, CommandInterpreter ci) throws Exception;

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
		commands.put("ls", new Command("[-r] [<path>] - list preferences") {

			@Override
			public void execute(final IPreferencesService preferencesService, final CommandInterpreter ci) throws Exception {
				boolean recursive = false;

				String path = ci.nextArgument();
				if (StringUtils.equals("-r", path)) {
					recursive = true;
					path = ci.nextArgument();
				}

				final IEclipsePreferences node = preferencesService.getRootNode();

				final String[] decodedPath = EclipsePreferencesUtil.decodePath(StringUtils.trimToEmpty(path));
				final Preferences preferencesNode = node.node(StringUtils.trimToEmpty(decodedPath[0]));
				if (recursive) {
					printTree(0, preferencesNode, ci);
				} else {
					if (StringUtils.isNotBlank(decodedPath[1])) {
						printValue(ci, preferencesNode, decodedPath[1]);
					} else {
						final String[] keys = preferencesNode.keys();
						for (final String key : keys) {
							printValue(ci, preferencesNode, key);
						}
						final String[] childrenNames = preferencesNode.childrenNames();
						for (final String child : childrenNames) {
							printChildInfo(0, preferencesNode.node(child), ci);
						}
					}
				}
			}

			private void printChildInfo(final int indent, final Preferences node, final CommandInterpreter ci) throws Exception {
				final String[] children = node.childrenNames();
				final StrBuilder spaces = new StrBuilder();
				for (int i = 0; i < indent; i++) {
					spaces.append(" ");
				}
				ci.println(spaces.append(node.absolutePath()).append(" (").append(children.length).append(")"));
			}

			private void printTree(final int indent, final Preferences node, final CommandInterpreter ci) throws Exception {
				printChildInfo(indent, node, ci);

				final String[] children = node.childrenNames();
				for (final String child : children) {
					printTree(indent + 1, node.node(child), ci);
				}

			}

			private void printValue(final CommandInterpreter ci, final Preferences preferencesNode, final String key) {
				final String value = preferencesNode.get(key, null);
				if (null != value) {
					ci.println(String.format("%s: %s=%s", preferencesNode.absolutePath(), key, value));
				} else {
					ci.println(String.format("%s: %s not set", preferencesNode.absolutePath(), key));
				}
			}
		});

		commands.put("export", new Command("<file> [<path>]- export to file") {

			@Override
			public void execute(final IPreferencesService preferencesService, final CommandInterpreter ci) throws Exception {
				final String filePath = ci.nextArgument();
				if (StringUtils.isBlank(filePath)) {
					ci.println("ERROR: missing file");
					printHelp(ci);
					return;
				} else if (!Path.EMPTY.isValidPath(filePath)) {
					ci.println("ERROR: invalid file path name");
					return;
				}

				IPath file = new Path(filePath);
				if (!StringUtils.equals(file.getFileExtension(), "epf")) {
					file = file.addFileExtension("epf");
				}

				IEclipsePreferences node = preferencesService.getRootNode();

				final String path = ci.nextArgument();
				if (StringUtils.isNotBlank(path)) {
					node = (IEclipsePreferences) node.node(path);
				}

				final FileOutputStream output = FileUtils.openOutputStream(file.toFile());
				try {
					preferencesService.exportPreferences(node, output, null);
				} finally {
					IOUtils.closeQuietly(output);
				}
			}
		});

		commands.put("import", new Command("<file> import from file") {

			@Override
			public void execute(final IPreferencesService preferencesService, final CommandInterpreter ci) throws Exception {
				final String filePath = ci.nextArgument();
				if (StringUtils.isBlank(filePath)) {
					ci.println("ERROR: missing file");
					printHelp(ci);
					return;
				} else if (!Path.EMPTY.isValidPath(filePath)) {
					ci.println("ERROR: invalid file path name");
					return;
				}

				IPath file = new Path(filePath);
				if (!StringUtils.equals(file.getFileExtension(), "epf")) {
					file = file.addFileExtension("epf");
				}

				if (!file.toFile().isFile()) {
					ci.println("ERROR: file not found");
					return;
				}

				final FileInputStream in = FileUtils.openInputStream(file.toFile());
				try {
					preferencesService.importPreferences(in);
				} finally {
					IOUtils.closeQuietly(in);
				}
			}
		});

	}

	static void printHelp(final CommandInterpreter ci) {
		ci.println("prefs <cmd> [args]");
		for (final String cmd : commands.keySet()) {
			ci.println("\t" + cmd + " " + commands.get(cmd).getHelp());
		}
	}

	public void _prefs(final CommandInterpreter ci) throws Exception {
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

		final IPreferencesService preferencesService;
		try {
			preferencesService = EclipsePreferencesUtil.getPreferencesService();
		} catch (final IllegalStateException e) {
			ci.println("ERROR: Required services not available! " + e.getMessage());
			return;
		}

		try {
			cmd.execute(preferencesService, ci);
		} catch (final Exception e) {
			if (PreferencesDebug.debug) {
				ci.printStackTrace(e);
			} else {
				ci.println(ExceptionUtils.getRootCauseMessage(e));
			}
		}
	}

	@Override
	public String getHelp() {
		final StrBuilder help = new StrBuilder(512);
		help.appendln("---Preferences Commands---");
		help.appendln("\tprefs <cmd> [args]");
		for (final String cmd : commands.keySet()) {
			help.appendln("\t\t" + cmd + " " + commands.get(cmd).getHelp());
		}
		return help.toString();
	}

}
