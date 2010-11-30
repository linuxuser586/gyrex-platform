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
package org.eclipse.gyrex.cloud.internal.zk;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.cloud.internal.CloudDebug;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;

import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central class within Gyrex to be used for all ZooKeeper related
 * communication.
 * <p>
 * Currently, this class capsulates the connection management and offers clients
 * to be notified about connection events.
 * </p>
 * <p>
 * Note, this class is not API. It's ZooKeeper specific. Patches are welcome to
 * restructure the Gyrex cloud stuff. A first step would require to refactor
 * everything ZooKeeper specific into a separate "implementation" bundle. Based
 * on this a cloud API could be established.
 * </p>
 */
public class ZooKeeperGate {

	/**
	 * Public connection listeners.
	 */
	public static interface IConnectionMonitor {

		/**
		 * the connection has been established
		 */
		void connected();

		/**
		 * the connection has been closed
		 */
		void disconnected();
	}

	/**
	 * Notifies a connection listener
	 */
	private static final class NotifyConnectionListener implements ISafeRunnable {
		private final boolean connected;
		private final Object listener;

		/**
		 * Creates a new instance.
		 * 
		 * @param connected
		 * @param listener
		 * @param gate
		 */
		private NotifyConnectionListener(final boolean connected, final Object listener) {
			this.connected = connected;
			this.listener = listener;
		}

		@Override
		public void handleException(final Throwable e) {
			// log error
			LOG.error("Removing bogous connection listener {} due to exception ({}).", new Object[] { listener, ExceptionUtils.getMessage(e), e });
			// remove listener directly
			connectionListeners.remove(listener);
		}

		@Override
		public void run() throws Exception {
			if (connected) {
				((IConnectionMonitor) listener).connected();
			} else {
				((IConnectionMonitor) listener).disconnected();
			}
		}
	}

