/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal.preferences;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.cloud.internal.CloudDebug;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperBasedService;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperHelper;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperMonitor;
import org.eclipse.gyrex.common.identifiers.IdHelper;

import org.eclipse.core.runtime.Path;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.ConnectionLossException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.KeeperException.SessionExpiredException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the counter-part to {@link ZooKeeperBasedPreferences} which
 * implements all the ZooKeeper communication.
 * <p>
 * There can potentially be many, many {@link ZooKeeperBasedPreferences
 * preference objects}. Instead of having each of them act as an individual
 * object communicating with ZooKeeper a single
 * {@link ZooKeeperPreferencesService service} object is used. This object
 * implements reading as well as persisting preferences.
 * </p>
 */
public class ZooKeeperPreferencesService extends ZooKeeperBasedService {

	/**
	 * Callable implementation for
	 * {@link ZooKeeperPreferencesService#connectNode(ZooKeeperBasedPreferences)}
	 * 
	 * @see ZooKeeperPreferencesService#connectNode(ZooKeeperBasedPreferences)
	 */
	private class ConnectNode extends ZooKeeperCallable<Boolean> {
		private final String path;

		/**
		 * Creates a new instance.
		 * 
		 * @param activeNode
		 */
		public ConnectNode(final String path) {
			this.path = path;
		}

		@Override
		protected Boolean call(final ZooKeeper keeper) throws Exception {
			checkClosed();

			final ZooKeeperBasedPreferences node = activeNodesByPath.get(path);
			if (null == node) {
				return false;
			}

			// log message
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Connecting node {} at {}.", new Object[] { node, path });
			}

			// check if path exists
			if (null == keeper.exists(path, monitor)) {
				if (CloudDebug.zooKeeperPreferences) {
					LOG.debug("Node {} at {} does not exists in ZooKeeper. Nothing to load.", node, path);
				}

				// done
				return true;
			}

			// note, when connecting a node for the first time we must not force
			// sync with remote; instead we can rely on the version to correctly
			// represent if a node is new and must be updated or if the
			// remote is newer and the local info must be updated

			// there is also some subtile implementation detail which requires
			// to carefully set force sync ... in case a node does not exist
			// remotely it will be deleted locally if force sync is true

			// thus, if a node had been connected and loaded successfully before
			// its version will be greater than -1; in such a case we must remove
			// it locally if it doesn't exist anymore when the system comes back
			// online; therefore, we set force sync based on version comparison

			// load properties
			new RefreshProperties(path, node.propertiesVersion > -1).call(keeper);

			// load children
			new RefreshChildren(path, node.childrenVersion > -1).call(keeper);

			// log message
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Connected node {} at {}.", new Object[] { node, path });
			}

