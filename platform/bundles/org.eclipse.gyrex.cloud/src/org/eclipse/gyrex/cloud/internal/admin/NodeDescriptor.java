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
package org.eclipse.gyrex.cloud.internal.admin;

import java.util.Collections;
import java.util.List;

import org.eclipse.gyrex.cloud.admin.INodeDescriptor;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperNodeInfo;

/**
 * {@link INodeDescriptor} implementation which supports dynamic/lazy loading.
 */
public class NodeDescriptor implements INodeDescriptor {

	private final String nodeId;
	private ZooKeeperNodeInfo info;
	private final boolean approved;

	/**
	 * Creates a new instance.
	 * 
	 * @param nodeId
	 */
	public NodeDescriptor(final String nodeId, final boolean approved) {
		this.nodeId = nodeId;
		this.approved = approved;
	}

	private ZooKeeperNodeInfo ensureInfo() {
		if (info != null) {
			return info;
		}

		info = ZooKeeperNodeInfo.load(nodeId, approved);
		if (info == null) {
			throw new IllegalStateException("node info not available!");
		}

		return info;
	}

	@Override
	public String getId() {
		return nodeId;
	}

	@Override
	public String getLocation() {
		final String location = ensureInfo().getLocation();
		if (location == null) {
			return "unknown";
		}
		return location;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.cloud.admin.INodeDescriptor#getName()
	 */
	@Override
	public String getName() {
		final String name = ensureInfo().getName();
		if (name == null) {
			return "";
		}
		return name;
	}

	@Override
	public List<String> getRoles() {
		final List<String> roles = ensureInfo().getRoles();
		if (roles == null) {
			return Collections.emptyList();
		}
		return roles;
	}

	@Override
	public boolean isApproved() {
		return approved;
	}
}
