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
package org.eclipse.gyrex.cloud.internal.zk;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import org.apache.commons.lang.text.StrBuilder;
import org.apache.zookeeper.AsyncCallback.VoidCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

/**
 *
 */
public class ZooKeeperConsoleCommands implements CommandProvider {

	static abstract class Command {
		private final String help;

		/**
		 * Creates a new instance.
		 */
		public Command(final String help) {
			this.help = help;
		}

		public abstract void execute(ZooKeeper zk, CommandInterpreter ci) throws Exception;

		public String getHelp() {
			return help;
		}

		protected void printInvalidArgs(final CommandInterpreter ci) {
			ci.println("ERROR: invalid arguments");
			ci.println("\t" + getHelp());
		}
	}

	static final Map<String, Command> zkCommands = new TreeMap<String, Command>();
	static {
		zkCommands.put("create", new Command("[-s] [-e] path [data]") {
			@Override
			public void execute(final ZooKeeper zk, final CommandInterpreter ci) throws Exception {
				String a1 = ci.nextArgument();
				String a2 = ci.nextArgument();
				if (a1 == null) {
					printInvalidArgs(ci);
					return;
				}

				CreateMode flags = CreateMode.PERSISTENT;
				if ((a2 != null) && ((a1.equals("-e") && a2.equals("-s")) || (a1.equals("-s") && (a2.equals("-e"))))) {
					flags = CreateMode.EPHEMERAL_SEQUENTIAL;
					a1 = ci.nextArgument();
					a2 = ci.nextArgument();
				} else if (a1.equals("-e")) {
					a1 = a2;
					a2 = ci.nextArgument();
					flags = CreateMode.EPHEMERAL;
				} else if (a1.equals("-s")) {
					a1 = a2;
					a2 = ci.nextArgument();
					flags = CreateMode.PERSISTENT_SEQUENTIAL;
				}

				if (a1 == null) {
					printInvalidArgs(ci);
					return;
				}

				final String newPath = zk.create(a1, null != a2 ? a2.getBytes() : null, ZooDefs.Ids.OPEN_ACL_UNSAFE, flags);
				ci.println("Created " + newPath);
			}
		});
		zkCommands.put("ls", new Command("[-r] path") {
			@Override
			public void execute(final ZooKeeper zk, final CommandInterpreter ci) throws Exception {
				String path = ci.nextArgument();
				if (path == null) {
					printInvalidArgs(ci);
					return;
				}

				boolean recursive = false;
				if (path.equals("-r")) {
					path = ci.nextArgument();
					recursive = true;
				}
				if (path == null) {
					printInvalidArgs(ci);
					return;
				}

				if (recursive) {
					printTree(path, 0, zk, ci);
				} else {
					final List<String> children = zk.getChildren(path, false);
					for (final String child : children) {
						ci.println(child);
					}
				}
			}

			private void printTree(final String path, final int indent, final ZooKeeper zk, final CommandInterpreter ci) throws Exception {
				final List<String> children = zk.getChildren(path, false);
				final StrBuilder spaces = new StrBuilder();
				for (int i = 0; i < indent; i++) {
					spaces.append(" ");
				}
				ci.println(spaces.append(path).append(" (").append(children.size()).append(")"));

				for (final String child : children) {
					printTree(path + (path.equals("/") ? "" : "/") + child, indent + 1, zk, ci);
				}

			}
		});
		zkCommands.put("rm", new Command("[-r] path") {
			private void deleteTree(final ZooKeeper zk, final String path) throws InterruptedException, KeeperException {
				// delete all children
				final List<String> children = zk.getChildren(path.toString(), false);
				for (final String child : children) {
					deleteTree(zk, path + (path.equals("/") ? "" : "/") + child);
				}
				// delete node itself
				zk.delete(path, -1);
			}

			@Override
			public void execute(final ZooKeeper zk, final CommandInterpreter ci) throws Exception {
				String path = ci.nextArgument();
				if (path == null) {
					printInvalidArgs(ci);
					return;
				}
				boolean recursive = false;
				if (path.equals("-r")) {
					path = ci.nextArgument();
					recursive = true;
				}
				if (path == null) {
					printInvalidArgs(ci);
					return;
				}

				if (recursive) {
					deleteTree(zk, path);
				} else {
					zk.delete(path, -1);
				}

				ci.println("Removed " + path);
			}
		});
		zkCommands.put("get", new Command("path") {
			@Override
			public void execute(final ZooKeeper zk, final CommandInterpreter ci) throws Exception {
				final String path = ci.nextArgument();
				if (path == null) {
					printInvalidArgs(ci);
					return;
				}

				byte data[] = zk.getData(path, false, null);
				data = (data == null) ? "null".getBytes() : data;
				ci.println(new String(data));
			}
		});
		zkCommands.put("set", new Command("path data") {
			@Override
			public void execute(final ZooKeeper zk, final CommandInterpreter ci) throws Exception {
				final String path = ci.nextArgument();
				final String data = ci.nextArgument();
				if ((path == null) || (data == null)) {
					printInvalidArgs(ci);
					return;
				}

				zk.setData(path, data.getBytes(), -1);
				ci.println("Updated " + path);
			}
		});
		zkCommands.put("stat", new Command("path") {
			@Override
			public void execute(final ZooKeeper zk, final CommandInterpreter ci) throws Exception {
				final String path = ci.nextArgument();
				if (path == null) {
					printInvalidArgs(ci);
					return;
				}

				final Stat stat = zk.exists(path, false);
				ci.println("cZxid: 0x" + Long.toHexString(stat.getCzxid()));
				ci.println("ctime: " + new Date(stat.getCtime()).toString());
				ci.println("mZxid: 0x" + Long.toHexString(stat.getMzxid()));
				ci.println("mtime: " + new Date(stat.getMtime()).toString());
				ci.println("pZxid: 0x" + Long.toHexString(stat.getPzxid()));
				ci.println("cversion: " + stat.getCversion());
				ci.println("dataVersion: " + stat.getVersion());
				ci.println("aclVersion: " + stat.getAversion());
				ci.println("ephemeralOwner: 0x" + Long.toHexString(stat.getEphemeralOwner()));
				ci.println("dataLength: " + stat.getDataLength());
				ci.println("numChildren: " + stat.getNumChildren());
			}
		});
		zkCommands.put("sync", new Command("path") {
			@Override
			public void execute(final ZooKeeper zk, final CommandInterpreter ci) throws Exception {
				final String path = ci.nextArgument();
				if (path == null) {
					printInvalidArgs(ci);
					return;
				}

				zk.sync(path, new VoidCallback() {
					@Override
					public void processResult(final int rc, final String path, final Object ctx) {
						((CommandInterpreter) ctx).println("Synched " + path + " (rc:" + rc + ")");
					}
				}, ci);
			}
		});
	}

