/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal.console;

import org.eclipse.gyrex.cloud.internal.CloudState;
import org.eclipse.gyrex.common.console.Command;

public class SetOfflineCmd extends Command {

	/**
	 * Creates a new instance.
	 */
	public SetOfflineCmd() {
		super("sets the current node offline");
	}

	@Override
	protected void doExecute() throws Exception {
		CloudState.unregisterNode();
		printf("Node un-registered from cloud!");
	}
}
