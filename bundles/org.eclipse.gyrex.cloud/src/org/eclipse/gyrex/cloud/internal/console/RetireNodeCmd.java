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

import org.eclipse.gyrex.cloud.admin.ICloudManager;
import org.eclipse.gyrex.cloud.internal.CloudActivator;
import org.eclipse.gyrex.common.console.Command;

import org.eclipse.core.runtime.IStatus;

import org.kohsuke.args4j.Argument;

public class RetireNodeCmd extends Command {

	@Argument(index = 0, metaVar = "NODEID", usage = "specify the node id", required = true)
	String nodeId;

	/**
	 * Creates a new instance.
	 */
	public RetireNodeCmd() {
		super("<NODEID> - retires a node");
	}

	@Override
	protected void doExecute() throws Exception {
		final ICloudManager cloudManager = CloudActivator.getInstance().getService(ICloudManager.class);

		final IStatus status = cloudManager.retireNode(nodeId);

		if (status.isOK()) {
			printf("Node %s retired!", nodeId);
		} else {
			printf(status.getMessage());
		}
	}
}
