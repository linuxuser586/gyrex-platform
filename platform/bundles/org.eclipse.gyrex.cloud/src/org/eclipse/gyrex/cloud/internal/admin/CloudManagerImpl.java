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
import org.eclipse.gyrex.cloud.admin.INodeListener;
import org.eclipse.gyrex.cloud.environment.INodeEnvironment;
import org.eclipse.gyrex.cloud.internal.CloudActivator;
import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperMonitor;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperNodeInfo;
import org.eclipse.gyrex.common.services.IServiceProxy;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ICloudManager} implementation.
 */
public class CloudManagerImpl implements ICloudManager {

	private static final Logger LOG = LoggerFactory.getLogger(CloudManagerImpl.class);

	private IServiceProxy<INodeEnvironment> nodeEnvironmentService;
	private ListenerList listenerList;

	private final ZooKeeperMonitor nodesMonitor = new ZooKeeperMonitor() {
		@Override
		protected void childrenChanged(final String path) {
			fireNodesChanged();
			installNodesMonitor(new Path(path));
		}

		@Override
		protected void pathCreated(final String path) {
			fireNodesChanged();
			installNodesMonitor(new Path(path));
		};

		@Override
		protected void pathDeleted(final String path) {
			fireNodesChanged();
			installNodesMonitor(new Path(path));
		};
	};

	@Override
	public void addNodeListener(final INodeListener nodeListener) {
		if (listenerList == null) {
			listenerList = new ListenerList();
			installNodesMonitor(IZooKeeperLayout.PATH_NODES_PENDING);
			installNodesMonitor(IZooKeeperLayout.PATH_NODES_APPROVED);
			installNodesMonitor(IZooKeeperLayout.PATH_NODES_ONLINE);
		}
		listenerList.add(nodeListener);
	}

	@Override
	public IStatus approveNode(final String nodeId) {
		try {
			ZooKeeperNodeInfo.approve(nodeId, null, null);
			return Status.OK_STATUS;
		} catch (final Exception e) {
			return new Status(IStatus.ERROR, CloudActivator.SYMBOLIC_NAME, "Error approving node. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	};

	void fireNodesChanged() {
		final Object[] listeners = listenerList.getListeners();
		for (final Object listener : listeners) {
			((INodeListener) listener).nodesChanged();
		}
	}

	@Override
	public Collection<INodeDescriptor> getApprovedNodes() {
		return readNodes(true);
	}

	@Override
	public INodeEnvironment getLocalInfo() {
		if (nodeEnvironmentService == null) {
			nodeEnvironmentService = CloudActivator.getInstance().getServiceHelper().trackService(INodeEnvironment.class);
		}
		return nodeEnvironmentService.getService();
	}

	@Override
	public INodeConfigurer getNodeConfigurer(final String nodeId) {
		return new NodeConfigurer(nodeId);
	}

	@Override
	public Collection<String> getOnlineNodes() {
		try {
			final ZooKeeperGate zk = ZooKeeperGate.get();
			final Collection<String> names = zk.readChildrenNames(IZooKeeperLayout.PATH_NODES_ONLINE, null);
			if (names == null) {
				return Collections.emptyList();
			}
			return names;
		} catch (final NoNodeException e) {
			return Collections.emptyList();
		} catch (final Exception e) {
			throw new RuntimeException("Error reading list of nodes. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	@Override
	public Collection<INodeDescriptor> getPendingNodes() {
		return readNodes(false);
	}

	private void installNodesMonitor(final IPath nodesPath) {
		try {
			final ZooKeeperGate zk = ZooKeeperGate.get();
			if (zk.exists(nodesPath, nodesMonitor)) {
				zk.readChildrenNames(nodesPath, nodesMonitor, null);
			}
		} catch (final Exception e) {
			LOG.warn("Unable to install node monitor at node {}. {}", nodesPath, ExceptionUtils.getRootCauseMessage(e));
		}
	}

	private Collection<INodeDescriptor> readNodes(final boolean approved) {
		try {
			final IPath path = approved ? IZooKeeperLayout.PATH_NODES_APPROVED : IZooKeeperLayout.PATH_NODES_PENDING;
			final ZooKeeperGate zk = ZooKeeperGate.get();
			final Collection<String> names = zk.readChildrenNames(path, null);
			if (names == null) {
				return Collections.emptyList();
			}
			final List<INodeDescriptor> nodes = new ArrayList<INodeDescriptor>(names.size());
			for (final String nodeId : names) {
				nodes.add(new NodeDescriptor(nodeId, approved));
			}
			return Collections.unmodifiableCollection(nodes);
		} catch (final NoNodeException e) {
			return Collections.emptyList();
		} catch (final Exception e) {
			throw new RuntimeException("Error reading list of nodes. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	@Override
	public void removeNodeListener(final INodeListener nodeListener) {
		listenerList.remove(nodeListener);
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
