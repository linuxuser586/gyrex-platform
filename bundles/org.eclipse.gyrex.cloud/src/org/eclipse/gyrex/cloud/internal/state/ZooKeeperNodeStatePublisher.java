/*******************************************************************************
 * Copyright (c) 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal.state;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.gyrex.cloud.internal.zk.GateDownException;
import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperBasedService;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.services.state.INodeState;
import org.eclipse.gyrex.cloud.services.state.query.INodeStateInfo;
import org.eclipse.gyrex.common.identifiers.IdHelper;

import org.eclipse.core.runtime.IPath;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.ConnectionLossException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NotEmptyException;
import org.apache.zookeeper.KeeperException.SessionExpiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publishes and reads {@link INodeStateInfo} from ZooKeeper.
 */
public class ZooKeeperNodeStatePublisher extends ZooKeeperBasedService {

	private static final class ReadAllForNodeId extends ReadFromPath {
		private final String nodeId;

		private ReadAllForNodeId(final String nodeId) {
			super(IZooKeeperLayout.PATH_NODES_STATE_BY_NODE_ID.append(nodeId));
			this.nodeId = nodeId;
		}

		@Override
		protected NodeStateInfoImpl createInfo(final Map<String, String> data, final String childName) {
			return new NodeStateInfoImpl(childName, nodeId, data);
		}
	}

	private static final class ReadAllForServicePid extends ReadFromPath {
		private final String servicePid;

		private ReadAllForServicePid(final String servicePid) {
			super(IZooKeeperLayout.PATH_NODES_STATE_BY_SERVICE_PID.append(servicePid));
			this.servicePid = servicePid;
		}

		@Override
		protected NodeStateInfoImpl createInfo(final Map<String, String> data, final String childName) {
			return new NodeStateInfoImpl(servicePid, childName, data);
		}
	}

	private static abstract class ReadFromPath extends ZooKeeperGateCallable<List<? extends INodeStateInfo>> {
		private final IPath path;

		public ReadFromPath(final IPath path) {
			this.path = path;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected List<? extends INodeStateInfo> call(final ZooKeeperGate keeper) throws Exception {
			// read child names
			List<String> childNames;
			try {
				childNames = keeper.readChildrenNames(path, null);
			} catch (final NoNodeException e) {
				return Collections.emptyList();
			}

			if (childNames.isEmpty()) {
				return Collections.emptyList();
			}

			final List<INodeStateInfo> infos = new ArrayList<INodeStateInfo>(childNames.size());
			for (final String childName : childNames) {
				// read data
				byte[] data;
				try {
					data = keeper.readRecord(path.append(childName), null);
				} catch (final NoNodeException e) {
					// skip to next
					continue;
				}

				// de-serialize service properties
				final Properties props = new Properties();
				props.load(new ByteArrayInputStream(data));

				// wrap into untyped, unmodifiable map
				final Map map = Collections.unmodifiableMap(props);

				// create info
				infos.add(createInfo(map, childName));
			}

			return infos;
		}

		protected abstract NodeStateInfoImpl createInfo(final Map<String, String> data, final String childName);

	}

	private static final class SortedProperties extends Properties {
		private static final long serialVersionUID = 1L;

		@Override
		public synchronized Enumeration<Object> keys() {
			return Collections.enumeration(keySet());
		}

		@Override
		public Set<Object> keySet() {
			return new TreeSet<Object>(super.keySet());
		}
	}

