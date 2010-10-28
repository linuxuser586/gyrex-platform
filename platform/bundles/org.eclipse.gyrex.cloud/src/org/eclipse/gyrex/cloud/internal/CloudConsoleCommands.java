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

import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import org.apache.commons.lang.text.StrBuilder;

/**
 *
 */
public class CloudConsoleCommands implements CommandProvider {

	public void _zkls(final CommandInterpreter ci) throws Exception {
		final StrBuilder string = new StrBuilder(1024 * 1024);
		ZooKeeperGate.get().dumpTree("/", 0, string);
		ci.println(string);
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
		help.appendln("---Gyrex Cloud Commands---");
		help.appendln("\tzkls <path> - Lists ZooKeeper layout at <path>");
		help.appendln("\tzkrm <path> - Removes ZooKeeper <path> (recursively)");
		return help.toString();
	}

}
