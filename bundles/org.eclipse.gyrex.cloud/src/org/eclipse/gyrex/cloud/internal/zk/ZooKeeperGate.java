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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.cloud.internal.CloudDebug;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.BadVersionException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
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
	 * ZooKeeper extension to make protected methods visible for better
	 * debugging.
	 * 
	 * @noinstantiate This class is not intended to be instantiated by clients.
	 */
	static final class DebuggableZooKeeper extends org.apache.zookeeper.ZooKeeper {
		public DebuggableZooKeeper(final String connectString, final int sessionTimeout, final Watcher watcher) throws IOException {
			super(connectString, sessionTimeout, watcher);
		}

		public DebuggableZooKeeper(final String connectString, final int sessionTimeout, final Watcher watcher, final long sessionId, final byte[] sessionPasswd) throws IOException {
			super(connectString, sessionTimeout, watcher, sessionId, sessionPasswd);
		}

		@Override
		protected SocketAddress testableRemoteSocketAddress() {
			return super.testableRemoteSocketAddress();
		}

		@Override
		protected boolean testableWaitForShutdown(final int wait) throws InterruptedException {
			return super.testableWaitForShutdown(wait);
		}
	}

	private static final CopyOnWriteArrayList<ZooKeeperGateListener> gateListeners = new CopyOnWriteArrayList<ZooKeeperGateListener>();
	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperGate.class);

	private static final AtomicReference<ZooKeeperGate> instanceRef = new AtomicReference<ZooKeeperGate>();

	/**
	 * Adds a connection monitor.
	 * <p>
	 * This method has no effect if the monitor is already registered or the
	 * specified monitor is <code>null</code>
	 * </p>
	 * 
	 * @param listener
	 *            the listener to register (may be <code>null</code>)
	 */
	public static void addConnectionMonitor(final ZooKeeperGateListener listener) {
		// ignore null monitors
		if (listener == null)
			return;

		// add listener first
		gateListeners.addIfAbsent(listener);
	}

	private static String gateDownError(final ZooKeeperGate gate) {
		try {
			return String.format("ZooKeeper Gate is DOWN. (%s)", String.valueOf(gate));
		} catch (final Throwable e) {
			if ((e instanceof VirtualMachineError) || (e instanceof LinkageError))
				throw (Error) e;
			return String.format("ZooKeeper Gate is DOWN. (%s)", ExceptionUtils.getRootCauseMessage(e));
		}
	}

	/**
	 * Returns the current active gate.
	 * 
	 * @return the active gate
	 * @throws GateDownException
	 *             if the gate is DOWN
	 */
	public static ZooKeeperGate get() throws GateDownException {
		final ZooKeeperGate gate = instanceRef.get();
		if (gate == null)
			throw new GateDownException(gateDownError(null));
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
	 * This method has no effect if the monitor is not registered
	 * </p>
	 * 
	 * @param connectionMonitor
	 *            the monitor to unregister (may be <code>null</code>)
	 */
	public static void removeConnectionMonitor(final ZooKeeperGateListener connectionMonitor) {
		// ignore null monitors
		if (connectionMonitor == null)
			return;

		// remove listener
		gateListeners.remove(connectionMonitor);
	}

	private final DebuggableZooKeeper zooKeeper;
	private final ZooKeeperGateListener reconnectMonitor;

	/**
	 * a job that triggers when the recovery time expires and a session should
	 * be closed
	 */
	private final Job markSessionExpiredJob;
	{
		markSessionExpiredJob = new Job("ZooKeeper Gate Session Timout") {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				// check if still alive
				if (!zooKeeper.getState().isAlive())
					return Status.CANCEL_STATUS;

				// if the gate is still disconnected then we'll set the session to expired and close the gate
				if (keeperStateRef.compareAndSet(KeeperState.Disconnected, KeeperState.Expired)) {
					LOG.info("ZooKeeper session expiration forced. Gate ({}) has been in RECOVERING too long.", ZooKeeperGate.this);
					shutdown(true);
				}
				return Status.OK_STATUS;
			}

		};
		markSessionExpiredJob.setSystem(true);
		markSessionExpiredJob.setPriority(Job.SHORT);
	}

	/** the primary gate watcher */
	private final Watcher gateWatcher = new Watcher() {

		@Override
		public void process(final WatchedEvent event) {
			// only process connection/state events
			if (event.getType() != EventType.None) {
				if (CloudDebug.zooKeeperGateLifecycle) {
					LOG.trace("Ignoring event ({}).", event);
				}
				return;
			}

			// only process event if we are the active gate
			final ZooKeeperGate gate = ZooKeeperGate.this;
			if (!isCurrentGate(gate)) {
				if (CloudDebug.zooKeeperGateLifecycle) {
					LOG.debug("Ignored connection event for inactive gate: {}, {}", gate, event);
				}
				return;
			}

			// log message
			if (CloudDebug.zooKeeperGateLifecycle) {
				LOG.debug("Connection event: {}", event);
			}

			// handle event
			switch (event.getState()) {
				case SyncConnected:
					// set state
					KeeperState oldState = keeperStateRef.getAndSet(KeeperState.SyncConnected);

					// SyncConnected ==> connection is UP
					LOG.info("ZooKeeper Gate is now UP (was {}). Session 0x{} established with {} (using timeout {}ms). [{}]", oldState, Long.toHexString(zooKeeper.getSessionId()), zooKeeper.testableRemoteSocketAddress(), zooKeeper.getSessionTimeout(), ZooKeeperGate.this);

					// reset recovery
					markSessionExpiredJob.cancel();

					// notify gate listeners (on state change only)
					if (oldState != KeeperState.SyncConnected) {
						notifyGateUp();
					} else {
						if (CloudDebug.zooKeeperGateLifecycle) {
							LOG.debug("Old state == new state, not sending any events.");
						}
					}
					break;

				case Disconnected:
					// set state
					oldState = keeperStateRef.getAndSet(KeeperState.Disconnected);

					// Disconnected ==> connection is down
					LOG.info("ZooKeeper Gate is now RECOVERING (was {}). Connection lost. [{}]", oldState, ZooKeeperGate.this);

					// ZK automatically tries to re-connect; however, until the connection
					// is established again, we won't see any events from the server;
					// we also can't expect to reliably receive a session expired event because
					// session expiration events come from the server, too

					// schedule a job to expire the session if recovery fails
					markSessionExpiredJob.schedule(Math.max(500L, zooKeeper.getSessionTimeout() + 500L));

					// notify gate listeners (on state change only)
					if (oldState != KeeperState.Disconnected) {
						notifyGateRecovering();
					} else {
						if (CloudDebug.zooKeeperGateLifecycle) {
							LOG.debug("Old state == new state, not sending any events.", oldState);
						}
					}
					break;

				case Expired:
					// set state
					oldState = keeperStateRef.getAndSet(KeeperState.Expired);

					// Expired || Disconnected ==> connection is down
					LOG.info("ZooKeeper Gate is now DOWN (was {}). Session expired. [{}]", oldState, ZooKeeperGate.this);

					// reset recovery
					markSessionExpiredJob.cancel();

					// trigger clean shutdown (and notify listeners)
					shutdown(oldState != KeeperState.Expired);
					break;

				case AuthFailed:
					// set state
					oldState = keeperStateRef.getAndSet(KeeperState.AuthFailed);

					// Expired || Disconnected ==> connection is down
					LOG.error("ZooKeeper Gate is now DOWN (was {}). Authentication failed. [{}]", oldState, ZooKeeperGate.this);

					// trigger clean shutdown (and notify listeners)
					shutdown(oldState != KeeperState.Expired);
					break;

				default:
					// ZooKeeper will re-try on it's own in all other cases
					LOG.warn("Received event {} from ZooKeeper. Gate is not intervening. [{}]", event.getState(), ZooKeeperGate.this);
					break;
			}
		}

	};

	private final AtomicReference<KeeperState> keeperStateRef = new AtomicReference<KeeperState>();

	private final String connectString;
	private final int sessionTimeout;

	ZooKeeperGate(final ZooKeeperGateConfig config, final ZooKeeperGateListener reconnectMonitor) throws IOException {
		// the gate manager monitor
		this.reconnectMonitor = reconnectMonitor;

		// initiate ZK connection
		connectString = config.getConnectString();
		sessionTimeout = config.getSessionTimeout();
		zooKeeper = new DebuggableZooKeeper(connectString, sessionTimeout, gateWatcher);

		// log message
		if (CloudDebug.zooKeeperGateLifecycle) {
			LOG.debug("New ZooKeeper Gate instance. {}", this, new Exception("ZooKeeper Gate Constructor Call Stack"));
		}
	}

	private IPath create(final IPath path, final CreateMode createMode, final byte[] data) throws InterruptedException, KeeperException, IOException {
		if (path == null)
			throw new IllegalArgumentException("path must not be null");
		if (createMode == null)
			throw new IllegalArgumentException("createMode must not be null");

		// create all parents
		ZooKeeperHelper.createParents(getZooKeeper(), path);

		// create node itself
		return new Path(getZooKeeper().create(path.toString(), data, ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode));
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
		if (recordData == null)
			throw new IllegalArgumentException("recordData must not be null");
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
		if (recordData == null)
			throw new IllegalArgumentException("recordData must not be null");
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
		if (path == null)
			throw new IllegalArgumentException("path must not be null");

		try {
			// delete all children
			final List<String> children = getZooKeeper().getChildren(path.toString(), false);
			for (final String child : children) {
				deletePath(path.append(child));
			}

			// delete node itself
			getZooKeeper().delete(path.toString(), -1);
		} catch (final KeeperException e) {
			if (e.code() != Code.NONODE)
				throw e;
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
		if (path == null)
			throw new IllegalArgumentException("path must not be null");

		// read stats
		final Stat stat = new Stat();

		// read children
		final List<String> children = getZooKeeper().getChildren(path.toString(), false, stat);

		// abort if version doesn't match
		if ((version != -1) && (stat.getVersion() != version))
			throw new BadVersionException(path.toString());

		// delete all children
		for (final String child : children) {
			deletePath(path.append(child));
		}

		// delete node itself
		getZooKeeper().delete(path.toString(), version);
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
		if (path == null)
			throw new IllegalArgumentException("path must not be null");
		try {
			return getZooKeeper().exists(path.toString(), monitor) != null;
		} catch (final KeeperException e) {
			throw e;
		}
	}

	/**
	 * Returns more information about the server this node is connected to.
	 * <p>
	 * This method is used for debugging purposes and may not be referenced
	 * elsewhere.
	 * </p>
	 * 
	 * @return the server info
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public String getConnectedServerInfo() {
		final SocketAddress socketAddress = zooKeeper.testableRemoteSocketAddress();
		if (socketAddress instanceof InetSocketAddress)
			return String.format("%s:%d", ((InetSocketAddress) socketAddress).getHostName(), ((InetSocketAddress) socketAddress).getPort());
		if (null != socketAddress)
			return socketAddress.toString();
		return null;
	}

	/**
	 * Returns the configured connect string used by the current active
	 * ZooKeeper gate.
	 * <p>
	 * This method is used for debugging purposes and may not be referenced
	 * elsewhere.
	 * </p>
	 * 
	 * @return the connect string
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public String getConnectString() {
		return connectString;
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
		return getZooKeeper().getSessionId();
	}

	/**
	 * Returns the configured session timeout used by the current active
	 * ZooKeeper gate.
	 * <p>
	 * This method is used for debugging purposes and may not be referenced
	 * elsewhere.
	 * </p>
	 * 
	 * @return the session timeout
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public int getSessionTimeout() {
		return sessionTimeout;
	}

	final ZooKeeper getZooKeeper() {
		// note, we don't perform any checks here but simply return what we have
		// this is essential because downstream code should rely on KeeperException
		// as thrown by ZooKeeper itself instead of also handling our custom logic
		return zooKeeper;
	}

	private void handleBrokenListener(final ZooKeeperGateListener listener, final Throwable t) {
		// log error
		LOG.error("Removing bogous connection listener {} due to exception ({}).", new Object[] { listener, ExceptionUtils.getMessage(t), t });
		// remove listener directly
		gateListeners.remove(listener);
	}

	void notifyGateDown() {
		// notify registered listeners
		for (final ZooKeeperGateListener listener : gateListeners) {
			try {
				if (CloudDebug.zooKeeperGateLifecycle) {
					LOG.debug("Sending gate down event to listener ({}).", listener);
				}
				listener.gateDown(this);
			} catch (final RuntimeException | AssertionError | LinkageError e) {
				handleBrokenListener(listener, e);
			}
		}

		// notify reconnect listener last
		if (reconnectMonitor != null) {
			reconnectMonitor.gateDown(this);
		}
	}

	void notifyGateRecovering() {
		// notify registered listeners
		for (final ZooKeeperGateListener listener : gateListeners) {
			try {
				if (CloudDebug.zooKeeperGateLifecycle) {
					LOG.debug("Sending gate recovering event to listener ({}).", listener);
				}
				listener.gateRecovering(this);
			} catch (final RuntimeException | AssertionError | LinkageError e) {
				handleBrokenListener(listener, e);
			}
		}

		// notify reconnect listener last
		if (reconnectMonitor != null) {
			reconnectMonitor.gateRecovering(this);
		}
	}

	void notifyGateUp() {
		// notify reconnect listener first
		if (reconnectMonitor != null) {
			reconnectMonitor.gateUp(this);
		}

		// notify registered listeners
		for (final ZooKeeperGateListener listener : gateListeners) {
			try {
				if (CloudDebug.zooKeeperGateLifecycle) {
					LOG.debug("Sending gate up event to listener ({}).", listener);
				}
				listener.gateUp(this);
			} catch (final RuntimeException | AssertionError | LinkageError e) {
				handleBrokenListener(listener, e);
			}
		}

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
	public List<String> readChildrenNames(final IPath path, final Stat stat) throws InterruptedException, KeeperException {
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
	public List<String> readChildrenNames(final IPath path, final ZooKeeperMonitor watch, final Stat stat) throws NoNodeException, KeeperException, InterruptedException {
		if (path == null)
			throw new IllegalArgumentException("path must not be null");
		return getZooKeeper().getChildren(path.toString(), watch, stat);
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
			if (data == null)
				return defaultValue;
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
		if (path == null)
			throw new IllegalArgumentException("path must not be null");
		return getZooKeeper().getData(path.toString(), watch, stat);
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
		if (path == null)
			throw new IllegalArgumentException("path must not be null");

		if ((createMode != null) && !exists(path)) {
			try {
				create(path, createMode, data);

				// create succeeded, continue with set below
			} catch (final KeeperException e) {
				if (e.code() != KeeperException.Code.NODEEXISTS)
					// rethrow
					throw e;
			}
		}

		// set data
		return getZooKeeper().setData(path.toString(), data, version);
	}

	/**
	 * Closes the gate.
	 * 
	 * @param notify
	 *            set to <code>true</code> to notify registered connection
	 *            listeners
	 */
	void shutdown(final boolean notify) {
		if (CloudDebug.zooKeeperGateLifecycle) {
			LOG.debug("Shutdown of ZooKeeper Gate. {}", this, new Exception("ZooKeeper Gate Shutdown Call Stack"));
		}

		// close ZooKeeper
		try {
			zooKeeper.close();
			LOG.info("ZooKeeper session 0x{} closed. Gate ({}) shut down.", Long.toHexString(zooKeeper.getSessionId()), this);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (final RuntimeException e) {
			// ignored shutdown exceptions
			if (CloudDebug.zooKeeperGateLifecycle) {
				LOG.debug("Ignored exception during shutdown: {}", e.getMessage(), e);
			}
		}

		// notify listeners
		if (notify) {
			notifyGateDown();
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
		builder.append("ZooKeeperGate ");
		if (isCurrentGate(this)) {
			builder.append("CURRENT ");
		}
		builder.append(keeperStateRef.get()).append(' ');
		builder.append("[").append(zooKeeper).append("]");
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
		if (recordData == null)
			throw new IllegalArgumentException("recordData must not be null");
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
		if (recordData == null)
			throw new IllegalArgumentException("recordData must not be null");
		if (createMode == null)
			throw new IllegalArgumentException("createMode must not be null");
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
		if (recordData == null)
			throw new IllegalArgumentException("recordData must not be null");
		if (createMode == null)
			throw new IllegalArgumentException("createMode must not be null");
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
