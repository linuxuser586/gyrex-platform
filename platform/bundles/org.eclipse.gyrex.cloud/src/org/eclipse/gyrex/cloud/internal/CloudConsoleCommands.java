/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.gyrex.cloud.admin.ICloudManager;
import org.eclipse.gyrex.cloud.admin.INodeDescriptor;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;

public class CloudConsoleCommands implements CommandProvider {

	static abstract class Command {
		private final String help;

		/**
		 * Creates a new instance.
		 */
		public Command(final String help) {
			this.help = help;
		}

		public abstract void execute(ICloudManager cloudManager, CommandInterpreter ci) throws Exception;

		public String getHelp() {
			return help;
		}

		protected void printInvalidArgs(final CommandInterpreter ci) {
			ci.println("ERROR: invalid arguments");
			ci.println("\t" + getHelp());
		}
	}

	/** APPROVED */
	static final String APPROVED = "approved";

	/** PENDING */
	static final String PENDING = "pending";

	static final Map<String, Command> cloudCommands = new TreeMap<String, Command>();
	static {
		cloudCommands.put("ls", new Command("pending|approved") {
			@Override
			public void execute(final ICloudManager cloudManager, final CommandInterpreter ci) throws Exception {
				final String a1 = ci.nextArgument();
				Collection<INodeDescriptor> nodes = null;
				if (StringUtils.startsWith(APPROVED, a1)) {
					nodes = cloudManager.getApprovedNodes();
				} else if (StringUtils.startsWith(PENDING, a1)) {
					nodes = cloudManager.getPendingNodes();
				}

				if (nodes == null) {
					printInvalidArgs(ci);
					return;
				}

				for (final INodeDescriptor node : nodes) {
					ci.println("Node " + node.getId() + " (" + node.getLocation() + ")");
				}
			}
		});
		cloudCommands.put("approve", new Command("nodeId") {
			@Override
			public void execute(final ICloudManager cloudManager, final CommandInterpreter ci) throws Exception {
				final String nodeId = ci.nextArgument();
				if (nodeId == null) {
					printInvalidArgs(ci);
					return;
				}

				final IStatus status = cloudManager.approveNode(nodeId);

				if (status.isOK()) {
					ci.println("Node " + nodeId + " approved!");
				} else {
					ci.println(status.getMessage());
				}
			}
		});
		cloudCommands.put("retire", new Command("nodeId") {
			@Override
			public void execute(final ICloudManager cloudManager, final CommandInterpreter ci) throws Exception {
				final String nodeId = ci.nextArgument();
				if (nodeId == null) {
					printInvalidArgs(ci);
					return;
				}

				final IStatus status = cloudManager.retireNode(nodeId);

				if (status.isOK()) {
					ci.println("Node " + nodeId + " retired!");
				} else {
					ci.println(status.getMessage());
				}
			}
		});
	}

	static void _cloudHelp(final CommandInterpreter ci) {
		ci.println("cloud <cmd> [args]");
		for (final String cmd : cloudCommands.keySet()) {
			ci.println("\t" + cmd + " " + cloudCommands.get(cmd).getHelp());
		}
	}

	private ICloudManager cloudManager;

	public void _cloud(final CommandInterpreter ci) throws Exception {
		final String command = ci.nextArgument();
		if (command == null) {
			_cloudHelp(ci);
			return;
		}

		final Command cmd = cloudCommands.get(command);
		if (cmd == null) {
			ci.println("ERROR: unknown ZooKeeper command " + command);
			_cloudHelp(ci);
			return;
		}

		ICloudManager cloudManager = null;
		try {
			cloudManager = getCloudManager();
		} catch (final Exception e) {
			ci.println("ZooKeeper not connected! " + e.getMessage());
			return;
		}
		cmd.execute(cloudManager, ci);
	}

	/**
	 * Returns the cloudManager.
	 * 
	 * @return the cloudManager
	 */
	public ICloudManager getCloudManager() {
		final ICloudManager manager = cloudManager;
		if (manager == null) {
			throw new IllegalStateException("cloud manager not available");
		}
		return manager;
	}

	@Override
	public String getHelp() {
		final StrBuilder help = new StrBuilder(512);
		help.appendln("---Cloud Commands---");
		help.appendln("\tcloud <cmd> [args]");
		for (final String cmd : cloudCommands.keySet()) {
			help.appendln("\t\t" + cmd + " " + cloudCommands.get(cmd).getHelp());
		}
		return help.toString();
	}

	/**
	 * Sets the cloudManager.
	 * 
	 * @param cloudManager
	 *            the cloudManager to set
	 */
	public void setCloudManager(final ICloudManager cloudManager) {
		this.cloudManager = cloudManager;
	}
}
