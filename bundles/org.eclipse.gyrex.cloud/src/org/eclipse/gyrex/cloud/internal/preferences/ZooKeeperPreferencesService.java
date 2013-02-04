/*******************************************************************************
 * Copyright (c) 2011, 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal.preferences;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.cloud.internal.CloudDebug;
import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperBasedService;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperHelper;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperMonitor;
import org.eclipse.gyrex.common.identifiers.IdHelper;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.zookeeper.AsyncCallback.VoidCallback;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.ConnectionLossException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.KeeperException.OperationTimeoutException;
import org.apache.zookeeper.KeeperException.SessionExpiredException;
import org.apache.zookeeper.KeeperException.SessionMovedException;
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
	 * Watcher implementation that defers processing of events into a separate
	 * thread to avoid a backlog in the ZooKeeper event thread.
	 * <p>
	 * The following events will be processed by the monitor:
	 * <ul>
	 * <li>RECORD CHANGED - when properties of a node should be refreshed</li>
	 * <li>CHILDREN CHANGED - when children of a node changed</li>
	 * <li>PATH CREATED - when a node that has been created locally (but never
	 * flushed) was also created remotely (either through a local flush or a
	 * remote flush)</li>
	 * <li>PATH DELETE - when a node that has been watched locally was removed
	 * remotely (either through a local flush or a remote flush)</li>
	 * </ul>
	 * This streamlines the update handling processing.
	 * </p>
	 */
	private final class DeferredProcessingMonitor extends ZooKeeperMonitor {

		final PathEvents events = new PathEvents();
		final Thread processEventsThread = new Thread(ZooKeeperPreferencesService.this.toString() + " EventProcessor") {
			@Override
			public void run() {
				// spin the loop as long as the service is not closed
				while (!isClosed()) {
					processEventsLoop();
				}
			};
		};

		/**
		 * Creates a new instance.
		 */
		public DeferredProcessingMonitor() {
			// start the event processing thread as soon as possible
			processEventsThread.start();
		}

		@Override
		protected void childrenChanged(final String path) {
			if (isClosed())
				return;

			events.childrenChanged(path);
		}

		@Override
		protected void pathCreated(final String path) {
			if (isClosed())
				return;

			events.created(path);
		}

		@Override
		protected void pathDeleted(final String path) {
			if (isClosed())
				return;

			events.deleted(path);
		}

		void processEventsLoop() {
			// make sure a connection is available
			if (CloudDebug.zooKeeperPreferences && !isConnected() && !isClosed()) {
				LOG.debug("Waiting for ZooKeeper connection.");
			}
			while (!isConnected() && !isClosed()) {
				try {
					Thread.sleep(250L);
				} catch (final InterruptedException e) {
					// set interrupt flag and abort
					Thread.currentThread().interrupt();
					return;
				}
			}

			// as long as the service is not closed
			while (!isClosed()) {

				// now start fetching events
				if (CloudDebug.zooKeeperPreferences) {
					LOG.debug("Waiting for events from ZooKeeper.");
				}

				// get next event
				String path;
				try {
					path = events.next();
				} catch (final InterruptedException e) {
					if (CloudDebug.zooKeeperPreferences) {
						LOG.debug("Terminating ZooKeeper event processor thread {}.", processEventsThread);
					}

					// set interrupt flag
					Thread.currentThread().interrupt();

					// return
					return;
				}

				if (CloudDebug.zooKeeperPreferencesSync) {
					LOG.debug("Processing event for path {}.", path);
				}

				// handle event
				final ZooKeeperBasedPreferences node = activeNodesByPath.get(path);
				if (null != node) {
					try {
						// events are processed asynchronously so any path might already be re-created or removed
						final Stat stat = getVersionInfo(path);
						if (null == stat) {
							// remove the node
							node.removeNode(true);
						} else {
							// refresh the node
							loadNode(path, true);
						}
					} catch (final IllegalStateException | ConnectionLossException | SessionExpiredException | OperationTimeoutException | SessionMovedException e) {
						// this may be the result of a shutdown
						if (CLOSED.equals(e.getMessage()) || isClosed())
							return;

						// return event back to the queue in case of connect issues
						events.processingFailed(path);

						// log warning and continue
						LOG.warn("System is not able to properly process event '{}' at this time: {} ", new Object[] { path, ExceptionUtils.getRootCauseMessage(e), e });
					} catch (final Exception e) {
						// log error and continue
						LOG.error("Error process event '{}'. Preferences may need to be refreshed. {} ", new Object[] { path, ExceptionUtils.getRootCauseMessage(e), e });
					}
				}
			}

		}

		@Override
		protected void recordChanged(final String path) {
			if (isClosed())
				return;

			events.recordChanged(path);
		}
	}

	/**
	 * Callable implementation for
	 * {@link ZooKeeperPreferencesService#getVersionInfo(String))}.
	 * 
	 * @see ZooKeeperPreferencesService#getVersionInfo(String))
	 */
	private final class GetVersionInfo extends ZooKeeperCallable<Stat> {

		private final String path;

		/**
		 * Creates a new instance.
		 * 
		 * @param path
		 */
		private GetVersionInfo(final String path) {
			this.path = path;
		}

		@Override
		protected Stat call(final ZooKeeper keeper) throws Exception {
			checkClosed();
			return keeper.exists(path, false);
		}
	}

	/**
	 * Callable implementation for
	 * {@link ZooKeeperPreferencesService#loadNode(ZooKeeperBasedPreferences)}
	 * 
	 * @see ZooKeeperPreferencesService#loadNode(ZooKeeperBasedPreferences)
	 */
	private class LoadNode extends ZooKeeperCallable<Boolean> {
		private final String path;

		/**
		 * Creates a new instance.
		 * 
		 * @param activeNode
		 */
		public LoadNode(final String path) {
			this.path = path;
		}

		@Override
		protected Boolean call(final ZooKeeper keeper) throws Exception {
			checkClosed();

			final ZooKeeperBasedPreferences node = activeNodesByPath.get(path);
			if (null == node)
				return false;

			// log message
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Loading node {} at {}.", new Object[] { node, path });
			}

			// check if path exists 
			// (also set monitor to wait for its creation or deletion)
			// TODO: we might need to make this configurable per requests to prevent exist watches for properly propagated deletes
			if (null == keeper.exists(path, monitor)) {
				if (CloudDebug.zooKeeperPreferences) {
					LOG.debug("Node {} at {} does not exists in ZooKeeper. Nothing to load.", node, path);
				}

				// done
				return true;
			}

			// note, when connecting a node for the first time we must not force
			// sync with remote; instead we must rely on the version to correctly
			// represent if a node is new and must be updated or if the
			// remote is newer and the local info must be updated

			// load properties
			new RefreshProperties(path, false).call(keeper);

			// load children
			new RefreshChildren(path, false).call(keeper);

			// log message
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Done loading node {} (version {}, cversion {}) at {}.", new Object[] { node, node.propertiesVersion, node.childrenVersion, path });
			}

			// done
			return true;
		}

	}

	/**
	 * An structure for collecting and combining ZooKeeper path events.
	 */
	static final class PathEvents {

		private final LinkedHashSet<String> pathEvents = new LinkedHashSet<String>();

		public void childrenChanged(final String path) {
			submitEvent(path);
		}

		public void clear() {
			synchronized (pathEvents) {
				pathEvents.clear();
				pathEvents.notifyAll();
			}
		}

		public void created(final String path) {
			submitEvent(path);
		}

		public void deleted(final String path) {
			submitEvent(path);
		}

		/**
		 * Receives the next entry.
		 * <p>
		 * Blocks until an entry becomes available.
		 * </p>
		 * 
		 * @return the next entry
		 * @throws InterruptedException
		 */
		public String next() throws InterruptedException {
			String next = null;
			while (next == null) {
				// retrieve next event
				synchronized (pathEvents) {
					final Iterator<String> iterator = pathEvents.iterator();
					if (iterator.hasNext()) {
						// remove next from map
						// (we don't care if it's null because
						// the while loop ensure that null is never returned)
						next = iterator.next();
						iterator.remove();
					} else {
						// wait if none is available
						pathEvents.wait();
					}
				}
			}

			return next;
		}

		public void processingFailed(final String path) {
			submitEvent(path);
		}

		public void recordChanged(final String path) {
			submitEvent(path);
		}

		private void submitEvent(final String path) {
			// synchronize on pathEvents
			synchronized (pathEvents) {
				// add
				pathEvents.add(path);

				// notify next waiting thread
				pathEvents.notify();
			}
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
			if (null == node)
				return false;

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
			if (!node.childrenModifyLock.tryLock(5, TimeUnit.MINUTES))
				throw new IllegalStateException(String.format("lock timeout waiting for childrenModifyLock on node '%s'", node));
			try {
				// get children
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

				if (CloudDebug.zooKeeperPreferences) {
					LOG.debug("Exception reading children for node {} from ZooKeeper {}. Removing node.", new Object[] { node, path, e });
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

			// log message
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Done reading children for node {} (version {}) from ZooKeeper {}", new Object[] { node, node.propertiesVersion, path });
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
			if (null == node)
				return false;

			// log message
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Reading properties for node {} (version {}) from ZooKeeper {}", new Object[] { node, node.propertiesVersion, path });
			}

			// read record data (and set new watcher)
			final Stat stat = new Stat();
			byte[] bytes;
			try {
				bytes = keeper.getData(path, monitor, stat);

				// we must acquire the properties modification lock when comparing the node version
				// otherwise it may happen that we infere with ongoing flushes
				final boolean needsLock = !forceSyncWithRemoteVersion;
				if (needsLock && !node.propertiesModificationLock.tryLock(5, TimeUnit.MINUTES))
					throw new IllegalStateException(String.format("lock timeout waiting for childrenModifyLock on node '%s'", node));
				try {

					// don't load properties if version is in the past
					if (!forceSyncWithRemoteVersion && (node.propertiesVersion >= stat.getVersion())) {
						if (CloudDebug.zooKeeperPreferences) {
							LOG.debug("Not updating properties of node {} - local version ({}) >= ZooKeeper version ({})", new Object[] { node, node.propertiesVersion, stat.getVersion() });
						}
						return false;
					}

					// update node properties
					node.loadProperties(bytes, stat.getVersion());
				} finally {
					// only release if needs lock is true (which means we got it previously)
					if (needsLock) {
						node.propertiesModificationLock.unlock();
					}
				}
			} catch (final NoNodeException e) {
				// the node does not exist in ZooKeeper
				if (!forceSyncWithRemoteVersion) {
					// no sync forced, thus we'll keep all the existing modifications and abort here
					if (CloudDebug.zooKeeperPreferences) {
						LOG.debug("Not updating properties of node {} (local version {}) - node does not exist in ZooKeeper ({})", new Object[] { node, node.propertiesVersion, e.getMessage() });
					}
					return false;
				}

				if (CloudDebug.zooKeeperPreferences) {
					LOG.debug("Exception reading properties for node {} from ZooKeeper {}. Removing node.", new Object[] { node, path, e });
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

			// log message
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Done reading properties for node {} (version {}) from ZooKeeper {}", new Object[] { node, node.propertiesVersion, path });
			}

			// done
			return true;
		}
	}

	/**
	 * Callable implementation for
	 * {@link ZooKeeperPreferencesService#removeNode(String, int))}.
	 * 
	 * @see ZooKeeperPreferencesService#removeNode(String, int))
	 */
	private final class RemoveNode extends ZooKeeperCallable<Boolean> {

		private final String path;
		private final int propertiesVersion;
		private final int childrenVersion;

		/**
		 * Creates a new instance.
		 * 
		 * @param path
		 * @param propertiesVersion
		 * @param childrenVersion
		 */
		private RemoveNode(final String path, final int propertiesVersion, final int childrenVersion) {
			this.path = path;
			this.propertiesVersion = propertiesVersion;
			this.childrenVersion = childrenVersion;
		}

		@Override
		protected Boolean call(final ZooKeeper keeper) throws Exception {
			checkClosed();

			try {
				ZooKeeperHelper.deleteTree(keeper, new Path(path), propertiesVersion, childrenVersion);
			} catch (final NoNodeException e) {
				// consider this a successful deletion
				if (CloudDebug.zooKeeperPreferences) {
					LOG.debug("Node at {} already deleted. ({})", new Object[] { path, ExceptionUtils.getRootCauseMessage(e) });
				}
			}
			return true;
		}
	}

	/**
	 * Callable implementation for
	 * {@link ZooKeeperPreferencesService#sync(String))}.
	 * 
	 * @see ZooKeeperPreferencesService#sync(String))
	 */
	private final class Sync extends ZooKeeperCallable<Boolean> {

		private final class WaitForFinishCallback implements VoidCallback {
			private final CountDownLatch waitForSyncFinish = new CountDownLatch(1);

			private WaitForFinishCallback() {
			}

			@Override
			public void processResult(final int rc, final String path, final Object ctx) {
				waitForSyncFinish.countDown();
			}

			public Boolean waitForFinish(final long timeout, final TimeUnit unit) {
				try {
					return waitForSyncFinish.await(timeout, unit);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return waitForSyncFinish.getCount() == 0;
			}
		}

		private final String path;

		/**
		 * Creates a new instance.
		 * 
		 * @param path
		 */
		private Sync(final String path) {
			this.path = path;
		}

		@Override
		protected Boolean call(final ZooKeeper keeper) throws Exception {
			checkClosed();

			// the callback to simulate a synchronous wait
			final WaitForFinishCallback waitForSync = new WaitForFinishCallback();

			// sync with ZooKeeper
			keeper.sync(path, waitForSync, null);

			// wait for the sync to finish
			return waitForSync.waitForFinish(5L, TimeUnit.MINUTES);
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
			if (null == node)
				return -1;

			// log message
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Writing properties for node {} (version {}) to ZooKeeper {}", new Object[] { node, node.propertiesVersion, path });
			}

			//
			// ---> ZooKeeper WATCHES <---
			//
			// As a general rule no watches are set on WRITE calls!
			// The policy is that any call which *loads* data will register watches which 
			// will be triggered when data is written and thus set again by refresh/load calls 
			// (which happens because of the trigger-fresh-cycle).
			//

			// create node but only if it doesn't exists and no explicit version is requested
			// (if version is specified and the node does not exists then it might have been removed meanwhile)
			if ((propertiesVersion < 0) && (null == keeper.exists(path, false))) {
				// create parents
				ZooKeeperHelper.createParents(keeper, new Path(path));

				// create path
				try {
					keeper.create(path, propertyBytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				} catch (final NodeExistsException e) {
					// it has been created concurrently (this is bad luck)
					// fail so that higher level API can react on those events
					throw e;
				}

				// FIXME: there is a small time window between CREATE and EXISTS where data can be modified without any event
				// (https://issues.apache.org/jira/browse/ZOOKEEPER-1297)
				// in order to properly detect this we avoid the EXISTS call and assume version "0" for a successful CREATE
				return 0;
			}

			// write data
			return keeper.setData(path, propertyBytes, propertiesVersion).getVersion();
		}
	}

	static final String CLOSED = "CLOSED";

	private static final int MINIMUM_SEGMENT_COUNT = IZooKeeperLayout.PATH_PREFERENCES_ROOT.segmentCount() + 1;

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
	 */
	final DeferredProcessingMonitor monitor;

	final String name;
	final ConcurrentMap<String, ZooKeeperBasedPreferences> activeNodesByPath = new ConcurrentHashMap<String, ZooKeeperBasedPreferences>();

	private volatile boolean connected;

	/**
	 * Creates a new instance and immediately activates it.
	 * 
	 * @param name
	 *            the service name (mainly for debugging purposed)
	 */
	public ZooKeeperPreferencesService(final String name) {
		super(50l, 3); /* experiment with a short retry dely for preferences */
		if (!IdHelper.isValidId(name))
			throw new IllegalArgumentException("invalid name; please use only ascii chars (see IdHelper)");
		// set name
		this.name = name;

		// initialize ZooKeeper watcher after name has been set
		monitor = new DeferredProcessingMonitor();

		// immediately activate the service
		activate();

		// start in the connected state
		connected = true;
	}

	/**
	 * Activates the specified node.
	 * <p>
	 * This method is triggered when a node was added to the local tree and the
	 * service should process events for the node.
	 * </p>
	 * 
	 * @param node
	 *            the node to activate
	 */
	public final void activateNode(final ZooKeeperBasedPreferences node) {
		// put to the active nodes map
		activeNodesByPath.put(node.zkPath, node);
	}

	final void checkClosed() {
		if (isClosed())
			throw new IllegalStateException(CLOSED);
	}

	/**
	 * Deactivates the specified node.
	 * <p>
	 * This method is triggered when a node is removed from the local tree and
	 * the service should no longer process events for the node.
	 * </p>
	 * 
	 * @param node
	 *            the node to de-activate
	 */
	public final void deactivateNode(final ZooKeeperBasedPreferences node) {
		// simply remove from the active nodes map
		// (but only the specified instance)
		activeNodesByPath.remove(node.zkPath, node);
	}

	@Override
	protected void disconnect() {
		if (CloudDebug.zooKeeperPreferencesSync) {
			LOG.debug("Disconnecting preferences {}", this);
		}

		// just set disconnected
		connected = false;
	}

	@Override
	protected void doClose() {
		if (CloudDebug.zooKeeperPreferencesSync) {
			LOG.trace("Stack for doClose of service {}", this, new Exception("doClose"));
		}

		// set disconnected
		connected = false;

		// shutdown event processor
		monitor.processEventsThread.interrupt();
		monitor.events.clear();

		// invalidate all nodes
		final Collection<ZooKeeperBasedPreferences> values = activeNodesByPath.values();
		for (final ZooKeeperBasedPreferences preferences : values) {
			preferences.dispose();
		}

		// clear active nodes
		activeNodesByPath.clear();
	}

	final String getChildPath(final String path, final String childName) {
		if (childName.indexOf(IPath.SEPARATOR) > -1)
			throw new IllegalArgumentException("invalid child name: " + childName);

		return path + IPath.SEPARATOR + childName;
	}

	final String getParentPath(final String path) {
		// note, although there is a performance overhead we'll use path and properly check
		// that the parent is still  within the preference hierarchy, i.e.
		// must have at least global preference root + 1 minimum segments
		final IPath parent = new Path(path);

		// abort if below preference root
		if (parent.segmentCount() <= MINIMUM_SEGMENT_COUNT)
			return null;

		return parent.removeLastSegments(1).toString();
	}

	@Override
	protected String getToStringDetails() {
		final StringBuilder details = new StringBuilder();
		details.append(name);
		return details.toString();
	}

	/**
	 * Returns the node remote versions (aka. properties version) of the
	 * specified path in ZooKeeper.
	 * 
	 * @param path
	 *            the ZooKeeper path of the preference node to remove
	 * @return the version information (may be <code>null</code> if the path
	 *         does not exist remotely)
	 */
	public final Stat getVersionInfo(final String path) throws Exception {
		checkClosed();

		return execute(new GetVersionInfo(path));
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

	/**
	 * Loads the specified node.
	 * <p>
	 * This method ensures that essential watchers are registered in ZooKeeper
	 * and the local node (if active) is properly populated with information
	 * from ZooKeeper.
	 * </p>
	 * 
	 * @param path
	 *            path to the node to load
	 * @param forceIfNotConnected
	 *            if the service is not connected an load attempt will be forced
	 *            (which may result in exceptions thrown by ZooKeeper)
	 * @throws Exception
	 */
	public final void loadNode(final String path, final boolean forceIfNotConnected) throws Exception {
		checkClosed();

		if (CloudDebug.zooKeeperPreferences) {
			LOG.trace("Stack for loadNode request for node {}.", path, new Exception("Stack for preference node load request."));
		}

		// connect the node if possible or required
		if (connected || forceIfNotConnected) {
			try {
				// connect the node (i.e. hook essential watchers)
				execute(new LoadNode(path));
			} catch (final ConnectionLossException e) {
				if (forceIfNotConnected)
					throw e;
				// ignore (will re-connect when gate comes back)
				if (CloudDebug.zooKeeperPreferences) {
					LOG.debug("Ignored exception loading node at {}: {} ", new Object[] { path, ExceptionUtils.getRootCauseMessage(e), e });
				}
			} catch (final SessionExpiredException e) {
				if (forceIfNotConnected)
					throw e;
				// ignore (will re-connect when gate comes back)
				if (CloudDebug.zooKeeperPreferences) {
					LOG.debug("Ignored exception loading node at {}: {} ", new Object[] { path, ExceptionUtils.getRootCauseMessage(e), e });
				}
			}
		} else {
			// ignore (will re-connect when gate comes back)
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Not loading node at {}: DISCONNECTED ({})", path, this);
			}
		}
	}

	@Override
	protected void reconnect() {
		if (CloudDebug.zooKeeperPreferencesSync) {
			LOG.debug("Reconnecting preferences {}", this);
		}

		// set connected
		connected = true;

		// re-connect all available nodes
		submit(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				final HashSet<String> paths = new HashSet<String>(activeNodesByPath.keySet());
				for (final String path : paths) {
					try {
						loadNode(path, true);
					} catch (final Exception e) {
						LOG.debug("Ignored exception re-connecting {}: {} ", new Object[] { path, ExceptionUtils.getRootCauseMessage(e), e });
					}
				}
				return null;
			}
		});
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
		if (!activeNodesByPath.containsKey(path))
			return;

		if (CloudDebug.zooKeeperPreferences) {
			LOG.trace("Stack for refreshChildren request for node {}.", path, new Exception("refreshChildren"));
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
		if (!activeNodesByPath.containsKey(path))
			return;

		if (CloudDebug.zooKeeperPreferences) {
			LOG.trace("Stack for refreshProperties request for node {}.", path, new Exception("refreshProperties"));
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
	 * @param propertiesVersion
	 *            the last known version of the node properties (will be used
	 *            for detecting concurrent node modification which may conflict
	 *            with the removal)
	 * @param childrenVersion
	 *            the last known version of the node children (will be used for
	 *            detecting concurrent node modification which may conflict with
	 *            the removal)
	 */
	public final void removeNode(final String path, final int propertiesVersion, final int childrenVersion) throws Exception {
		checkClosed();

		if (CloudDebug.zooKeeperPreferences) {
			LOG.trace("Stack for removeNode request for node {}.", path, new Exception("removeNode"));
		}

		execute(new RemoveNode(path, propertiesVersion, childrenVersion));
	}

	/**
	 * Closes the service.
	 */
	public final void shutdown() {
		close();
	}

	/**
	 * Synchronized the ZooKeeper client with the ZooKeeper leader service.
	 * 
	 * @param path
	 *            the ZooKeeper path of the preference node to sync
	 * @throws Exception
	 */
	public void sync(final String path) throws Exception {
		checkClosed();

		execute(new Sync(path));
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
		if (!activeNodesByPath.containsKey(path))
			return -1;

		if (CloudDebug.zooKeeperPreferences) {
			LOG.trace("Stack for writeProperties request for node {}.", path, new Exception("writeProperties"));
		}

		return execute(new WriteProperties(path, propertyBytes, propertiesVersion));
	}
}
