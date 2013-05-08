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

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.eclipse.gyrex.cloud.admin.ICloudManager;
import org.eclipse.gyrex.cloud.admin.INodeDescriptor;
import org.eclipse.gyrex.cloud.internal.CloudActivator;
import org.eclipse.gyrex.common.console.Command;

import org.apache.commons.lang.StringUtils;

import org.kohsuke.args4j.Argument;

public class ListNodesCmd extends Command {

	private static final String APPROVED = "approved";
	private static final String PENDING = "pending";
	private static final String ONLINE = "online";

	static boolean matchesFilter(final String filter, final INodeDescriptor node) {
		return StringUtils.isBlank(filter) || StringUtils.containsIgnoreCase(node.getId(), filter) || StringUtils.containsIgnoreCase(node.getName(), filter) || StringUtils.containsIgnoreCase(node.getLocation(), filter);
	}

	@Argument(index = 0, metaVar = "pending|approved|online", usage = "specify the type of nodes to list")
	String what;

	@Argument(index = 1, metaVar = "FILTER", usage = "an optional filter string")
	String filter;

	/**
	 * Creates a new instance.
	 */
	public ListNodesCmd() {
		super("pending|approved|online - list nodes");
	}

	private void collectedNodes(final Map<String, INodeDescriptor> nodes, final Collection<INodeDescriptor> nodeDescriptors) {
		for (final INodeDescriptor nodeDescriptor : nodeDescriptors) {
			nodes.put(nodeDescriptor.getId(), nodeDescriptor);
		}
	}

	@Override
	protected void doExecute() throws Exception {
		final ICloudManager cloudManager = CloudActivator.getInstance().getService(ICloudManager.class);

		// collect all nodes
		final Collection<String> onlineNodes = cloudManager.getOnlineNodes();
		final Map<String, INodeDescriptor> nodes = new HashMap<String, INodeDescriptor>();
		collectedNodes(nodes, cloudManager.getApprovedNodes());
		collectedNodes(nodes, cloudManager.getPendingNodes());

		// build result list
		final Collection<INodeDescriptor> result = new TreeSet<INodeDescriptor>(new Comparator<INodeDescriptor>() {
			@Override
			public int compare(final INodeDescriptor o1, final INodeDescriptor o2) {
				return o1.getId().compareTo(o2.getId());
			}
		});

		if (StringUtils.isBlank(what)) {
			result.addAll(nodes.values());
		} else if (StringUtils.startsWithIgnoreCase(APPROVED, what)) {
			for (final INodeDescriptor node : nodes.values()) {
				if (node.isApproved()) {
					result.add(node);
				}
			}
		} else if (StringUtils.startsWithIgnoreCase(PENDING, what)) {
			for (final INodeDescriptor node : nodes.values()) {
				if (!node.isApproved()) {
					result.add(node);
				}
			}
		} else if (StringUtils.startsWith(ONLINE, what)) {
			for (final String onlineNode : onlineNodes) {
				final INodeDescriptor node = nodes.get(onlineNode);
				if (null != node) {
					result.add(node);
				}
			}
		}

		for (final INodeDescriptor node : result) {
			if (matchesFilter(filter, node)) {
				final String name = StringUtils.isNotBlank(node.getName()) ? node.getName() : "<unnamed>";
				final String location = StringUtils.isNotBlank(node.getLocation()) ? node.getLocation() : "<unknown>";
				final String tags = StringUtils.join(node.getTags(), ", ");
				final String approved = node.isApproved() ? "approved" : "not approved";
				if (onlineNodes.contains(node.getId())) {
					printf("%s - %s @ %s [%s, online] %s", node.getId(), name, location, approved, tags);
				} else {
					printf("%s - %s @ %s [%s] %s", node.getId(), name, location, approved, tags);
				}
			}
		}
	}
}
