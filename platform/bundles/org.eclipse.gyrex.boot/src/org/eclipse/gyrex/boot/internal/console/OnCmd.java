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

import org.eclipse.gyrex.common.console.Command;

import org.eclipse.osgi.service.debug.DebugOptions;

public class OnCmd extends Command {
	/**
	 * Creates a new instance.
	 */
	public OnCmd() {
		super("enables debug output on this node");
	}

	@Override
	protected void doExecute() throws Exception {
		final DebugOptions debugOptions = DebugConsoleCommands.getDebugOptions();
		debugOptions.setDebugEnabled(true);
		printf("Enabled debug output!");
	}
}