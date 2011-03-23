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
package org.eclipse.gyrex.p2.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.equinox.p2.metadata.Version;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.p2.internal.installer.PackageScanner;
import org.eclipse.gyrex.p2.internal.packages.IPackageManager;
import org.eclipse.gyrex.p2.internal.packages.InstallableUnitReference;
import org.eclipse.gyrex.p2.internal.packages.PackageDefinition;
import org.eclipse.gyrex.p2.internal.repositories.IRepositoryDefinitionManager;
import org.eclipse.gyrex.p2.internal.repositories.RepositoryDefinition;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.text.StrBuilder;

/**
 * Commands for software installations
 */
public class SoftwareConsoleCommands implements CommandProvider {

	static abstract class Command {
		private final String help;

		/**
		 * Creates a new instance.
		 */
		public Command(final String help) {
			this.help = help;
		}

		public abstract void execute(IRepositoryDefinitionManager repoManager, IPackageManager packageManager, CommandInterpreter ci) throws Exception;

		public String getHelp() {
			return help;
		}

		protected void printInvalidArgs(final String errorMessage, final CommandInterpreter ci) {
			ci.println("ERROR: invalid arguments: " + errorMessage);
			ci.println("\t" + getHelp());
		}
	}

	private static final class ListCommand extends Command {
		private ListCommand() {
			super(" repos|packages [filterString] \t - list repos or packages");
		}

		@Override
		public void execute(final IRepositoryDefinitionManager repoManager, final IPackageManager packageManager, final CommandInterpreter ci) throws Exception {
			final String what = ci.nextArgument();
			if (what == null) {
				printInvalidArgs("Don't know what to list. Repos? Packages?", ci);
				return;
			}

			final String filterString = ci.nextArgument();
			if (StringUtils.startsWithIgnoreCase("repos", what)) {
				final Collection<RepositoryDefinition> repos = repoManager.getRepositories();
				for (final RepositoryDefinition repo : repos) {
					if ((null == filterString) || StringUtils.contains(repo.getId(), filterString) || ((null != repo.getLocation()) && StringUtils.contains(repo.getLocation().toString(), filterString))) {
						ci.println(String.format("%s [%s]", repo.getId(), repo.toString()));
					}
				}
			} else if (StringUtils.startsWithIgnoreCase("packages", what)) {
				final Collection<PackageDefinition> packages = packageManager.getPackages();
				for (final PackageDefinition pkdefinition : packages) {
					if ((null == filterString) || StringUtils.contains(pkdefinition.getId(), filterString)) {
						ci.println(String.format("%s [%s]", pkdefinition.getId(), pkdefinition.toString()));
					}
				}
			} else {
				printInvalidArgs("repos|packages expected", ci);
				return;
			}

		}
	}