			// done
			return true;
		}

	}

	/**
	 * Callable implementation for
	 * {@link ZooKeeperPreferencesService#refreshChildren(String, boolean)}.
	 * 
	 * @see ZooKeeperPreferencesService#refreshChildren(String, boolean)
	 */
	private final class RefreshChildren extends ZooKeeperCallable<Boolean> {

		private final boolean forceSyncWithRemoteVersion;
		private final String path;

		/**
		 * Creates a new instance.
		 * 
		 * @param path
		 * @param forceSyncWithRemoteVersion
		 */
		private RefreshChildren(final String path, final boolean forceSyncWithRemoteVersion) {
			this.path = path;
			this.forceSyncWithRemoteVersion = forceSyncWithRemoteVersion;
		}

		@Override
		protected Boolean call(final ZooKeeper keeper) throws Exception {
			checkClosed();

			final ZooKeeperBasedPreferences node = activeNodesByPath.get(path);
			if (null == node) {
				return false;
			}

			// log message
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Reading children for node {} (cversion {}) from ZooKeeper {}", new Object[] { node, node.childrenVersion, path });
			}

			// get list of children (and also set watcher)
			final Stat stat = new Stat();

			// there is an issue with childrenVersion; ZooKeeper has no atomic way to set/get/sync
			// children; for example, when creating an empty node in ZooKeeper the childrenVersion is 0;
			// this conflicts with a new node with children and #loadChildren call triggered by a watcher
			// which would remove all children (after childrenModifyLock is released) because this nodes
			// childrenVersion is still -1;
			// the only thing we can do in order to prevent watchers on the same node to remove children
			// while we are adding them is to ensure that the childrenModifyLock is properly set
			// and we wait for any concurrent flushes to finish before loading the list for children from ZooKeeper
			if (!node.childrenModifyLock.tryLock(45, TimeUnit.SECONDS)) {
				throw new IllegalStateException(String.format("lock timeout waiting for childrenModifyLock on node '%s'", node));
			}
			try {
				final Collection<String> childrenNames = keeper.getChildren(path, monitor, stat);

				// don't load children if version is in the past
				if (!forceSyncWithRemoteVersion && (node.childrenVersion >= stat.getCversion())) {
					if (CloudDebug.zooKeeperPreferences) {
						LOG.debug("Not updating children of node {} - local cversion ({}) >= ZooKeeper cversion ({})", new Object[] { node, node.childrenVersion, stat.getCversion() });
					}
					return false;
				}

				// update children
				node.loadChildren(childrenNames, stat.getCversion());
			} catch (final NoNodeException e) {
				// the node does not exist in ZooKeeper
				if (!forceSyncWithRemoteVersion) {
					// no sync forced, thus we'll keep all the existing modifications and abort here
					if (CloudDebug.zooKeeperPreferences) {
						LOG.debug("Not updating children of node {} (local cversion {}) - node does not exist in ZooKeeper ({})", new Object[] { node, node.childrenVersion, e.getMessage() });
					}
					return false;
				}

				// a sync is forced and the node does not exist
				// thus, we need to remove the local node
				// note, we do it the long way in order to trigger proper events
				try {
					node.removeNode(true);
				} catch (final Exception ignored) {
					// assume the node is removed anyway
					deactivateNode(node);
				}
			} finally {
				node.childrenModifyLock.unlock();
			}

			// done
			return true;
		}
	}

	/**
	 * Callable implementation for
	 * {@link ZooKeeperPreferencesService#refreshProperties(String, boolean)}.
	 * 
	 * @see ZooKeeperPreferencesService#refreshProperties(String, boolean)
	 */
	private final class RefreshProperties extends ZooKeeperCallable<Boolean> {

		private final boolean forceSyncWithRemoteVersion;
		private final String path;

		/**
		 * Creates a new instance.
		 * 
		 * @param path
		 * @param forceSyncWithRemoteVersion
		 */
		private RefreshProperties(final String path, final boolean forceSyncWithRemoteVersion) {
			this.path = path;
			this.forceSyncWithRemoteVersion = forceSyncWithRemoteVersion;
		}

		@Override
		protected Boolean call(final ZooKeeper keeper) throws Exception {
			checkClosed();

			final ZooKeeperBasedPreferences node = activeNodesByPath.get(path);
			if (null == node) {
				return false;
			}

			// log message
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Reading properties for node {} (version {}) from ZooKeeper {}", new Object[] { node, node.propertiesVersion, path });
			}

			// read record data (and set new watcher)
			final Stat stat = new Stat();
			byte[] bytes;
			try {
				bytes = keeper.getData(path, monitor, stat);

				// don't load properties if version is in the past
				if (!forceSyncWithRemoteVersion && (node.propertiesVersion >= stat.getVersion())) {
					if (CloudDebug.zooKeeperPreferences) {
						LOG.debug("Not updating properties of node {} - local version ({}) >= ZooKeeper version ({})", new Object[] { node, node.propertiesVersion, stat.getVersion() });
					}
					return false;
				}

				// update node properties
				node.loadProperties(bytes, stat.getVersion());
			} catch (final NoNodeException e) {
				// the node does not exist in ZooKeeper
				if (!forceSyncWithRemoteVersion) {
					// no sync forced, thus we'll keep all the existing modifications and abort here
					if (CloudDebug.zooKeeperPreferences) {
						LOG.debug("Not updating properties of node {} (local version {}) - node does not exist in ZooKeeper ({})", new Object[] { node, node.propertiesVersion, e.getMessage() });
					}
					return false;
				}

				// a sync is forced and the node does not exist
				// thus, we need to remove it locally
				// note, we do it the long way in order to trigger proper events
				try {
					node.removeNode(true);
				} catch (final Exception ignored) {
					// assume the node is removed anyway
					deactivateNode(node);
				}
			}

			// done
			return true;
		}
	}

	/**
	 * Callable implementation for
	 * {@link ZooKeeperPreferencesService#removeNode(String)}.
	 * 
	 * @see ZooKeeperPreferencesService#removeNode(String)
	 */
	private final class RemoveNode extends ZooKeeperGateCallable<Boolean> {

		private final String path;

		/**
		 * Creates a new instance.
		 * 
		 * @param path
		 */
		private RemoveNode(final String path) {
			this.path = path;
		}

		@Override
		protected Boolean call(final ZooKeeperGate gate) throws Exception {
			checkClosed();
			gate.deletePath(new Path(path));
			return true;
		}
	}

	/**
	 * Callable implementation for
	 * {@link ZooKeeperPreferencesService#writeProperties(String, byte[], int)}.
	 * 
	 * @see ZooKeeperPreferencesService#writeProperties(String, byte[], int)
	 */
	private final class WriteProperties extends ZooKeeperCallable<Integer> {

		private final String path;
		private final byte[] propertyBytes;
		private final int propertiesVersion;

		/**
		 * Creates a new instance.
		 * 
		 * @param path
		 * @param propertyBytes
		 * @param propertiesVersion
		 */
		private WriteProperties(final String path, final byte[] propertyBytes, final int propertiesVersion) {
			this.path = path;
			this.propertyBytes = propertyBytes;
			this.propertiesVersion = propertiesVersion;
		}

		@Override
		protected Integer call(final ZooKeeper keeper) throws Exception {
			checkClosed();

			final ZooKeeperBasedPreferences node = activeNodesByPath.get(path);
			if (null == node) {
				return -1;
			}

			// log message
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Writing properties for node {} (version {}) to ZooKeeper {}", new Object[] { node, node.propertiesVersion, path });
			}

			// create node but only if it doesn't exists and no explicit version is requested
			// (if version is specified and the node does not exists then it might have been removed meanwhile)
			if ((propertiesVersion < 0) && (null == keeper.exists(path, false))) {
				// create parents
				ZooKeeperHelper.createParents(keeper, new Path(path));

				// create path
				Stat stat = null;
				while (null == stat) {
					try {
						keeper.create(path, propertyBytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
						stat = keeper.exists(path, false);
					} catch (final NodeExistsException e) {
						// it has been created concurrently (this is bad luck)
						// fail so that final higher level API final can react on those events
						throw e;
					}
				}
				return stat.getVersion();
			}

			// write data
			return keeper.setData(path, propertyBytes, propertiesVersion).getVersion();
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperPreferencesService.class);

	/**
	 * Singleton monitor to listen for changes in ZooKeeper.
	 * <p>
	 * As there can be many, many preference we only use a single monitor object
	 * to listen for changes to nodes in ZooKeeper. When the ZooKeeper
	 * connection is lost and a new session is initiated, the monitor needs to
	 * be re-registered for all existing nodes. However, only already loaded
	 * nodes should be refreshed/hooked.
	 * </p>
	 * <p>
	 * Only the following events will be processed by the monitor:
	 * <ul>
	 * <li>RECORD CHANGED - when properties of a node should be refreshed</li>
	 * <li>CHILDREN CHANGED - when children of a node should be refreshed</li>
	 * <li>PATH CREATED - when a node that has been created locally (but never
	 * flushed) was also created remotely</li>
	 * </ul>
	 * This streamlines the update handling processing.
	 * </p>
	 */
	final ZooKeeperMonitor monitor = new ZooKeeperMonitor() {

		@Override
		protected void childrenChanged(final String path) {
			if (isClosed()) {
				return;
			}

			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("({}) Preference at {} updated remotely: CHILDREN CHANGED", ZooKeeperPreferencesService.this, path);
			}

			try {
				// refresh children and notify listeners (but only if remote is newer)
				refreshChildren(path, false);
			} catch (final Exception ignored) {
				LOG.debug("Ignored exception refreshing children of '{}': {} ", new Object[] { path, ExceptionUtils.getRootCauseMessage(ignored), ignored });
			}
		}

		@Override
		protected void pathCreated(final String path) {
			if (isClosed()) {
				return;
			}

			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("({}) Preference at {} created remotely", ZooKeeperPreferencesService.this, path);
			}

			try {
				// refresh properties and notify listeners (but only if remote is newer)
				refreshProperties(path, false);

				// also refresh children and notify listeners (but only if remote is newer)
				// (this is necessary because even the parent node may not exist;
				// in such a case, this will be the only watch triggered by ZooKeeper)
				// TODO: we may want to "schedule" a refresh of all parent nodes
				refreshChildren(path, false);
			} catch (final Exception ignored) {
				LOG.debug("Ignored exception refreshing properties stored at '{}': {} ", new Object[] { path, ExceptionUtils.getRootCauseMessage(ignored), ignored });
			}
		}

		@Override
		protected void recordChanged(final String path) {
			if (isClosed()) {
				return;
			}

			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("({}) Preference at {} updated remotely: PROPERTIES", ZooKeeperPreferencesService.this, path);
			}

			try {
				// refresh properties and notify listeners (but only if remote is newer)
				refreshProperties(path, false);
			} catch (final Exception ignored) {
				LOG.debug("Ignored exception refreshing properties stored at '{}': {} ", new Object[] { path, ExceptionUtils.getRootCauseMessage(ignored), ignored });
			}
		};
	};

	final String name;
	final ConcurrentMap<String, ZooKeeperBasedPreferences> activeNodesByPath = new ConcurrentHashMap<String, ZooKeeperBasedPreferences>();

	private volatile boolean connected = true;

	/**
	 * Creates a new instance and immediately activates it.
	 * 
	 * @param name
	 *            the service name (mainly for debugging purposed)
	 */
	public ZooKeeperPreferencesService(final String name) {
		super(50l, 3); /* experiment with a short retry dely for preferences */
		if (!IdHelper.isValidId(name)) {
			throw new IllegalArgumentException("invalid name; please use only ascii chars (see IdHelper)");
		}
		this.name = name;

		// immediately activate the service
		activate();
	}

	/**
	 * Activates the specified node.
	 * <p>
	 * This method is triggered when a new node is added to the local tree and
	 * events must be processed for the node.
	 * </p>
	 * 
	 * @param node
	 *            the node to activate
	 * @return <code>true</code> if the node has to be connected and the method
	 *         should fail it it couldn't (note, the node is still active
	 *         afterwards)
	 * @throws Exception
	 */
	public final void activateNode(final ZooKeeperBasedPreferences node, final boolean forceConnection) throws Exception {
		checkClosed();

		// add to active nodes
		final ZooKeeperBasedPreferences existingNode = activeNodesByPath.putIfAbsent(node.zkPath, node);
		if (null != existingNode) {
			// already active
			// sanity check
			if (existingNode != node) {
				LOG.error("Attempt to activate a second node for the same path {}: existing=({}) new=({})", new Object[] { node.zkPath, existingNode, node, new Exception("Activation Call Stack") });
				throw new IllegalStateException(String.format("Invalid attempt to activate a seconde node instance (%s).", node.absolutePath()));
			}
			return;
		}

		// connect the node if possible or required
		if (connected || forceConnection) {
			try {
				connectNode(node);
			} catch (final ConnectionLossException e) {
				if (forceConnection) {
					throw e;
				}
				// ignore (will re-connect when gate comes back)
				LOG.debug("Ignored exception connecting node {}: {} ", new Object[] { node, ExceptionUtils.getRootCauseMessage(e), e });
			} catch (final SessionExpiredException e) {
				if (forceConnection) {
					throw e;
				}
				// ignore (will re-connect when gate comes back)
				LOG.debug("Ignored exception connecting node {}: {} ", new Object[] { node, ExceptionUtils.getRootCauseMessage(e), e });
			}
		}
	}

	final void checkClosed() {
		if (isClosed()) {
			throw new IllegalStateException("CLOSED");
		}
	}

	/**
	 * Connects the specified node.
	 * <p>
	 * This method ensures that essential watchers are registered in ZooKeeper.
	 * </p>
	 * 
	 * @param node
	 *            the node to connect
	 * @return <code>true</code> if the node has been connected
	 * @throws Exception
	 */
	final void connectNode(final ZooKeeperBasedPreferences node) throws Exception {
		checkClosed();

		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("Stack for connectNode request for node {}.", node, new Exception("Stack for preference node connection request."));
		}

		// connect the node (i.e. hook essential watchers)
		execute(new ConnectNode(node.zkPath));
	}

	/**
	 * Deactivates the specified node.
	 * <p>
	 * This method is triggered when a node is removed from the local tree and
	 * the service should no longer process events for the node.
	 * </p>
	 * 
	 * @param node
	 *            the node to de activate
	 */
	public final void deactivateNode(final ZooKeeperBasedPreferences node) {
		// simply remove from the active nodes map
		activeNodesByPath.remove(node.zkPath, node);
	}

	@Override
	protected void disconnect() {
		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("Disconnecting preferences {}", this);
		}

		// just set disconnected
		connected = false;
	}

	@Override
	protected void doClose() {
		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("Closing preferences {}", this);
		}

		// set disconnected
		connected = false;

		// invalidate all nodes
		final Collection<ZooKeeperBasedPreferences> values = activeNodesByPath.values();
		for (final ZooKeeperBasedPreferences preferences : values) {
			preferences.dispose();
		}

		// clear active nodes
		activeNodesByPath.clear();
	}

	@Override
	protected String getToStringDetails() {
		final StringBuilder details = new StringBuilder();
		details.append(name);
		return details.toString();
	}

	/**
	 * Indicates if the specified node is active.
	 * 
	 * @param node
	 * @return <code>true</code> if active, <code>false</code> otherwise
	 */
	public final boolean isActive(final ZooKeeperBasedPreferences node) {
		return activeNodesByPath.get(node.zkPath) == node;
	}

	/**
	 * Indicates if the service is currently connected to ZooKeeper.
	 * 
	 * @return <code>true</code> if connected, <code>false</code> otherwise
	 */
	public final boolean isConnected() {
		return connected;
	}

	@Override
	protected void reconnect() {
		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("Reconnecting preferences {}", this);
		}

		// set connected
		connected = true;

		// re-connect all available nodes
		final HashSet<ZooKeeperBasedPreferences> nodes = new HashSet<ZooKeeperBasedPreferences>(activeNodesByPath.values());
		for (final ZooKeeperBasedPreferences node : nodes) {
			try {
				connectNode(node);
			} catch (final Exception e) {
				LOG.debug("Ignored exception connecting node {}: {} ", new Object[] { node, ExceptionUtils.getRootCauseMessage(e), e });
			}
		}
	}

	/**
	 * Refreshes the children of the specified path.
	 * <p>
	 * This method reads the nodes from ZooKeeper and processes <em>added</em>
	 * as well as <em>removed</em> nodes.
	 * </p>
	 * 
	 * @param path
	 *            the ZooKeeper path of the preference node to refresh
	 * @param forceSyncWithRemoteVersion
	 *            set to <code>true</code> in order to override all local
	 *            changes with what is in ZooKeeper (<code>false</code> to only
	 *            update the local content if the remove is newer)
	 */
	public final void refreshChildren(final String path, final boolean forceSyncWithRemoteVersion) throws Exception {
		checkClosed();

		// check if loaded
		if (!activeNodesByPath.containsKey(path)) {
			return;
		}

		execute(new RefreshChildren(path, forceSyncWithRemoteVersion));
	}

	/**
	 * Refreshes the properties of the specified path.
	 * <p>
	 * This method reads the data for a node from ZooKeeper and processes
	 * <em>added</em> as well as <em>updated</em> and <em>removed</em>
	 * properties.
	 * </p>
	 * 
	 * @param path
	 *            the ZooKeeper path of the preference node to refresh
	 * @param forceSyncWithRemoteVersion
	 *            set to <code>true</code> in order to override all local
	 *            changes with what is in ZooKeeper (<code>false</code> to only
	 *            update the local content if the remove is newer)
	 */
	public final void refreshProperties(final String path, final boolean forceSyncWithRemoteVersion) throws Exception {
		checkClosed();

		// check if loaded
		if (!activeNodesByPath.containsKey(path)) {
			return;
		}

		execute(new RefreshProperties(path, forceSyncWithRemoteVersion));
	}

	/**
	 * Removed the specified path in ZooKeeper.
	 * <p>
	 * Note, this does not make a node inactive.
	 * </p>
	 * 
	 * @param path
	 *            the ZooKeeper path of the preference node to remove
	 */
	public final void removeNode(final String path) throws Exception {
		checkClosed();

		execute(new RemoveNode(path));
	}

	/**
	 * Closes the service.
	 */
	public final void shutdown() {
		close();
	}

	/**
	 * Writes the properties at the specified path.
	 * <p>
	 * Any folder between will be created.
	 * </p>
	 * 
	 * @param path
	 *            the ZooKeeper path of the preference node to write
	 * @param propertyBytes
	 *            the bytes to write
	 * @param propertiesVersion
	 *            the version to expect
	 * @return the new version
	 * @throws Exception
	 */
	public final int writeProperties(final String path, final byte[] propertyBytes, final int propertiesVersion) throws Exception {
		checkClosed();

		// check if loaded
		if (!activeNodesByPath.containsKey(path)) {
			return -1;
		}
		return execute(new WriteProperties(path, propertyBytes, propertiesVersion));
	}
}
