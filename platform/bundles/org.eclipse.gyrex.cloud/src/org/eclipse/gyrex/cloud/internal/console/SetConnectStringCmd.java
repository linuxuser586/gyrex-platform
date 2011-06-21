/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal.console;

import org.eclipse.gyrex.cloud.admin.ICloudManager;
import org.eclipse.gyrex.cloud.internal.CloudActivator;
import org.eclipse.gyrex.cloud.internal.NodeInfo;
import org.eclipse.gyrex.common.console.Command;

import org.eclipse.core.runtime.IStatus;

import org.kohsuke.args4j.Argument;

public class SetConnectStringCmd extends Command {

	@Argument(index = 0, metaVar = "CONNECTSTRING", usage = "specify the connect string for the local node", required = true)
	String connectString;

	/**
	 * Creates a new instance.
	 */
	public SetConnectStringCmd() {
		super("<CONNECTSTRING> - sets the connect string for the local node");
	}

	@Override
	protected void doExecute() throws Exception {
		final ICloudManager cloudManager = CloudActivator.getInstance().getService(ICloudManager.class);

		final IStatus status = cloudManager.getNodeConfigurer(new NodeInfo().getNodeId()).configureConnection(connectString);

		if (status.isOK()) {
			printf("Node cloud connection updated to %s!", connectString);
		} else {
			printf(status.getMessage());
		}
	}
}