	static void _zkHelp(final CommandInterpreter ci) {
		ci.println("zk <cmd> [args]");
		for (final String cmd : zkCommands.keySet()) {
			ci.println("\t" + cmd + " " + zkCommands.get(cmd).getHelp());
		}
	}

	public void _zk(final CommandInterpreter ci) throws Exception {
		final String command = ci.nextArgument();
		if (command == null) {
			_zkHelp(ci);
			return;
		}

		final Command cmd = zkCommands.get(command);
		if (cmd == null) {
			ci.println("ERROR: unknown ZooKeeper command " + command);
			_zkHelp(ci);
			return;
		}

		ZooKeeper keeper = null;
		try {
			keeper = ZooKeeperGate.get().ensureConnected();
		} catch (final Exception e) {
			ci.println("ZooKeeper not connected! " + e.getMessage());
			return;
		}
		cmd.execute(keeper, ci);
	}

	public void _zkrm(final CommandInterpreter ci) throws Exception {
		final String pathStr = ci.nextArgument();
		if (pathStr == null) {
			throw new IllegalArgumentException("path required");
		}

		final IPath path = new Path(pathStr);
		ZooKeeperGate.get().deletePath(path);
		ci.println("deleted " + path);
	}

	@Override
	public String getHelp() {
		final StrBuilder help = new StrBuilder(512);
		help.appendln("---ZooKeeper Commands---");
		help.appendln("\tzk <cmd> [args]");
		for (final String cmd : zkCommands.keySet()) {
			help.appendln("\t\t" + cmd + " " + zkCommands.get(cmd).getHelp());
		}
		return help.toString();
	}

}