	static final Map<String, Command> commands = new TreeMap<String, Command>();
	static {
		commands.put("ls", new ListCommand());
		commands.put("addRepo", new Command("<id> <uri> - adds a repository") {

			@Override
			public void execute(final IRepositoryDefinitionManager repoManager, final IPackageManager packageManager, final CommandInterpreter ci) throws Exception {
				final String id = ci.nextArgument();
				if (!IdHelper.isValidId(id)) {
					printInvalidArgs("invalid repo id", ci);
					return;
				}

				final String uri = ci.nextArgument();
				if (null == uri) {
					printInvalidArgs("uri is missing", ci);
					return;
				}

				URI location;
				try {
					location = new URI(uri);
				} catch (final URISyntaxException e) {
					ci.println("invalid uri:" + e.getMessage());
					return;
				}

				final RepositoryDefinition repo = new RepositoryDefinition();
				repo.setId(id);
				repo.setLocation(location);
				repoManager.saveRepository(repo);
				ci.println("repository added");
			}

		});
		commands.put("rmRepo", new Command("<id> - removes a repository") {

			@Override
			public void execute(final IRepositoryDefinitionManager repoManager, final IPackageManager packageManager, final CommandInterpreter ci) throws Exception {
				final String id = ci.nextArgument();
				if (!IdHelper.isValidId(id)) {
					printInvalidArgs("invalid repo id", ci);
					return;
				}

				repoManager.removeRepository(id);
				ci.println("repository removed");
			}
		});
		commands.put("addPkg", new Command("<id> - adds a package") {

			@Override
			public void execute(final IRepositoryDefinitionManager repoManager, final IPackageManager packageManager, final CommandInterpreter ci) throws Exception {
				final String id = ci.nextArgument();
				if (!IdHelper.isValidId(id)) {
					printInvalidArgs("invalid package id", ci);
					return;
				}

				final PackageDefinition packageDefinition = new PackageDefinition();
				packageDefinition.setId(id);
				packageManager.savePackage(packageDefinition);
				ci.println("package added");
			}

		});
		commands.put("rmPkg", new Command("<id> - removes a package") {

			@Override
			public void execute(final IRepositoryDefinitionManager repoManager, final IPackageManager packageManager, final CommandInterpreter ci) throws Exception {
				final String id = ci.nextArgument();
				if (!IdHelper.isValidId(id)) {
					printInvalidArgs("invalid package id", ci);
					return;
				}

				packageManager.removePackage(id);
				ci.println("package removed");
			}
		});

		commands.put("addIU2Pkg", new Command("<packageId> <installableUnitId> [<installUnitVersion>] - adds an installable unit to a package") {

			@Override
			public void execute(final IRepositoryDefinitionManager repoManager, final IPackageManager packageManager, final CommandInterpreter ci) throws Exception {
				final String id = ci.nextArgument();
				if (!IdHelper.isValidId(id)) {
					printInvalidArgs("invalid package id", ci);
					return;
				}

				final PackageDefinition packageDefinition = packageManager.getPackage(id);
				if (null == packageDefinition) {
					ci.println("package not found");
					return;
				}

				// verify the package is not marked for roll-out
				if (packageManager.isMarkedForInstall(packageDefinition)) {
					ci.println("Package already rolled-out! Please create new package for updates or revoke package first.");
					return;
				}

				final InstallableUnitReference iu = new InstallableUnitReference();
				final String iuId = ci.nextArgument();
				if (!IdHelper.isValidId(iuId)) {
					printInvalidArgs("invalid installable unit id", ci);
					return;
				}
				iu.setId(iuId);

				final String iuVersion = ci.nextArgument();
				if (null != iuVersion) {
					try {
						iu.setVersion(Version.create(iuVersion));
					} catch (final IllegalArgumentException e) {
						printInvalidArgs("invalid installable unit version: " + e.getMessage(), ci);
						return;
					}
				}

				packageDefinition.addComponentToInstall(iu);
				packageManager.savePackage(packageDefinition);
				ci.println("package updated");
			}

		});

		commands.put("rollout", new Command("<packageId> - rolls out a package") {

			@Override
			public void execute(final IRepositoryDefinitionManager repoManager, final IPackageManager packageManager, final CommandInterpreter ci) throws Exception {
				final String id = ci.nextArgument();
				if (!IdHelper.isValidId(id)) {
					printInvalidArgs("invalid package id", ci);
					return;
				}

				final PackageDefinition packageDefinition = packageManager.getPackage(id);
				if (null == packageDefinition) {
					ci.println("package not found");
					return;
				}

				packageManager.markedForInstall(packageDefinition);
				ci.println("package marked for rollout");
			}

		});

		commands.put("revoke", new Command("<packageId> - revokes a package") {

			@Override
			public void execute(final IRepositoryDefinitionManager repoManager, final IPackageManager packageManager, final CommandInterpreter ci) throws Exception {
				final String id = ci.nextArgument();
				if (!IdHelper.isValidId(id)) {
					printInvalidArgs("invalid package id", ci);
					return;
				}

				final PackageDefinition packageDefinition = packageManager.getPackage(id);
				if (null == packageDefinition) {
					ci.println("package not found");
					return;
				}

				packageManager.markedForUninstall(packageDefinition);
				ci.println("package revoked");
			}

		});

		commands.put("update", new Command("updated the local node") {

			@Override
			public void execute(final IRepositoryDefinitionManager repoManager, final IPackageManager packageManager, final CommandInterpreter ci) throws Exception {
				final PackageScanner packageScanner = PackageScanner.getInstance();
				if (packageScanner.getState() == Job.RUNNING) {
					ci.println("update already in progess");
					return;
				} else {
					if (!packageScanner.cancel()) {
						ci.println("update already in progess; unable to cancel");
						return;
					}
				}

				// enable debug logging
				final boolean wasDebugging = P2Debug.nodeInstallation;
				P2Debug.nodeInstallation = true;

				// execute
				packageScanner.schedule();
				packageScanner.join();

				// reset logging
				P2Debug.nodeInstallation = wasDebugging;
				ci.println("update finished");
			}

		});

	}

	static void printHelp(final CommandInterpreter ci) {
		ci.println("sw <cmd> [args]");
		for (final String cmd : commands.keySet()) {
			ci.println("\t" + cmd + " " + commands.get(cmd).getHelp());
		}
	}

	public void _sw(final CommandInterpreter ci) throws Exception {
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

		IRepositoryDefinitionManager repoManager = null;
		try {
			repoManager = P2Activator.getInstance().getRepositoryManager();
		} catch (final IllegalStateException e) {
			ci.println("ERROR: Repository manager not available!");
			return;
		}
		IPackageManager packageManager = null;
		try {
			packageManager = P2Activator.getInstance().getPackageManager();
		} catch (final IllegalStateException e) {
			ci.println("ERROR: Package manager not available!");
			return;
		}
		try {
			cmd.execute(repoManager, packageManager, ci);
		} catch (final Exception e) {
			if (P2Debug.debug) {
				ci.printStackTrace(e);
			} else {
				ci.println(ExceptionUtils.getRootCauseMessage(e));
			}
		}
	}

	@Override
	public String getHelp() {
		final StrBuilder help = new StrBuilder(512);
		help.appendln("---Software Commands---");
		help.appendln("\tsw <cmd> [args]");
		for (final String cmd : commands.keySet()) {
			help.appendln("\t\t" + cmd + " " + commands.get(cmd).getHelp());
		}
		return help.toString();
	}

}
