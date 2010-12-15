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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate.IConnectionMonitor;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperMonitor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Captures general state of the cloud.
 * <p>
 * This class maintains the cloud membership of a node in a cluster. When active
 * it waits for ZooKeeperGate connection event. Once connected, it tries to
 * register the node using a generate node id.
 * </p>
 */
public class CloudState implements IConnectionMonitor {

	private final class RegistrationJob extends Job implements ISchedulingRule {
		private static final int MAX_DELAY = 30000;
		private static final int MIN_DELAY = 1000;
		private volatile int delay = 0;

		public RegistrationJob() {
			super("Cloud Registration");
			setSystem(true);
			setRule(this);
			setPriority(SHORT);
		}

		@Override
		public boolean contains(final ISchedulingRule rule) {
			return rule == this;
		}

		@Override
		public boolean isConflicting(final ISchedulingRule rule) {
			// we conflict on the class name in order to also conflict with parallel versions
			// XXX this does not handle refactorings
			return rule.getClass().getName().equals(RegistrationJob.class.getName());
		}

		private void reschedule() {
			delay = Math.min(MAX_DELAY, delay > 0 ? delay * 2 : MIN_DELAY);
			LOG.warn("Node registration in cloud failed. Will retry in {} seconds.", delay / 1000);
			schedule(delay);
		}

		@Override
		protected IStatus run(final IProgressMonitor monitor) {
			if (monitor.isCanceled() || (getConnectState() != State.CONNECTING)) {
				return Status.CANCEL_STATUS;
			}

			if (CloudDebug.cloudState) {
				LOG.debug("Attempting node registration in cloud.");
			}

			if (!registerWithCloud()) {
				// only re-schedule if state is still CONNECTING
				if (getConnectState() == State.CONNECTING) {
					reschedule();
				}
				return Status.CANCEL_STATUS;
			}

			if (CloudDebug.cloudState) {
				LOG.debug("Node registered in cloud!");
			}
			return Status.OK_STATUS;
		}
	}

	public static enum State {
		DISCONNECTED, CONNECTING, CONNECTED, CLOSED
	}

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
		// note, we use get() here in order to support concurrent #unregisterNode calls which should return null!
		// (addConnectionMonitor can handle null monitors)
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

		// set CLOSED
		cloudState.state.set(State.CLOSED);
	}

	private final Lock registrationLock = new ReentrantLock();
	private final AtomicReference<State> state = new AtomicReference<State>(State.DISCONNECTED);
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
				reconnect(nodeInfo);
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
				reconnect(nodeInfo);
			}
		}

		private void reconnect(final NodeInfo nodeInfo) {
			try {
				unregisterNode();
			} catch (final Exception e) {
				if (CloudDebug.cloudState) {
					LOG.debug("Exception unregistering node {}. {}", new Object[] { nodeInfo, e.getMessage(), e });
				}
			}
			try {
				registerNode();
			} catch (final Exception e) {
				if (CloudDebug.debug) {
					LOG.error("Failed to re-register node. Node {} will not be available in cloud. {}", new Object[] { nodeInfo, e.getMessage(), e });
				} else {
					LOG.error("Failed to re-register node. Node {} will not be available in cloud. {}", new Object[] { nodeInfo, e.getMessage() });
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
				// force re-connect
				reconnect(nodeInfo);
			}
		}
	};

	/**
	 * Creates a new instance.
	 */
	private CloudState() {
	}

	@Override
	public void connected() {
		// update state (but only if DISCONNECTED)
		if (!state.compareAndSet(State.DISCONNECTED, State.CONNECTING)) {
			LOG.warn("Received CONNECTED event but old state is not DISCONNECTED: {}", this);
			return;
		}

		// register asynchronously
		new RegistrationJob().schedule();
	};

	@Override
	public void disconnected() {
		final State oldState = state.getAndSet(State.DISCONNECTED);
		if (oldState == State.DISCONNECTED) {
			// already disconnected
			return;
		}

		// synchronize in order to prevent concurrent registrations
		registrationLock.lock();
		try {
			// get current node info
			final NodeInfo nodeInfo = myInfo.getAndSet(null);
			if (nodeInfo == null) {
				// already unregistered
				return;
			}

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
		} finally {
			registrationLock.unlock();
		}
	}

	State getConnectState() {
		return state.get();
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

	/**
	 * Attempts registering the node in the cloud.
	 * 
	 * @return <code>true</code> on success, <code>false</code> otherwise
	 */
	boolean registerWithCloud() {
		// synchronize in order to prevent concurrent registrations
		registrationLock.lock();
		try {
			if (getConnectState() != State.CONNECTING) {
				// abort when state is not connecting
				return false;
			}

			// load / create our node info
			NodeInfo nodeInfo = null;
			try {
				nodeInfo = initializeNodeInfo();
				LOG.info("Node {} initialized.", nodeInfo);
			} catch (final Exception e) {
				// log error
				if (CloudDebug.debug) {
					LOG.error("Unable to initialize node info. Node will not be available in cloud. {}", new Object[] { e.getMessage(), e });
				} else {
					LOG.error("Unable to initialize node info. Node will not be available in cloud. {}", new Object[] { e.getMessage() });
				}
				return false;
			}

			// set node info
			myInfo.set(nodeInfo);

			// set node online
			try {
				if (nodeInfo.isApproved()) {
					setNodeOnline(nodeInfo);
					LOG.info("Node {} is now online.", nodeInfo);
				} else {
					LOG.info("Node {} needs to be approved first. Please contact cloud administrator with node details for approval.", nodeInfo);
				}
			} catch (final Exception e) {
				if (CloudDebug.debug) {
					LOG.error("Unable to set node to online. Node will not be available in cloud.", e);
				} else {
					LOG.error("Unable to set node to online. Node will not be available in cloud. {}", e.getMessage());
				}

				return false;
			}

			// successful registration at this point
			return true;
		} finally {
			registrationLock.unlock();
		}
	}

	private void setNodeOffline(final NodeInfo node) throws Exception {
		// de-activate cloud roles
		for (final String role : node.getRoles()) {
			LocalRolesManager.deactivateRole(role);
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

		// create an ephemeral record for this node in the "online" tree
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
			LocalRolesManager.activateRole(role);
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("CloudState [").append(state.get()).append(", ").append(myInfo.get()).append("]");
		return builder.toString();
	}
}
