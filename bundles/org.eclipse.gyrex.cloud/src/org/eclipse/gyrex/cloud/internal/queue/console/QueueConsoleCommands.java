/*******************************************************************************
 * Copyright (c) 2010, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal.queue.console;

import org.eclipse.gyrex.cloud.internal.CloudDebug;
import org.eclipse.gyrex.common.console.BaseCommandProvider;

import org.eclipse.osgi.framework.console.CommandInterpreter;

public class QueueConsoleCommands extends BaseCommandProvider {

	/**
	 * Creates a new instance.
	 */
	public QueueConsoleCommands() {
		registerCommand("ls", ListQueuesCmd.class);

		registerCommand("create", CreateQueueCmd.class);
		registerCommand("rm", RemoveQueueCmd.class);
	}

	public void _queue(final CommandInterpreter ci) {
		printStackTraces = CloudDebug.debug;
		execute(ci);
	}

	@Override
	protected String getCommandName() {
		return "queue";
	}

}