	private static final Set<String> KEYS_TO_IGNORE = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(Constants.OBJECTCLASS, Constants.SERVICE_ID, Constants.SERVICE_PID, Constants.SERVICE_RANKING)));
	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperNodeStatePublisher.class);

	static Properties getStateData(final ServiceReference<INodeState> reference) {
		final Properties props = new SortedProperties();
		for (final String key : reference.getPropertyKeys()) {
			if (KEYS_TO_IGNORE.contains(key)) {
				continue;
			}
			final Object value = reference.getProperty(key);
			if (null != value) {
				props.setProperty(key, String.valueOf(value));
			}
		}
		return props;
	}

	private final String myNodeId;

	/**
	 * Creates a new instance.
	 * 
	 * @param myNodeId
	 */
	public ZooKeeperNodeStatePublisher(final String myNodeId) {
		this.myNodeId = myNodeId;
	}

	public List<? extends INodeStateInfo> findByNodeId(final String nodeId) {
		if (!IdHelper.isValidId(nodeId)) {
			throw new IllegalArgumentException("Invalid Node ID");
		}

		try {
			return execute(new ReadAllForNodeId(nodeId));
		} catch (final SessionExpiredException e) {
			// safe to ignore because we use ephemeral nodes
		} catch (final Exception e) {
			if ((e instanceof ConnectionLossException) || (e instanceof GateDownException)) {
				LOG.debug("Unable to read state for node '{}'. {}", new Object[] { nodeId, ExceptionUtils.getRootCauseMessage(e), e });
			} else {
				LOG.error("Unable to read state for node '{}'. {}", new Object[] { nodeId, ExceptionUtils.getRootCauseMessage(e), e });
			}
		}
		return Collections.emptyList();
	}

	public List<? extends INodeStateInfo> findByServicePid(final String servicePid) {
		if (!IdHelper.isValidId(servicePid)) {
			throw new IllegalArgumentException("Invalid Service PID");
		}

		try {
			return execute(new ReadAllForServicePid(servicePid));
		} catch (final SessionExpiredException e) {
			// safe to ignore because we use ephemeral nodes
		} catch (final Exception e) {
			if ((e instanceof ConnectionLossException) || (e instanceof GateDownException)) {
				LOG.debug("Unable to read state for service pid '{}'. {}", new Object[] { servicePid, ExceptionUtils.getRootCauseMessage(e), e });
			} else {
				LOG.error("Unable to read state for service pid '{}'. {}", new Object[] { servicePid, ExceptionUtils.getRootCauseMessage(e), e });
			}
		}
		return Collections.emptyList();
	}

	@Override
	protected String getToStringDetails() {
		return myNodeId;
	}

	public void publish(final String pid, final ServiceReference<INodeState> reference) {
		if (!IdHelper.isValidId(pid)) {
			throw new IllegalArgumentException("Invalid PID");
		}
		try {
			execute(new ZooKeeperGateCallable<Boolean>() {

				@Override
				protected Boolean call(final ZooKeeperGate keeper) throws Exception {

					// create data to save
					final Properties props = getStateData(reference);
					if (props.isEmpty()) {
						return Boolean.FALSE;
					}

					// serialize service properties
					final ByteArrayOutputStream out = new ByteArrayOutputStream();
					props.store(out, null);
					final byte[] data = out.toByteArray();

					// write data
					keeper.writeRecord(IZooKeeperLayout.PATH_NODES_STATE_BY_NODE_ID.append(myNodeId).append(pid), CreateMode.EPHEMERAL, data);
					keeper.writeRecord(IZooKeeperLayout.PATH_NODES_STATE_BY_SERVICE_PID.append(pid).append(myNodeId), CreateMode.EPHEMERAL, data);

					return Boolean.TRUE;
				}
			});
		} catch (final SessionExpiredException e) {
			// safe to ignore because we use ephemeral nodes
		} catch (final Exception e) {
			if ((e instanceof ConnectionLossException) || (e instanceof GateDownException)) {
				LOG.debug("Unable to publish node state '{}'. {}", new Object[] { pid, ExceptionUtils.getRootCauseMessage(e), e });
			} else {
				LOG.error("Unable to publish node state '{}'. {}", new Object[] { pid, ExceptionUtils.getRootCauseMessage(e), e });
			}
		}
	}

	public void remove(final String pid) {

		try {
			execute(new ZooKeeperGateCallable<Boolean>() {

				@Override
				protected Boolean call(final ZooKeeperGate keeper) throws Exception {
					// remove any written path
					keeper.deletePath(IZooKeeperLayout.PATH_NODES_STATE_BY_NODE_ID.append(myNodeId).append(pid));
					keeper.deletePath(IZooKeeperLayout.PATH_NODES_STATE_BY_SERVICE_PID.append(pid).append(myNodeId));

					// remove parents if empty
					try {
						keeper.deletePath(IZooKeeperLayout.PATH_NODES_STATE_BY_NODE_ID.append(myNodeId));
						keeper.deletePath(IZooKeeperLayout.PATH_NODES_STATE_BY_NODE_ID);
					} catch (final NotEmptyException e) {
						// not-empty
					}
					try {
						keeper.deletePath(IZooKeeperLayout.PATH_NODES_STATE_BY_SERVICE_PID.append(pid));
						keeper.deletePath(IZooKeeperLayout.PATH_NODES_STATE_BY_SERVICE_PID);
					} catch (final NotEmptyException e) {
						// not-empty
					}
					return Boolean.TRUE;
				}

			});
		} catch (final SessionExpiredException e) {
			// safe to ignore because we use ephemeral nodes
		} catch (final Exception e) {
			if ((e instanceof ConnectionLossException) || (e instanceof GateDownException)) {
				LOG.debug("Unable to remove node state '{}'. {}", new Object[] { pid, ExceptionUtils.getRootCauseMessage(e), e });
			} else {
				LOG.error("Unable to remove node state '{}'. {}", new Object[] { pid, ExceptionUtils.getRootCauseMessage(e), e });
			}
		}
	}

	public void shutdown() {
		close();
	}

}