	private static final ListenerList connectionListeners = new ListenerList(ListenerList.IDENTITY);
	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperGate.class);

	private static final AtomicReference<ZooKeeperGate> instanceRef = new AtomicReference<ZooKeeperGate>();
	private static final AtomicBoolean connected = new AtomicBoolean();

	/**
	 * Adds a connection monitor.
	 * <p>
	 * If the gate is currently UP, the {@link IConnectionMonitor#connected()}
	 * will be called as part of the registration.
	 * </p>
	 * <p>
	 * This method has no effect if the monitor is already registered
	 * </p>
	 * 
	 * @param iConnectionMonitor
	 *            the monitor to register
	 */
	public static void addConnectionMonitor(final IConnectionMonitor iConnectionMonitor) {
		// add listener first
		connectionListeners.add(iConnectionMonitor);

		// notify
		if (connected.get()) {
			SafeRunner.run(new NotifyConnectionListener(true, iConnectionMonitor));
		}
	}

	/**
	 * Returns the current active gate.
	 * 
	 * @return the active gate
	 * @throws IllegalStateException
	 *             if the gate is DOWN
	 */
	public static ZooKeeperGate get() throws IllegalStateException {
		final ZooKeeperGate gate = instanceRef.get();
		if (gate == null) {
			throw new IllegalStateException("ZooKeeper Gate is DOWN.");
		}
		return gate;
	}

	static ZooKeeperGate getAndSet(final ZooKeeperGate gate) {
		final ZooKeeperGate old = instanceRef.getAndSet(gate);
		if (CloudDebug.zooKeeperGateLifecycle) {
			LOG.debug("Set new ZooKeeper Gate instance. {} (old {})", new Object[] { gate, old });
		}
		return old;
	}

	static boolean isCurrentGate(final ZooKeeperGate gate) {
		return (gate != null) && (gate == instanceRef.get());
	}

	/**
	 * Removed a connection monitor.
	 * <p>
	 * If the gate is currently UP, the
	 * {@link IConnectionMonitor#disconnected()} will be called as part of the
	 * registration.
	 * </p>
	 * <p>
	 * This method has no effect if the monitor is not registered
	 * </p>
	 * 
	 * @param iConnectionMonitor
	 *            the monitor to unregister
	 */
	public static void removeConnectionMonitor(final IConnectionMonitor iConnectionMonitor) {
		// get state first (to ensure that we call a disconnect)
		final boolean notify = connected.get();

		// remove listener
		connectionListeners.remove(iConnectionMonitor);

		// notify
		if (notify) {
			SafeRunner.run(new NotifyConnectionListener(false, iConnectionMonitor));
		}
	}

	private final ZooKeeper zooKeeper;
	private final IConnectionMonitor reconnectMonitor;
	private final Watcher connectionMonitor = new Watcher() {

		@Override
		public void process(final WatchedEvent event) {
			// only process event if we are the active gate
			if (!isCurrentGate(ZooKeeperGate.this)) {
				if (CloudDebug.zooKeeperGateLifecycle) {
					LOG.debug("Ignored connection event for inactive gate: {}, {}", this, event);
				}
			}

			// log message
			if (CloudDebug.zooKeeperGateLifecycle) {
				LOG.debug("Connection event: {}", event);
			}

			// handle event
			if (event.getState() == KeeperState.SyncConnected) {
				// SyncConnected ==> connection is UP
				LOG.info("ZooKeeper Gate is now UP. Connection to cloud established.");
				connected.set(true);

				// notify connection listeners
				fireConnectionEvent(true);
			} else if (event.getState() == KeeperState.Expired) {
				// we rely on Expired event for real DOWN detection
				LOG.info("ZooKeeper Gate is now DOWN. Connection to cloud lost.", event.getState());
				connected.set(false);

				// trigger clean shutdown (and notify listeners)
				shutdown(true);
			} else {
				// ZooKeeper will re-try on it's own in all other cases
				LOG.info("ZooKeeper is now {}. Gate is not intervening. ({})", event.getState(), zooKeeper);
			}
		}

	};

	ZooKeeperGate(final ZooKeeperGateConfig config, final IConnectionMonitor reconnectMonitor) throws IOException {
		this.reconnectMonitor = reconnectMonitor;
		zooKeeper = new ZooKeeper(config.getConnectString(), config.getSessionTimeout(), connectionMonitor);
		if (CloudDebug.zooKeeperGateLifecycle) {
			LOG.debug("New ZooKeeper Gate instance. {}", this, new Exception("ZooKeeper Gate Constructor Call Stack"));
		}
	}

	private void create(final IPath path, final CreateMode createMode, final byte[] data) throws InterruptedException, KeeperException, IOException {
		if (path == null) {
			throw new IllegalArgumentException("path must not be null");
		}
		if (createMode == null) {
			throw new IllegalArgumentException("createMode must not be null");
		}

		// create all parents
		for (int i = path.segmentCount() - 1; i > 0; i--) {
			final IPath parentPath = path.removeLastSegments(i);
			try {
				ensureConnected().create(parentPath.toString(), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			} catch (final KeeperException e) {
				if (e.code() != KeeperException.Code.NODEEXISTS) {
					// rethrow
					throw e;
				}
			}
		}

		// create node itself
		ensureConnected().create(path.toString(), data, ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
	}

	/**
	 * Creates a path in ZooKeeper.
	 * <p>
	 * If the path parents don't exist they will be created using
	 * {@link CreateMode#PERSISTENT}.
	 * </p>
	 * 
	 * @param path
	 *            the path to create
	 * @param createMode
	 *            the creation mode
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void createPath(final IPath path, final CreateMode createMode) throws KeeperException, InterruptedException, IOException {
		create(path, createMode, null);
	}

	/**
	 * Creates a path in ZooKeeper and sets the specified data.
	 * <p>
	 * If the path parents don't exist they will be created using
	 * {@link CreateMode#PERSISTENT}.
	 * </p>
	 * 
	 * @param path
	 *            the path to create
	 * @param createMode
	 *            the creation mode
	 * @param recordData
	 *            the record data
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void createPath(final IPath path, final CreateMode createMode, final byte[] recordData) throws KeeperException, InterruptedException, IOException {
		if (recordData == null) {
			throw new IllegalArgumentException("recordData must not be null");
		}
		create(path, createMode, recordData);
	}

	/**
	 * Creates a path in ZooKeeper and sets the specified data.
	 * <p>
	 * If the path parents don't exist they will be created using
	 * {@link CreateMode#PERSISTENT}.
	 * </p>
	 * 
	 * @param path
	 *            the path to create
	 * @param createMode
	 *            the creation mode
	 * @param recordData
	 *            the record data
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void createPath(final IPath path, final CreateMode createMode, final String recordData) throws KeeperException, InterruptedException, IOException {
		if (recordData == null) {
			throw new IllegalArgumentException("recordData must not be null");
		}
		try {
			createPath(path, createMode, recordData.getBytes(CharEncoding.UTF_8));
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("JVM does not support UTF-8.", e);
		}
	}

	/**
	 * Removes a path in ZooKeeper.
	 * <p>
	 * If the path parents don't exist they will be created using
	 * {@link CreateMode#PERSISTENT}.
	 * </p>
	 * 
	 * @param path
	 *            the path to create
	 * @param createMode
	 *            the creation mode
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void deletePath(final IPath path) throws KeeperException, InterruptedException, IOException {
		if (path == null) {
			throw new IllegalArgumentException("path must not be null");
		}

		// delete all children
		final List<String> children = ensureConnected().getChildren(path.toString(), false);
		for (final String child : children) {
			deletePath(path.append(child));
		}

		// delete node itself
		ensureConnected().delete(path.toString(), -1);
	}

	final ZooKeeper ensureConnected() {
		if (!zooKeeper.getState().isAlive()) {
			throw new IllegalStateException("ZooKeeper Gate is DOWN.");
		}
		return zooKeeper;
	}

	/**
	 * Checks if the specified path exists.
	 * 
	 * @param path
	 *            the path to create
	 * @return <code>true</code> if the path exists, <code>false</code>
	 *         otherwise
	 * @throws InterruptedException
	 * @throws KeeperException
	 */
	public boolean exists(final IPath path) throws InterruptedException, KeeperException {
		return exists(path, null);
	}

	/**
	 * Checks if the specified path exists.
	 * 
	 * @param path
	 *            the path to create
	 * @return <code>true</code> if the path exists, <code>false</code>
	 *         otherwise
	 * @throws InterruptedException
	 * @throws KeeperException
	 */
	public boolean exists(final IPath path, final ZooKeeperMonitor monitor) throws InterruptedException, KeeperException {
		if (path == null) {
			throw new IllegalArgumentException("path must not be null");
		}
		try {
			return ensureConnected().exists(path.toString(), monitor) != null;
		} catch (final KeeperException e) {
			throw e;
		}
	}

	void fireConnectionEvent(final boolean connected) {
		// notify registered listeners
		final Object[] listeners = connectionListeners.getListeners();
		for (final Object listener : listeners) {
			SafeRunner.run(new NotifyConnectionListener(connected, listener));
		}

		// notify reconnect listener
		SafeRunner.run(new NotifyConnectionListener(connected, reconnectMonitor));
	}

	/**
	 * Reads the list of children from the specified path in ZooKeeper if it
	 * exists.
	 * 
	 * @param path
	 *            the path to the record
	 * @return the list of children (maybe <code>null</code> the path doesn't
	 *         exist)
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public Collection<String> readChildrenNames(final IPath path) throws InterruptedException, KeeperException {
		return readChildrenNames(path, null);
	}

	/**
	 * Reads the list of children from the specified path in ZooKeeper if it
	 * exists.
	 * 
	 * @param path
	 *            the path to the record
	 * @param watch
	 *            optional watch to set (may be <code>null</code>)
	 * @return the list of children (maybe <code>null</code> the path doesn't
	 *         exist)
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 * @see {@link ZooKeeper#getChildren(String, Watcher)}
	 */
	public Collection<String> readChildrenNames(final IPath path, final Watcher watch) throws InterruptedException, KeeperException {
		if (path == null) {
			throw new IllegalArgumentException("path must not be null");
		}
		try {
			return ensureConnected().getChildren(path.toString(), watch);
		} catch (final KeeperException e) {
			if (e.code() == KeeperException.Code.NONODE) {
				return null;
			}
			throw e;
		}
	}

	/**
	 * Reads a record from the specified path in ZooKeeper if it exists.
	 * 
	 * @param path
	 *            the path to the record
	 * @param stat
	 *            optional object to populated with ZooKeeper statistics of the
	 *            underlying node
	 * @return the record data (maybe <code>null</code> if it doesn't exist)
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public byte[] readRecord(final IPath path, final Stat stat) throws KeeperException, InterruptedException, IOException {
		return readRecord(path, (Watcher) null, stat);
	}

	/**
	 * Reads a record from the specified path in ZooKeeper if it exists.
	 * 
	 * @param path
	 *            the path to the record
	 * @param defaultValue
	 *            a default value to return the record does not exist.
	 * @param stat
	 *            optional object to populated with ZooKeeper statistics of the
	 *            underlying node
	 * @return the record data (maybe <code>null</code> if
	 *         <code>defaultValue</code> was <code>null</code>)
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public String readRecord(final IPath path, final String defaultValue, final Stat stat) throws KeeperException, InterruptedException, IOException {
		final byte[] data = readRecord(path, stat);
		if (data == null) {
			return defaultValue;
		}
		try {
			return new String(data, CharEncoding.UTF_8);
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("JVM does not support UTF-8.", e);
		}
	}

	/**
	 * Reads a record from the specified path in ZooKeeper if it exists.
	 * 
	 * @param path
	 *            the path to the record
	 * @param watch
	 *            optional watch to set (may be <code>null</code>)
	 * @param stat
	 *            optional object to populated with ZooKeeper statistics of the
	 *            underlying node
	 * @return the record data (maybe <code>null</code> if it doesn't exist)
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 * @see {@link ZooKeeper#getData(String, Watcher, org.apache.zookeeper.data.Stat)}
	 */
	public byte[] readRecord(final IPath path, final Watcher watch, final Stat stat) throws KeeperException, InterruptedException, IOException {
		if (path == null) {
			throw new IllegalArgumentException("path must not be null");
		}
		try {
			return ensureConnected().getData(path.toString(), watch, stat);
		} catch (final KeeperException e) {
			if (e.code() == KeeperException.Code.NONODE) {
				return null;
			}
			throw e;
		}
	}

	private Stat setDataOrCreate(final IPath path, final CreateMode createMode, final byte[] data) throws InterruptedException, KeeperException, IOException {
		if (path == null) {
			throw new IllegalArgumentException("path must not be null");
		}
		if (createMode == null) {
			throw new IllegalArgumentException("createMode must not be null");
		}

		if (!exists(path)) {
			try {
				create(path, createMode, data);

				// create succeeded, continue with set below
			} catch (final KeeperException e) {
				if (e.code() != KeeperException.Code.NODEEXISTS) {
					// rethrow
					throw e;
				}
			}
		}

		// set data
		return ensureConnected().setData(path.toString(), data, -1);
	}

	/**
	 * Closes the gate.
	 * 
	 * @param notify
	 *            set to <code>true</code> in notify registered connection
	 *            listeners
	 */
	void shutdown(final boolean notify) {
		if (CloudDebug.zooKeeperGateLifecycle) {
			LOG.debug("Shutdown of ZooKeeper Gate. {}", this, new Exception("ZooKeeper Gate Shutdown Call Stack"));
		}

		// notify listeners
		if (notify) {
			fireConnectionEvent(false);
		}

		try {
			zooKeeper.close();
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (final Exception e) {
			// ignored shutdown exceptions
			if (CloudDebug.zooKeeperGateLifecycle) {
				LOG.debug("Ignored exception during shutdown: {}", e.getMessage(), e);
			}
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ZooKeeperGate [current=");
		builder.append(isCurrentGate(this));
		builder.append(", zk=");
		builder.append(zooKeeper);
		builder.append("]");
		return builder.toString();
	}

	/**
	 * Writes a record at the specified path in ZooKeeper.
	 * <p>
	 * If the path (or any of its parents) doesn't exist it will be created
	 * using the specified creation mode.
	 * </p>
	 * 
	 * @param path
	 *            the path to create
	 * @param createMode
	 *            the creation mode
	 * @param recordData
	 *            the record data
	 * @return ZooKeeper statistics about the underlying node
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public Stat writeRecord(final IPath path, final CreateMode createMode, final byte[] recordData) throws KeeperException, InterruptedException, IOException {
		if (recordData == null) {
			throw new IllegalArgumentException("recordData must not be null");
		}
		return setDataOrCreate(path, createMode, recordData);
	}

	/**
	 * Creates a record at the specified path in ZooKeeper.
	 * <p>
	 * If the path parents don't exist they will be created using the specified
	 * creation mode.
	 * </p>
	 * 
	 * @param path
	 *            the path to create
	 * @param createMode
	 *            the creation mode
	 * @param recordData
	 *            the record data
	 * @return ZooKeeper statistics about the underlying node
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public Stat writeRecord(final IPath path, final CreateMode createMode, final String recordData) throws KeeperException, InterruptedException, IOException {
		if (recordData == null) {
			throw new IllegalArgumentException("recordData must not be null");
		}
		try {
			return writeRecord(path, createMode, recordData.getBytes(CharEncoding.UTF_8));
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("JVM does not support UTF-8.", e);
		}
	}
}
