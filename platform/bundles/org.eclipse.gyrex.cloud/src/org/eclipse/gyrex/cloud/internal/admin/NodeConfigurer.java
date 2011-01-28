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

import java.util.Set;

import org.eclipse.gyrex.cloud.admin.INodeConfigurer;
import org.eclipse.gyrex.cloud.internal.CloudActivator;
import org.eclipse.gyrex.cloud.internal.CloudState;
import org.eclipse.gyrex.cloud.internal.NodeInfo;
import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGateConfig;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperNodeInfo;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.ZooKeeper.States;

/**
 *
 */
public class NodeConfigurer implements INodeConfigurer {

	private final String nodeId;

	/**
	 * Creates a new instance.
	 * 
	 * @param nodeId
	 */
	public NodeConfigurer(final String nodeId) {
		this.nodeId = nodeId;
	}

	@Override
	public IStatus configureConnection(final String connectString) {
		ZooKeeper zk = null;
		try {
			if (connectString != null) {
				// try connect
				zk = new ZooKeeper(connectString, 5000, null);

				// wait at most 5 seconds for a connection
				int waitTimeMs = 5000;
				while ((zk.getState() != States.CONNECTED) && (waitTimeMs > 0)) {
					try {
						Thread.sleep(500);
						waitTimeMs -= 500;
					} catch (final InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}

				if (zk.getState() != States.CONNECTED) {
					throw new IllegalStateException(String.format("Timeout waiting for a connection to '%s'. Please verify the connect string.", connectString));
				}

				// try reading some information from the cloud
				if (null == zk.exists(IZooKeeperLayout.PATH_GYREX_ROOT.toString(), false)) {
					// maybe a new cloud, try initialization
					final String path = IZooKeeperLayout.PATH_GYREX_ROOT.toString();
					final String createdPath = zk.create(path, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
					if (!path.equals(createdPath)) {
						throw new IllegalStateException(String.format("created path does not match expected path (%s != %s)", path, createdPath));
					}
				}

				// at this point the connect string seems to be ok
				// TODO: store it in ZooKeeper and implement ZK to local sync
			}

			// store in instance preferences if local
			if (new NodeInfo().getNodeId().equals(nodeId)) {
				final Preferences preferences = new InstanceScope().getNode(CloudActivator.SYMBOLIC_NAME).node(ZooKeeperGateConfig.PREF_NODE_ZOOKEEPER);
				if (connectString != null) {
					preferences.put(ZooKeeperGateConfig.PREF_KEY_CLIENT_CONNECT_STRING, connectString);
				} else {
					preferences.remove(ZooKeeperGateConfig.PREF_KEY_CLIENT_CONNECT_STRING);
				}
				preferences.flush();

				// restart cloud
				CloudState.unregisterNode();
				CloudState.registerNode();
			}
		} catch (final Exception e) {
			return new Status(IStatus.ERROR, CloudActivator.SYMBOLIC_NAME, "Unable to connect to ZooKeeper. " + ExceptionUtils.getRootCauseMessage(e), e);
		} finally {
			if (zk != null) {
				try {
					zk.close();
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}

		return Status.OK_STATUS;
	}

	@Override
	public String getConnectionString() {
		final Preferences preferences = new InstanceScope().getNode(CloudActivator.SYMBOLIC_NAME).node(ZooKeeperGateConfig.PREF_NODE_ZOOKEEPER);
		return preferences.get(ZooKeeperGateConfig.PREF_KEY_CLIENT_CONNECT_STRING, null);
	}

	@Override
	public IStatus setLocation(final String text) {
		try {
			// load info
			final ZooKeeperNodeInfo info = ZooKeeperNodeInfo.load(nodeId, true);
			// update roles
			info.setLocation(text);
			// write info
			ZooKeeperNodeInfo.save(info, true);
		} catch (final Exception e) {
			return new Status(IStatus.ERROR, CloudActivator.SYMBOLIC_NAME, "Unable to update node info in ZooKeeper. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
		return Status.OK_STATUS;
	}

	@Override
	public IStatus setName(final String text) {
		try {
			// load info
			final ZooKeeperNodeInfo info = ZooKeeperNodeInfo.load(nodeId, true);
			// update roles
			info.setName(text);
			// write info
			ZooKeeperNodeInfo.save(info, true);
		} catch (final Exception e) {
			return new Status(IStatus.ERROR, CloudActivator.SYMBOLIC_NAME, "Unable to update node info in ZooKeeper. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
		return Status.OK_STATUS;
	}

	@Override
	public IStatus setTags(final Set<String> tags) {
		try {
			// load info
			final ZooKeeperNodeInfo info = ZooKeeperNodeInfo.load(nodeId, true);
			// update roles
			info.setTags(tags);
			// write info
			ZooKeeperNodeInfo.save(info, true);
		} catch (final Exception e) {
			return new Status(IStatus.ERROR, CloudActivator.SYMBOLIC_NAME, "Unable to update node info in ZooKeeper. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
		return Status.OK_STATUS;
	}
}
