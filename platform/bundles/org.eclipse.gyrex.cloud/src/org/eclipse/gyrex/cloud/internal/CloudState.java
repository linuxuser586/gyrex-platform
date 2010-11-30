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

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate.IConnectionMonitor;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGateApplication;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperMonitor;

import org.eclipse.osgi.util.NLS;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
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
	private final ZooKeeperMonitor monitor = new ZooKeeperMonitor() {
		@Override
		protected void pathCreated(final String path) {
			// check if active
			final NodeInfo nodeInfo = myInfo.get();
			if (nodeInfo == null) {
				return;
			}

			// process node activated events
			if (path.equals(IZooKeeperLayout.PATH_NODES_APPROVED.append(nodeInfo.getNodeId()).toString())) {
				// force re-connect
				unregisterNode();
				try {
					registerNode();
				} catch (final Exception e) {
					LOG.error("Failed to re-register node. Node {} will not be available in cloud. {}", new Object[] { nodeInfo, e.getMessage(), e });
				}
			}
		};

		@Override
		protected void pathDeleted(final String path) {
			// check if active
			final NodeInfo nodeInfo = myInfo.get();
			if (nodeInfo == null) {
				return;
			}

			// process node de-activation events
			if (path.equals(IZooKeeperLayout.PATH_NODES_APPROVED.append(nodeInfo.getNodeId()).toString())) {
				// force re-connect
				unregisterNode();
				try {
					registerNode();
				} catch (final Exception e) {
					LOG.error("Failed to re-register node. Node {} will not be available in cloud. {}", new Object[] { nodeInfo, e.getMessage(), e });
				}
			}
		};

		@Override
		protected void recordChanged(final String path) {
			// check if active
			final NodeInfo nodeInfo = myInfo.get();
			if (nodeInfo == null) {
				return;
			}

			// process node de-activation events
			if (path.equals(IZooKeeperLayout.PATH_NODES_APPROVED.append(nodeInfo.getNodeId()).toString())) {
				try {
					updateNodeInfo(nodeInfo);
				} catch (final Exception e) {
					LOG.error("Failed to update node state. Node {} might be dysfunctional in cloud. {}", new Object[] { nodeInfo, e.getMessage(), e });
				}
			}
		}
	};

	/**
	 * Creates a new instance.
	 */
	private CloudState() {
	}

	private void activateRole(final String role) {
		// TODO Auto-generated method stub

	}

	@Override
	public void connected() {
		// load / create our node info
		NodeInfo nodeInfo = null;
		try {
			nodeInfo = initializeNodeInfo();
			LOG.info("Node {} initialized.", nodeInfo);
		} catch (final Exception e) {
			// log error
			LOG.error("Unable to initialize node info. Node will not be available in cloud. {}", new Object[] { e.getMessage(), e });

			// bring down gate
			ZooKeeperGateApplication.forceShutdown();
			return;
		}

		// set node online
		if (myInfo.compareAndSet(null, nodeInfo)) {
			// synchronize in order to prevent concurrent updates
			synchronized (myInfo) {
				if (myInfo.get() == nodeInfo) {
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
		}
	}

	private void deactivateRole(final String role) {
		// TODO Auto-generated method stub

	}

	@Override
	public void disconnected() {
		final NodeInfo nodeInfo = myInfo.getAndSet(null);
		if (nodeInfo != null) {
			// synchronize in order to prevent concurrent updates
			synchronized (myInfo) {
				if (myInfo.get() == null) {
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
		}
	}

	/**
	 * Initializes the node information.
	 */
	private NodeInfo initializeNodeInfo() throws Exception {
		final NodeInfo info = new NodeInfo();

		// check for node id clashes by creating an ephemeral "all" record
		try {
			ZooKeeperGate.get().createPath(IZooKeeperLayout.PATH_NODES_ALL.append(info.getNodeId()), CreateMode.EPHEMERAL, info.getLocation());
		} catch (final KeeperException e) {
			if (e.code() == Code.NODEEXISTS) {
				final String existingEntry = ZooKeeperGate.get().readRecord(IZooKeeperLayout.PATH_NODES_ALL.append(info.getNodeId()), (String) null, null);
				throw new IllegalStateException(NLS.bind("Node id {0} already in use by {1}!", info.getNodeId(), existingEntry));
			}
		}

		// check if there is a recored in the "approved" list
		final byte[] record = ZooKeeperGate.get().readRecord(IZooKeeperLayout.PATH_NODES_APPROVED.append(info.getNodeId()), monitor, null);
		if (record != null) {
			return new NodeInfo(record);
		}

		// create an ephemeral pending record
		ZooKeeperGate.get().writeRecord(IZooKeeperLayout.PATH_NODES_PENDING.append(info.getNodeId()), CreateMode.EPHEMERAL, info.getLocation());

		return info;
	}

	private void setNodeOffline(final NodeInfo node) throws Exception {
		// de-activate cloud roles
		for (final String role : node.getRoles()) {
			deactivateRole(role);
		}

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

		// TODO setup node metrics publisher (cpu load, memory resources)

		// activate cloud roles
		for (final String role : node.getRoles()) {
			activateRole(role);
		}
	}

	/**
	 * Smart update handling of the node info, i.e. de-activation of lost server
	 * roles, activation of new roles, etc.
	 * 
	 * @param oldNodeInfo
	 * @throws Exception
	 */
	void updateNodeInfo(final NodeInfo oldNodeInfo) throws Exception {
		if (oldNodeInfo == null) {
			throw new IllegalArgumentException("old node info must not be null");
		}
		// read new "approved" record
		final byte[] record = ZooKeeperGate.get().readRecord(IZooKeeperLayout.PATH_NODES_APPROVED.append(oldNodeInfo.getNodeId()), monitor, null);
		if (record == null) {
			return;
		}

		// create new node info
		final NodeInfo nodeInfo = new NodeInfo(record);

		// ignore updates from other thread
		if (!myInfo.compareAndSet(oldNodeInfo, nodeInfo)) {
			return;
		}

		// synchronize in order to prevent concurrent "smart" updates
		synchronized (myInfo) {
			if (myInfo.get() != nodeInfo) {
				return;
			}

			final Collection<String> newRoles = nodeInfo.getRoles();
			final Collection<String> oldRoles = oldNodeInfo.getRoles();

			// deactivate old roles
			for (final String role : oldRoles) {
				if (!newRoles.contains(role)) {
					deactivateRole(role);
				}
			}

			// activate new roles
			for (final String role : newRoles) {
				if (!oldRoles.contains(role)) {
					activateRole(role);
				}
			}
		}
	};
}
