/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.boot.internal.console;

import java.util.Map;
import java.util.TreeSet;

import org.eclipse.gyrex.common.console.Command;

import org.eclipse.osgi.service.debug.DebugOptions;

public class ListCmd extends Command {
	/**
	 * Creates a new instance.
	 */
	public ListCmd() {
		super("prints all current debug options");
	}

	@Override
	protected void doExecute() throws Exception {
		final DebugOptions debugOptions = DebugConsoleCommands.getDebugOptions();
		if (debugOptions.isDebugEnabled()) {
			printf("Traceing is currently enabled.");
		} else {
			printf("Traceing is not enabled.");
		}

		final Map<String, String> options = debugOptions.getOptions();
		final TreeSet<String> keys = new TreeSet<String>(options.keySet());
		if (!keys.isEmpty()) {
			printf("The following debug options are set:");
			for (final String key : keys) {
				printf("  %s = %s", key, options.get(key));
			}
		} else {
			printf("No debug options set.");
		}
	}
}