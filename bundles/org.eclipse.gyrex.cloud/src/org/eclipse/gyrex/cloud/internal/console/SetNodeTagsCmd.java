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

import java.util.HashSet;
import java.util.List;

import org.eclipse.gyrex.cloud.admin.ICloudManager;
import org.eclipse.gyrex.cloud.internal.CloudActivator;
import org.eclipse.gyrex.common.console.Command;

import org.eclipse.core.runtime.IStatus;

import org.apache.commons.lang.StringUtils;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public class SetNodeTagsCmd extends Command {

	@Argument(index = 0, metaVar = "NODEID", usage = "specify the node id", required = true)
	String nodeId;

	@Option(name = "-t", aliases = { "--tag" }, usage = "specify a tag")
	List<String> tags;

	/**
	 * Creates a new instance.
	 */
	public SetNodeTagsCmd() {
		super("<nodeId> [-t <tag> [-t <tag>]] - set tags for a node");
	}

	@Override
	protected void doExecute() throws Exception {
		final ICloudManager cloudManager = CloudActivator.getInstance().getService(ICloudManager.class);

		final HashSet<String> consolidatedTags = null != tags ? new HashSet<String>(tags) : null;
		final IStatus status = cloudManager.getNodeConfigurer(nodeId).setTags(consolidatedTags);

		if (status.isOK()) {
			if (null != consolidatedTags) {
				printf("Tags of node %s updated to %s!", nodeId, StringUtils.join(consolidatedTags, ','));
			} else {
				printf("Tags of node %s cleared!", nodeId);
			}
		} else {
			printf(status.getMessage());
		}
	}
}
