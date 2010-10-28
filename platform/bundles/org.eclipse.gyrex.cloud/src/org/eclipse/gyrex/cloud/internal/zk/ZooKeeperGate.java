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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.gyrex.cloud.internal.CloudActivator;
import org.eclipse.gyrex.cloud.internal.CloudDebug;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IPreferencesService;

import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.CharSetUtils;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.text.StrBuilder;
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

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperGate.class);

	private static final AtomicReference<ZooKeeperGate> instanceRef = new AtomicReference<ZooKeeperGate>();

	public static ZooKeeperGate get() {
		final ZooKeeperGate gate = instanceRef.get();
		if (gate != null) {
			return gate;
		}
		instanceRef.compareAndSet(null, new ZooKeeperGate());
		return instanceRef.get();
	}

	private final AtomicReference<ZooKeeper> zkRef = new AtomicReference<ZooKeeper>();
	private final Lock zkCreationLock = new ReentrantLock();
	private final AtomicReference<KeeperState> connectionState = new AtomicReference<KeeperState>();

	private final Watcher connectionMonitor = new Watcher() {

		@Override
		public void process(final WatchedEvent event) {
			if (CloudDebug.zooKeeperConnectionLifecycle) {
				LOG.debug("Connection event: {}", event);
			}

			final KeeperState newState = event.getState();
			connectionState.set(newState);
			switch (newState) {
				case SyncConnected:
					LOG.info("Connection to cloud established.");
					break;

				default:
					LOG.info("Lost connection to cloud ({}).", newState);
					reset();
					break;
			}
		}
	};

	/**
	 * Creates a new instance.
	 */
	private ZooKeeperGate() {
		// empty
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
	 * Creates a record at the specified path in ZooKeeper.
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
	public void createRecord(final IPath path, final CreateMode createMode, final String recordData) throws KeeperException, InterruptedException, IOException {
		if (recordData == null) {
			throw new IllegalArgumentException("recordData must not be null");
		}
		try {
			create(path, createMode, recordData.getBytes(CharEncoding.UTF_8));
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

	public void dumpTree(final String path, final int indent, final StrBuilder string) throws Exception {
		final byte[] data = ensureConnected().getData(path, false, null);
		final List<String> children = ensureConnected().getChildren(path, false);
		final StringBuilder spaces = new StringBuilder();
		for (int i = 0; i < indent; i++) {
			spaces.append(" ");
		}
		string.append(spaces).append(path).append(" (").append(children.size()).appendln(")");
		if (data != null) {
			String dataString = new String(data, CharEncoding.UTF_8);
			dataString = CharSetUtils.delete(dataString, "" + CharUtils.CR);
			dataString = StringUtils.replace(dataString, "" + CharUtils.LF, SystemUtils.LINE_SEPARATOR + spaces + "  ");
			string.append(spaces).append("D:").appendln(dataString);
		}

		for (final String child : children) {
			dumpTree(path + (path.equals("/") ? "" : "/") + child, indent + 1, string);
		}

	}

	final ZooKeeper ensureConnected() {
		ZooKeeper zk = zkRef.get();
		if (zk != null) {
			return zk;
		}

		/*
		 * This is a bit misleading. Connections happen asynchronously. We can't reliably ensure
		 * the connected state here. However, we try out best.
		 */

		try {
			// port/address to listen for client connections
			final IPreferencesService preferenceService = CloudActivator.getInstance().getPreferenceService();
			final int clientPort = preferenceService.getInt(CloudActivator.SYMBOLIC_NAME, ZooKeeperServerConfig.PREF_KEY_CLIENT_PORT, 2181, null);

			if (!zkCreationLock.tryLock(5, TimeUnit.SECONDS)) {
				throw new IllegalStateException("unable to connect, connect lock timeout");
			}
			try {
				zk = zkRef.get();
				if (zk != null) {
					return zk;
				}
				if (CloudDebug.zooKeeperConnectionLifecycle) {
					LOG.debug("Creating new ZooKeeper instance");
				}

				zk = new ZooKeeper("localhost:" + clientPort, 5000, connectionMonitor);
				zkRef.set(zk);
			} finally {
				zkCreationLock.unlock();
			}

			int maxWait = 100;
			while (!isConnected() && (maxWait-- > 0)) {
				if (CloudDebug.zooKeeperConnectionLifecycle) {
					LOG.debug("Waiting for connection...");
				}
				Thread.sleep(150);
			}
			if (!isConnected()) {
				// give up
				zk.close();
				throw new IllegalStateException("unable to connect, timeout");
			}
			return zk;
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("unable to connect, connect lock interrupted");
		} catch (final IOException e) {
			throw new IllegalStateException("unable to connect, connection error: " + e.getMessage(), e);
		}

	}

	public boolean isConnected() {
		return connectionState.get() == KeeperState.SyncConnected;
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
		if (path == null) {
			throw new IllegalArgumentException("path must not be null");
		}
		try {
			return ensureConnected().getData(path.toString(), false, null);
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

	final void reset() {
		if (CloudDebug.zooKeeperConnectionLifecycle) {
			LOG.debug("Resetting ZooKeeper gate");
		}
		zkCreationLock.lock();
		try {
			connectionState.set(null);
			final ZooKeeper oldKeeper = zkRef.getAndSet(null);
			if (oldKeeper != null) {
				if (CloudDebug.zooKeeperConnectionLifecycle) {
					LOG.debug("Closing old ZooKeeper instance");
				}
				try {
					oldKeeper.close();
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		} finally {
			zkCreationLock.unlock();
		}
	}
}
