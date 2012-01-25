/*******************************************************************************
 * Copyright (c) 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.internal.console;

import org.eclipse.gyrex.common.console.BaseCommandProvider;
import org.eclipse.gyrex.persistence.internal.PersistenceDebug;

import org.eclipse.osgi.framework.console.CommandInterpreter;

/**
 * Console commands for managing repositories.
 */
public class RepositoryConsoleCommands extends BaseCommandProvider {

	/**
	 * Creates a new instance.
	 */
	public RepositoryConsoleCommands() {
		registerCommand("ls", LsRepos.class);
		registerCommand("create", CreateRepo.class);
		registerCommand("config", ConfigRepo.class);
		registerCommand("providers", LsProviders.class);
	}

	public void _repos(final CommandInterpreter ci) throws Exception {
		printStackTraces = PersistenceDebug.debug;
		execute(ci);
	}

	@Override
	protected String getCommandName() {
		return "repos";
	}

}
