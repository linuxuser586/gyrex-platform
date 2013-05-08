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

import java.util.Collection;
import java.util.Date;
import java.util.TreeSet;

import org.eclipse.gyrex.cloud.internal.CloudActivator;
import org.eclipse.gyrex.cloud.internal.queue.Message;
import org.eclipse.gyrex.cloud.internal.queue.ZooKeeperQueue;
import org.eclipse.gyrex.cloud.internal.queue.ZooKeeperQueueService;
import org.eclipse.gyrex.cloud.services.queue.IQueueService;
import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.common.identifiers.IdHelper;

import org.apache.commons.lang.StringUtils;

import org.kohsuke.args4j.Argument;

public class ListQueuesCmd extends Command {

	@Argument(index = 0, metaVar = "FILTER", usage = "an optional filter string")
	String filter;

	/**
	 * Creates a new instance.
	 */
	public ListQueuesCmd() {
		super("[FILTER] - list queues");
	}

	@Override
	protected void doExecute() throws Exception {
		final ZooKeeperQueueService queueService = getQueueService();

		// collect (and sort) all queues
		final Collection<String> queueIds = new TreeSet<String>(queueService.getQueues());

		// check for direct match
		if (IdHelper.isValidId(filter) && queueIds.contains(filter)) {
			printQueueDetails(filter);
			return;
		}

		for (final String queueId : queueIds) {
			if (StringUtils.isBlank(filter) || StringUtils.containsIgnoreCase(queueId, filter)) {
				final ZooKeeperQueue queue = queueService.getQueue(queueId, null);
				if (null != queue) {
					printf("%s (%d)", queueId, queue.size());
				}
			}
		}
	}

	private ZooKeeperQueueService getQueueService() {
		return (ZooKeeperQueueService) CloudActivator.getInstance().getService(IQueueService.class);
	}

	private void printQueueDetails(final String queueId) {
		final ZooKeeperQueue queue = getQueueService().getQueue(queueId, null);
		if (null == queue) {
			printf("Queue '%s' has been removed!", queueId);
			return;
		}

		printf("%s", queueId);

		final Collection<Message> messages = queue.getMessages();
		if (!messages.isEmpty()) {
			for (final Message message : messages) {
				if (message.isHidden()) {
					printf("  %s (hidden till %tc)", message.getMessageId(), new Date(message.getInvisibleTimeoutTS()));
				} else {
					printf("  %s (%d bytes)", message.getMessageId(), message.getBody().length);
				}
			}
		} else {
			printf("  (empty)");
		}
	}
}
