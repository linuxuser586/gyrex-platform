/*******************************************************************************
 * Copyright (c) 2010 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal;

import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import org.apache.commons.lang.text.StrBuilder;

/**
 *
 */
public class CloudConsoleCommands implements CommandProvider {

	public void _zkDump(final CommandInterpreter ci) throws Exception {
		final StrBuilder string = new StrBuilder(1024 * 1024);
		ZooKeeperGate.get().dumpTree("/", 0, string);
		ci.println(string);
	}

	@Override
	public String getHelp() {
		final StrBuilder help = new StrBuilder(512);
		help.appendln("---Gyrex Cloud Commands---");
		help.appendln("\tzkDump - Dumps the current ZooKeeper layout");
		return help.toString();
	}

}
