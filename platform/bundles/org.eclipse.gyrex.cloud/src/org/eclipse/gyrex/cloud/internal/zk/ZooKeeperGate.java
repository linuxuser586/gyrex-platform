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
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central class within Gyrex to be used for all ZooKeeper related
 * communication.
 * <p>
 * Currently, this class capsulates the connection management. However, it might
 * be better to have a more de-coupled model and use events (connected --> gate
 * up; disconnected --> gate down).
 * </p>
 * <p>
 * On the other hand, each caller has to deal with gate up/down situations. It
 * might likely be necessary to allow registering of arbitrary gate-up/connected
 * listeners and maybe clean-up/disconnected listeners.
 * </p>
 */
public class ZooKeeperGate {

	/**
	 * Public connection listeners.
	 */
	public static interface ConnectionMonitor {

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
		public void handleException(final Throwable exception) {
			LOG.warn("Removing bogous connection listener {} due to exception ({}).", listener, exception.toString());
			removeConnectionMonitor((ConnectionMonitor) listener);
		}

		@Override
		public void run() throws Exception {
			if (connected) {
				((ConnectionMonitor) listener).connected();
			} else {
				((ConnectionMonitor) listener).disconnected();
			}
		}
	}

	static interface ShutdownListener {
		void gateDown();
	}

	private static final ListenerList connectionListeners = new ListenerList(ListenerList.IDENTITY);
	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperGate.class);

	private static final AtomicReference<ZooKeeperGate> instanceRef = new AtomicReference<ZooKeeperGate>();
	private static final AtomicBoolean connected = new AtomicBoolean();

	/**
	 * Adds a connection monitor.
	 * <p>
	 * If the gate is currently UP, the {@link ConnectionMonitor#connected()}
	 * will be called as part of the registration.
	 * </p>
	 * 
	 * @param connectionMonitor
	 */
	public static void addConnectionMonitor(final ConnectionMonitor connectionMonitor) {
		// add listener first
		connectionListeners.add(connectionMonitor);

		// notify
		if (connected.get()) {
			SafeRunner.run(new NotifyConnectionListener(true, connectionMonitor));
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
		return instanceRef.getAndSet(gate);
	}

	/**
	 * Removed a connection monitor.
	 * <p>
	 * If the gate is currently UP, the {@link ConnectionMonitor#disconnected()}
	 * will be called as part of the registration.
	 * </p>
	 * 
	 * @param connectionMonitor
	 */
	public static void removeConnectionMonitor(final ConnectionMonitor connectionMonitor) {
		// get state first (to ensure that we call a disconnect)
		final boolean notify = connected.get();

		// remove listener
		connectionListeners.remove(connectionMonitor);

		// notify
		if (notify) {
			SafeRunner.run(new NotifyConnectionListener(false, connectionMonitor));
		}
	}

	private final ShutdownListener listener;
	private final ZooKeeper zooKeeper;

	private final Watcher connectionMonitor = new Watcher() {
		@Override
		public void process(final WatchedEvent event) {
			if (CloudDebug.zooKeeperGateLifecycle) {
				LOG.debug("Connection event: {}", event);
			}

			if (event.getState() == KeeperState.SyncConnected) {
				LOG.info("ZooKeeper Gate is now UP. Connection to cloud established.");
				connected.set(true);
				final Object[] listeners = connectionListeners.getListeners();
				for (final Object listener : listeners) {
					SafeRunner.run(new NotifyConnectionListener(true, listener));
				}
			} else {
				LOG.info("ZooKeeper Gate is now DOWN. Connection to cloud lost ({}).", event.getState());
				connected.set(false);
				final Object[] listeners = connectionListeners.getListeners();
				for (final Object listener : listeners) {
					SafeRunner.run(new NotifyConnectionListener(false, listener));
				}
			}
		}
	};

	ZooKeeperGate(final ZooKeeperGateConfig config, final ShutdownListener listener) throws IOException {
		zooKeeper = new ZooKeeper(config.getConnectString(), config.getSessionTimeout(), connectionMonitor);
		this.listener = listener;
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
	 * Creates a path in ZooKeeper
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
	 * Creates a record at the specified path in ZooKeeper if one doesn't exist.
	 * <p>
	 * If the path parents don't exist they will be created using the specified
	 * creation mode.
	 * </p>
	 * <p>
	 * If a record already exists, an exception will be thrown.
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
	public void createRecord(final IPath path, final CreateMode createMode, final byte[] recordData) throws KeeperException, InterruptedException, IOException {
		if (recordData == null) {
			throw new IllegalArgumentException("recordData must not be null");
		}
		create(path, createMode, recordData);
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
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void createRecord(final IPath path, final CreateMode createMode, final String recordData) throws KeeperException, InterruptedException, IOException {
		if (recordData == null) {
			throw new IllegalArgumentException("recordData must not be null");
		}
		try {
			createRecord(path, createMode, recordData.getBytes(CharEncoding.UTF_8));
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
	 * @return the record data (maybe <code>null</code> if it doesn't exist)
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public byte[] readRecord(final IPath path) throws KeeperException, InterruptedException, IOException {
		return readRecord(path, (Watcher) null);
	}

	/**
	 * Reads a record from the specified path in ZooKeeper if it exists.
	 * 
	 * @param path
	 *            the path to the record
	 * @param defaultValue
	 *            a default value to return the record does not exist.
	 * @return the record data (maybe <code>null</code> if
	 *         <code>defaultValue</code> was <code>null</code>)
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public String readRecord(final IPath append, final String defaultValue) throws KeeperException, InterruptedException, IOException {
		final byte[] data = readRecord(append);
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
	 * @return the record data (maybe <code>null</code> if it doesn't exist)
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 * @see {@link ZooKeeper#getData(String, Watcher, org.apache.zookeeper.data.Stat)}
	 */
	public byte[] readRecord(final IPath path, final Watcher watch) throws KeeperException, InterruptedException, IOException {
		if (path == null) {
			throw new IllegalArgumentException("path must not be null");
		}
		try {
			return ensureConnected().getData(path.toString(), watch, null);
		} catch (final KeeperException e) {
			if (e.code() == KeeperException.Code.NONODE) {
				return null;
			}
			throw e;
		}
	}

	private void setData(final IPath path, final CreateMode createMode, final byte[] data) throws InterruptedException, KeeperException, IOException {
		if (path == null) {
			throw new IllegalArgumentException("path must not be null");
		}
		if (createMode == null) {
			throw new IllegalArgumentException("createMode must not be null");
		}

		if (!exists(path)) {
			try {
				create(path, createMode, data);
			} catch (final KeeperException e) {
				if (e.code() != KeeperException.Code.NODEEXISTS) {
					// rethrow
					throw e;
				}
			}
		}

		// set data
		ensureConnected().setData(path.toString(), data, -1);
	}

	/**
	 * Closes the gate.
	 */
	public void shutdown() {
		if (CloudDebug.zooKeeperGateLifecycle) {
			LOG.debug("Received stop signal for ZooKeeper Gate.");
		}

		try {
			zooKeeper.close();
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (final Exception e) {
			// ignored shutdown exceptions
			e.printStackTrace();
		}

		// notify listeners
		listener.gateDown();
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
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void writeRecord(final IPath path, final CreateMode createMode, final byte[] recordData) throws KeeperException, InterruptedException, IOException {
		if (recordData == null) {
			throw new IllegalArgumentException("recordData must not be null");
		}
		setData(path, createMode, recordData);
	}
}
