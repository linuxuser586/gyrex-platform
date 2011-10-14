/*******************************************************************************
 * Copyright (c) 2010, 2011 AGETO Service GmbH and others.
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
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SafeRunner;

import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.BadVersionException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
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
	// TODO: implement RECOVERING state and rename to ZooKeeperGateListener
	public static interface IConnectionMonitor {

		/**
		 * the connection has been established
		 * 
		 * @param gate
		 *            the gate which have been connected
		 */
		void connected(ZooKeeperGate gate);

		/**
		 * the connection has been closed
		 * 
		 * @param gate
		 *            the gate which have been disconnected
		 */
		void disconnected(ZooKeeperGate gate);
	}

	/**
	 * Notifies a connection listener
	 */
	private static final class NotifyConnectionListener implements ISafeRunnable {
		private final boolean connected;
		private final Object listener;
		private final ZooKeeperGate gate;

		/**
		 * Creates a new instance.
		 * 
		 * @param connected
		 * @param listener
		 * @param gate
		 * @param gate
		 */
		private NotifyConnectionListener(final boolean connected, final Object listener, final ZooKeeperGate gate) {
			this.connected = connected;
			this.listener = listener;
			this.gate = gate;
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
				((IConnectionMonitor) listener).connected(gate);
			} else {
				((IConnectionMonitor) listener).disconnected(gate);
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
	 * If the gate is currently UP, the
	 * {@link IConnectionMonitor#connected(ZooKeeperGate)} will be called as
	 * part of the registration.
	 * </p>
	 * <p>
	 * This method has no effect if the monitor is already registered or the
	 * specified monitor is <code>null</code>
	 * </p>
	 * 
	 * @param connectionMonitor
	 *            the monitor to register (may be <code>null</code>)
	 */
	public static void addConnectionMonitor(final IConnectionMonitor connectionMonitor) {
		// ignore null monitors
		if (connectionMonitor == null) {
			return;
		}

		// add listener first
		connectionListeners.add(connectionMonitor);

		// notify
		if (connected.get()) {
			SafeRunner.run(new NotifyConnectionListener(true, connectionMonitor, instanceRef.get()));
		}
	}

	private static String gateDownError(final ZooKeeperGate gate) {
		try {
			return String.format("ZooKeeper Gate is DOWN. (%s)", String.valueOf(gate));
		} catch (final Throwable e) {
			if ((e instanceof VirtualMachineError) || (e instanceof LinkageError)) {
				throw (Error) e;
			}
			return String.format("ZooKeeper Gate is DOWN. (%s)", ExceptionUtils.getRootCauseMessage(e));
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
			throw new IllegalStateException(gateDownError(null));
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
	 * {@link IConnectionMonitor#disconnected(ZooKeeperGate)} will be called as
	 * part of the registration.
	 * </p>
	 * <p>
	 * This method has no effect if the monitor is not registered
	 * </p>
	 * 
	 * @param connectionMonitor
	 *            the monitor to unregister (must not be <code>null</code>)
	 */
	public static void removeConnectionMonitor(final IConnectionMonitor connectionMonitor) {
		// ignore null monitor
		if (connectionMonitor == null) {
			throw new IllegalArgumentException("connection monitor must not be null");
		}

		// get state first (to ensure that we call a disconnect)
		final boolean notify = connected.get();

		// remove listener
		connectionListeners.remove(connectionMonitor);

		// notify
		if (notify) {
			SafeRunner.run(new NotifyConnectionListener(false, connectionMonitor, instanceRef.get()));
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
			switch (event.getState()) {
				case SyncConnected:
					// SyncConnected ==> connection is UP
					// TODO: should build some debug details and print it (such as node connected to, session id)
					LOG.info("ZooKeeper Gate is now UP. Connection to ZooKeeper established.");
					connected.set(true);

					// notify connection listeners
					fireConnectionEvent(true);
					break;

				case Expired:
				case Disconnected:
					// Expired || Disconnected ==> connection is down
					// TODO: implement RECOVERING state
					LOG.info("ZooKeeper Gate is now DOWN. Connection to ZooKeeper lost.");
					connected.set(false);

					// TODO: there is room for improvement here
					// should we try to silently re-establish a session?
					// if that fails we can still bring down the node completely

					// another issue are "Disconnected" events
					// they means connection issues, but a session including its
					// ephemeral nodes might still be valid on the server;
					// plus, ZK automatically tries to re-connect
					// however, any watches might have gone away
					// a delayed notify might be interesting, because technically we
					// don't need to bring the whole gate down on "Disconnected", it doesn't
					// rely on watches but other code down the road might (eg. locks)

					// TODO: we need to clarify and better document our decision here

					// trigger clean shutdown (and notify listeners)
					shutdown(true);
					break;

				case AuthFailed:
					// AuthFailed ==> impossible to connect
					LOG.warn("ZooKeeper Gate is unable to connect. Authentication faild.");
					connected.set(false);

					// trigger clean shutdown (but don't notify listeners because they should never got notified before)
					shutdown(false);
					break;

				default:
					// ZooKeeper will re-try on it's own in all other cases
					LOG.warn("ZooKeeper is now {}. Gate is not intervening. ({})", event.getState(), zooKeeper);
					break;
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

	private IPath create(final IPath path, final CreateMode createMode, final byte[] data) throws InterruptedException, KeeperException, IOException {
		if (path == null) {
			throw new IllegalArgumentException("path must not be null");
		}
		if (createMode == null) {
			throw new IllegalArgumentException("createMode must not be null");
		}

		// create all parents
		ZooKeeperHelper.createParents(ensureConnected(), path);

		// create node itself
		return new Path(ensureConnected().create(path.toString(), data, ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode));
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
	 * @return the actual path of the created node
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public IPath createPath(final IPath path, final CreateMode createMode) throws KeeperException, InterruptedException, IOException {
		return create(path, createMode, null);
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
	 * @return the actual path of the created node
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public IPath createPath(final IPath path, final CreateMode createMode, final byte[] recordData) throws KeeperException, InterruptedException, IOException {
		if (recordData == null) {
			throw new IllegalArgumentException("recordData must not be null");
		}
		return create(path, createMode, recordData);
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
	public IPath createPath(final IPath path, final CreateMode createMode, final String recordData) throws KeeperException, InterruptedException, IOException {
		if (recordData == null) {
			throw new IllegalArgumentException("recordData must not be null");
		}
		try {
			return createPath(path, createMode, recordData.getBytes(CharEncoding.UTF_8));
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("JVM does not support UTF-8.", e);
		}
	}

	/**
	 * Removes a path in ZooKeeper.
	 * <p>
	 * If the path doesn't exist the operation is also considered successful.
	 * Otherwise it behaves as {@link #deletePath(IPath, int)} with a version
	 * value of <code>-1</code>.
	 * </p>
	 * 
	 * @param path
	 *            the path to delete
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 * @see {@link #deletePath(IPath, int)}
	 */
	public void deletePath(final IPath path) throws KeeperException, InterruptedException, IOException {
		if (path == null) {
			throw new IllegalArgumentException("path must not be null");
		}

		try {
			// delete all children
			final List<String> children = ensureConnected().getChildren(path.toString(), false);
			for (final String child : children) {
				deletePath(path.append(child));
			}

			// delete node itself
			ensureConnected().delete(path.toString(), -1);
		} catch (final KeeperException e) {
			if (e.code() != Code.NONODE) {
				throw e;
			}
			// node does not exist
			// we don't care, the result matters
			return;
		}
	}

	/**
	 * Removes a path in ZooKeeper.
	 * <p>
	 * The call will succeed if such a node exists, and the given version
	 * matches the node's version (if the given version is -1, it matches any
	 * node's versions).
	 * </p>
	 * <p>
	 * A KeeperException with error code KeeperException.NoNode will be thrown
	 * if the nodes does not exist.
	 * </p>
	 * <p>
	 * A KeeperException with error code KeeperException.BadVersion will be
	 * thrown if the given version does not match the node's version.
	 * </p>
	 * 
	 * @param path
	 *            the path to delete
	 * @param version
	 *            the expected node version
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 * @see {@link ZooKeeper#delete(String, int)}
	 */
	public void deletePath(final IPath path, final int version) throws InterruptedException, IOException, KeeperException {
		if (path == null) {
			throw new IllegalArgumentException("path must not be null");
		}

		// delete all children
		final List<String> children = ensureConnected().getChildren(path.toString(), false);
		for (final String child : children) {
			deletePath(path.append(child));
		}

		// delete node itself
		ensureConnected().delete(path.toString(), version);
	}

	final ZooKeeper ensureConnected() {
		if (!zooKeeper.getState().isAlive()) {
			throw new IllegalStateException(gateDownError(this));
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
	 * <p>
	 * If the watch is non-null and the call is successful (no exception is
	 * thrown), a watch will be left on the node with the given path. The watch
	 * will be triggered by a successful operation that creates/delete the node
	 * or sets the data on the node.
	 * </p>
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
			SafeRunner.run(new NotifyConnectionListener(connected, listener, this));
		}

		// notify reconnect listener
		if (reconnectMonitor != null) {
			SafeRunner.run(new NotifyConnectionListener(connected, reconnectMonitor, this));
		}
	}

	/**
	 * Returns the id of the current ZooKeeper session.
	 * <p>
	 * This method is used for debugging purposes and may not be referenced
	 * elsewhere.
	 * </p>
	 * 
	 * @return the session id
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public long getSessionId() {
		return ensureConnected().getSessionId();
	}

	/**
	 * Reads the list of children from the specified path in ZooKeeper.
	 * <p>
	 * A {@link NoNodeException} will be thrown if no node with the given path
	 * exists.
	 * </p>
	 * 
	 * @param path
	 *            the path to the record
	 * @param stat
	 *            optional object to populated with ZooKeeper statistics of the
	 *            underlying node
	 * @return an unordered list of children of the node at the specified path
	 * @throws NoNodeException
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @see {@link ZooKeeper#getChildren(String, ZooKeeperMonitor)}
	 */
	public Collection<String> readChildrenNames(final IPath path, final Stat stat) throws InterruptedException, KeeperException {
		return readChildrenNames(path, null, stat);
	}

	/**
	 * Reads the list of children from the specified path in ZooKeeper.
	 * <p>
	 * A {@link NoNodeException} will be thrown if no node with the given path
	 * exists.
	 * </p>
	 * 
	 * @param path
	 *            the path to the record
	 * @param watch
	 *            optional watch to set (may be <code>null</code>)
	 * @param stat
	 *            optional object to populated with ZooKeeper statistics of the
	 *            underlying node
	 * @return an unordered list of children of the node at the specified path
	 * @throws NoNodeException
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @see {@link ZooKeeper#getChildren(String, ZooKeeperMonitor)}
	 */
	public Collection<String> readChildrenNames(final IPath path, final ZooKeeperMonitor watch, final Stat stat) throws NoNodeException, KeeperException, InterruptedException {
		if (path == null) {
			throw new IllegalArgumentException("path must not be null");
		}
		return ensureConnected().getChildren(path.toString(), watch, stat);
	}

	/**
	 * Reads a record from the specified path in ZooKeeper.
	 * <p>
	 * Throws {@link NoNodeException} if the path does not exists.
	 * </p>
	 * 
	 * @param path
	 *            the path to the record
	 * @param stat
	 *            optional object to populated with ZooKeeper statistics of the
	 *            underlying node
	 * @return the record data (maybe <code>null</code> if no data is stored at
	 *         the specified path)
	 * @throws NoNodeException
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public byte[] readRecord(final IPath path, final Stat stat) throws NoNodeException, KeeperException, InterruptedException, IOException {
		return readRecord(path, (ZooKeeperMonitor) null, stat);
	}

	/**
	 * Reads a record from the specified path in ZooKeeper if it exists.
	 * <p>
	 * Returns the specified {@code defaultValue} if the path does not exists.
	 * </p>
	 * 
	 * @param path
	 *            the path to the record
	 * @param defaultValue
	 *            a default value to return if the record does not exist or no
	 *            data is stored at the specified path.
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
		try {
			final byte[] data = readRecord(path, stat);
			if (data == null) {
				return defaultValue;
			}
			return new String(data, CharEncoding.UTF_8);
		} catch (final NoNodeException e) {
			return defaultValue;
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("JVM does not support UTF-8.", e);
		}
	}

	/**
	 * Reads a record from the specified path in ZooKeeper.
	 * <p>
	 * <p>
	 * Throws {@link NoNodeException} if the path does not exists.
	 * </p>
	 * 
	 * @param path
	 *            the path to the record
	 * @param watch
	 *            optional watch to set (may be <code>null</code>)
	 * @param stat
	 *            optional object to populated with ZooKeeper statistics of the
	 *            underlying node
	 * @return the record data (maybe <code>null</code> if no data is stored at
	 *         the specified path)
	 * @throws NoNodeException
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 * @see {@link ZooKeeper#getData(String, ZooKeeperMonitor, org.apache.zookeeper.data.Stat)}
	 */
	public byte[] readRecord(final IPath path, final ZooKeeperMonitor watch, final Stat stat) throws NoNodeException, KeeperException, InterruptedException, IOException {
		if (path == null) {
			throw new IllegalArgumentException("path must not be null");
		}
		return ensureConnected().getData(path.toString(), watch, stat);
	}

	/**
	 * Sets the data on the specified path.
	 * <p>
	 * Does create the path (including its parents) if <code>createMode</code>
	 * is not <code>null</code>.
	 * </p>
	 * 
	 * @param path
	 * @param createMode
	 * @param data
	 * @param version
	 * @return
	 * @throws InterruptedException
	 * @throws KeeperException
	 * @throws IOException
	 * @see {@link ZooKeeper#setData(String, byte[], int)}
	 */
	private Stat setData(final IPath path, final CreateMode createMode, final byte[] data, final int version) throws InterruptedException, KeeperException, IOException {
		if (path == null) {
			throw new IllegalArgumentException("path must not be null");
		}

		if ((createMode != null) && !exists(path)) {
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
		return ensureConnected().setData(path.toString(), data, version);
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

	/**
	 * Closes the gate.
	 * <p>
	 * This method is used for testing purposes and may not be referenced
	 * elsewhere.
	 * </p>
	 * 
	 * @throws InterruptedException
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void testShutdown() throws InterruptedException {
		shutdown(true);
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
	 * If the path does not exist a {@link NoNodeException} will be thrown.
	 * </p>
	 * <p>
	 * If the version does not match a {@link BadVersionException} will be
	 * thrown.
	 * </p>
	 * 
	 * @param path
	 *            the path to create
	 * @param recordData
	 *            the record data
	 * @param version
	 * @return ZooKeeper statistics about the underlying node
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public Stat writeRecord(final IPath path, final byte[] recordData, final int version) throws InterruptedException, KeeperException, IOException {
		if (recordData == null) {
			throw new IllegalArgumentException("recordData must not be null");
		}
		return setData(path, null, recordData, version);
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
		if (createMode == null) {
			throw new IllegalArgumentException("createMode must not be null");
		}
		return setData(path, createMode, recordData, -1);
	}

	/**
	 * Writes a record at the specified path in ZooKeeper.
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
		if (createMode == null) {
			throw new IllegalArgumentException("createMode must not be null");
		}
		try {
			return writeRecord(path, createMode, recordData.getBytes(CharEncoding.UTF_8));
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("JVM does not support UTF-8.", e);
		}
	}

	/**
	 * Writes a record at the specified path in ZooKeeper.
	 * <p>
	 * If the path does not exist a {@link NoNodeException} will be thrown.
	 * </p>
	 * <p>
	 * If the version does not match a {@link BadVersionException} will be
	 * thrown.
	 * </p>
	 * 
	 * @param path
	 *            the path to create
	 * @param recordData
	 *            the record data
	 * @param version
	 * @return ZooKeeper statistics about the underlying node
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public Stat writeRecord(final IPath path, final String recordData, final int version) throws InterruptedException, KeeperException, IOException {
		try {
			return writeRecord(path, recordData.getBytes(CharEncoding.UTF_8), version);
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("JVM does not support UTF-8.", e);
		}
	}

}
