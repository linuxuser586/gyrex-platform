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
package org.eclipse.gyrex.cloud.internal.queue.console;

import org.eclipse.gyrex.cloud.internal.CloudActivator;
import org.eclipse.gyrex.cloud.internal.queue.ZooKeeperQueueService;
import org.eclipse.gyrex.cloud.services.queue.IQueueService;
import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.common.identifiers.IdHelper;

import org.kohsuke.args4j.Argument;

public class RemoveQueueCmd extends Command {

	@Argument(index = 0, metaVar = "QUEUEID", usage = "id of the queue")
	String queueId;

	/**
	 * Creates a new instance.
	 */
	public RemoveQueueCmd() {
		super("<QUEUEID> - removes a queue");
	}

	@Override
	protected void doExecute() throws Exception {
		if (!IdHelper.isValidId(queueId)) {
			printf("ERROR: Invalid queue id!");
			return;
		}

		getQueueService().deleteQueue(queueId, null);
	}

	private ZooKeeperQueueService getQueueService() {
		return (ZooKeeperQueueService) CloudActivator.getInstance().getService(IQueueService.class);
	}
}
