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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.gyrex.cloud.admin.ICloudManager;
import org.eclipse.gyrex.cloud.admin.INodeConfigurer;
import org.eclipse.gyrex.cloud.admin.INodeDescriptor;
import org.eclipse.gyrex.cloud.internal.CloudActivator;
import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperNodeInfo;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * {@link ICloudManager} implementation.
 */
public class CloudManagerImpl implements ICloudManager {

	@Override
	public IStatus approveNode(final String nodeId) {
		try {
			ZooKeeperNodeInfo.approve(nodeId, null, null);
			return Status.OK_STATUS;
		} catch (final Exception e) {
			return new Status(IStatus.ERROR, CloudActivator.SYMBOLIC_NAME, "Error approving node. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	@Override
	public Collection<INodeDescriptor> getApprovedNodes() {
		return readNodes(true);
	}

	@Override
	public INodeConfigurer getNodeConfigurer(final String nodeId) {
		return new NodeConfigurer(nodeId);
	}

	@Override
	public Collection<INodeDescriptor> getPendingNodes() {
		return readNodes(false);
	}

	private Collection<INodeDescriptor> readNodes(final boolean approved) {
		try {
			final IPath path = approved ? IZooKeeperLayout.PATH_NODES_APPROVED : IZooKeeperLayout.PATH_NODES_PENDING;
			final ZooKeeperGate zk = ZooKeeperGate.get();
			final Collection<String> names = zk.readChildrenNames(path, null);
			final List<INodeDescriptor> nodes = new ArrayList<INodeDescriptor>(names.size());
			for (final String nodeId : names) {
				nodes.add(new NodeDescriptor(nodeId, approved));
			}
			return Collections.unmodifiableCollection(nodes);
		} catch (final Exception e) {
			throw new RuntimeException("Error reading list of nodes. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	@Override
	public IStatus retireNode(final String nodeId) {
		try {
			final ZooKeeperGate zk = ZooKeeperGate.get();

			zk.deletePath(IZooKeeperLayout.PATH_NODES_APPROVED.append(nodeId));

			return Status.OK_STATUS;
		} catch (final Exception e) {
			return new Status(IStatus.ERROR, CloudActivator.SYMBOLIC_NAME, "Error approving node. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}
}
