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

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;

import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.zookeeper.CreateMode;

public class CloudConsoleCommands implements CommandProvider {

	static abstract class Command {
		private final String help;

		/**
		 * Creates a new instance.
		 */
		public Command(final String help) {
			this.help = help;
		}

		public abstract void execute(ZooKeeperGate gate, CommandInterpreter ci) throws Exception;

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
			public void execute(final ZooKeeperGate zk, final CommandInterpreter ci) throws Exception {
				final String a1 = ci.nextArgument();
				IPath path = null;
				if (StringUtils.startsWith(APPROVED, a1)) {
					path = IZooKeeperLayout.PATH_NODES_APPROVED;
				} else if (StringUtils.startsWith(PENDING, a1)) {
					path = IZooKeeperLayout.PATH_NODES_PENDING;
				}

				if (path == null) {
					printInvalidArgs(ci);
					return;
				}

				final Collection<String> names = zk.readChildrenNames(path);
				for (final String nodeId : names) {
					final String info = zk.readRecord(IZooKeeperLayout.PATH_NODES_PENDING.append(nodeId), "", null);
					if (info.length() > 0) {
						ci.println("Node " + nodeId + " (" + info + ")");
					} else {
						ci.println("Node " + nodeId);
					}
				}
			}
		});
		cloudCommands.put("approve", new Command("nodeId") {
			@Override
			public void execute(final ZooKeeperGate zk, final CommandInterpreter ci) throws Exception {
				final String nodeId = ci.nextArgument();
				if (nodeId == null) {
					printInvalidArgs(ci);
					return;
				}

				final Properties nodeData = new Properties();
				final String info = zk.readRecord(IZooKeeperLayout.PATH_NODES_PENDING.append(nodeId), "", null);
				if (info.length() > 0) {
					nodeData.put("location", info);
				}

				final ByteArrayOutputStream out = new ByteArrayOutputStream();
				nodeData.store(out, getHelp());

				zk.createPath(IZooKeeperLayout.PATH_NODES_APPROVED.append(nodeId), CreateMode.PERSISTENT, out.toByteArray());
				zk.deletePath(IZooKeeperLayout.PATH_NODES_PENDING.append(nodeId));

				ci.println("Node " + nodeId + " approved!");
			}
		});
		cloudCommands.put("retire", new Command("nodeId") {
			@Override
			public void execute(final ZooKeeperGate zk, final CommandInterpreter ci) throws Exception {
				final String nodeId = ci.nextArgument();
				if (nodeId == null) {
					printInvalidArgs(ci);
					return;
				}

				zk.deletePath(IZooKeeperLayout.PATH_NODES_APPROVED.append(nodeId));

				ci.println("Node " + nodeId + " retired!");
			}
		});
	}

	static void _cloudHelp(final CommandInterpreter ci) {
		ci.println("cloud <cmd> [args]");
		for (final String cmd : cloudCommands.keySet()) {
			ci.println("\t" + cmd + " " + cloudCommands.get(cmd).getHelp());
		}
	}

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

		ZooKeeperGate gate = null;
		try {
			gate = ZooKeeperGate.get();
		} catch (final Exception e) {
			ci.println("ZooKeeper not connected! " + e.getMessage());
			return;
		}
		cmd.execute(gate, ci);
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
}
