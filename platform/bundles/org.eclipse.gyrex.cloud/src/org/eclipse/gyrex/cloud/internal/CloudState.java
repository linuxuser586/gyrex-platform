/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate.IConnectionMonitor;

import org.eclipse.osgi.util.NLS;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Captures general state of the cloud.
 */
public class CloudState implements IConnectionMonitor {

	private static final Logger LOG = LoggerFactory.getLogger(CloudState.class);

	private static final AtomicReference<CloudState> instanceRef = new AtomicReference<CloudState>();

	/**
	 * Registers this node with the cloud.
	 */
	public static void registerNode() throws Exception {
		if (!instanceRef.compareAndSet(null, new CloudState())) {
			throw new IllegalStateException("Already registered!");
		}

		// hook with ZooKeeperGate
		ZooKeeperGate.addConnectionMonitor(instanceRef.get());
	}

	/**
	 * Unregisters the node from the cloud.
	 */
	public static void unregisterNode() {
		final CloudState cloudState = instanceRef.getAndSet(null);
		if (cloudState == null) {
			throw new IllegalStateException("Not registered!");
		}

		// remove from gate
		ZooKeeperGate.removeConnectionMonitor(cloudState);
	}

	private final AtomicReference<NodeInfo> myInfo = new AtomicReference<NodeInfo>();

	/**
	 * Creates a new instance.
	 */
	private CloudState() {
	}

	@Override
	public void connected() {
		// TODO: this is wrong, the flow needs to change
		// someone should trigger a ZooKeeper connection
		// the ZooKeeper connection would trigger this when connected successfully

		// load / create our node info
		NodeInfo nodeInfo = null;
		try {
			nodeInfo = initializeNodeInfo();
			LOG.info("Node {} initialized.", nodeInfo);
		} catch (final Exception e) {
			LOG.error("Unable to initialize node info. Node will not be available in cloud.", e);
			return;
		}

		// set node online
		if (myInfo.compareAndSet(null, nodeInfo)) {
			try {
				if (nodeInfo.isApproved()) {
					setNodeOnline(nodeInfo);
					LOG.info("Node {} is now online.", nodeInfo);
				} else {
					LOG.info("Node {} needs to be approved first. Please contact cloud administrator with node details for approval.", nodeInfo);
				}
			} catch (final Exception e) {
				LOG.error("Unable to set node to online. Node will not be available in cloud.", e);
				return;
			}
		}
	}

	@Override
	public void disconnected() {
		final NodeInfo nodeInfo = myInfo.getAndSet(null);
		if (nodeInfo != null) {
			// we may be already disconnected but we try to remove the ephemeral node anyway
			try {
				setNodeOffline(nodeInfo);
				LOG.info("Node {} is now offline.", nodeInfo);
			} catch (final IllegalStateException e) {
				// ok, gate is gone
			} catch (final Exception e) {
				// this may happen (eg. connection lost unintentionally), but log a warning
				LOG.warn("Unable to set node to offline. You may need to check node directory manually. {}", e.toString());
			}
		}
	}

	/**
	 * Initializes the node information.
	 * 
	 * @param gate
	 */
	private NodeInfo initializeNodeInfo() throws Exception {
		final NodeInfo info = new NodeInfo();

		// check if there is a recored in the "approved" list
		final byte[] record = ZooKeeperGate.get().readRecord(IZooKeeperLayout.PATH_NODES_APPROVED.append(info.getNodeId()), null);
		if (record != null) {
			return new NodeInfo(record);
		}

		// create an ephemeral pending record
		ZooKeeperGate.get().writeRecord(IZooKeeperLayout.PATH_NODES_PENDING.append(info.getNodeId()), CreateMode.EPHEMERAL, info.getLocation());

		return info;
	}

	private void setNodeOffline(final NodeInfo node) throws Exception {
		// remove an ephemeral record for this node
		try {
			ZooKeeperGate.get().deletePath(IZooKeeperLayout.PATH_NODES_ONLINE.append(node.getNodeId()));
		} catch (final KeeperException e) {
			// if the node already exists something is off
			if (e.code() != KeeperException.Code.NONODE) {
				throw e;
			}
		}
	}

	private void setNodeOnline(final NodeInfo node) throws Exception {
		if (!node.isApproved()) {
			throw new IllegalArgumentException(NLS.bind("Node {0} must be approved first before it is allowed to join the cloud.", node.getNodeId()));
		}

		// ensure the persistent record containing all online nodes exist
		try {
			ZooKeeperGate.get().createPath(IZooKeeperLayout.PATH_NODES_ONLINE, CreateMode.PERSISTENT);
		} catch (final KeeperException e) {
			// its okay if the node already exists
			if (e.code() != KeeperException.Code.NODEEXISTS) {
				throw e;
			}
		}

		// create an ephemeral record for this node
		try {
			ZooKeeperGate.get().writeRecord(IZooKeeperLayout.PATH_NODES_ONLINE.append(node.getNodeId()), CreateMode.EPHEMERAL, node.getLocation());
		} catch (final KeeperException e) {
			// if the node already exists something is off
			if (e.code() == KeeperException.Code.NODEEXISTS) {
				// TODO investigate this error further
				throw new IllegalStateException(NLS.bind("Unable to register node {0} in the cloud. There is already a node with that id running.", node.getNodeId()), e);
			} else {
				throw e;
			}
		}
	}
}
