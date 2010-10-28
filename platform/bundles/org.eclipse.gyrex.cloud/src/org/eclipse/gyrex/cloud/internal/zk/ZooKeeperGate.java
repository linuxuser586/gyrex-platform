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
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central class within Gyrex to be used for all ZooKeeper related
 * communication.
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
	private final Lock zkLock = new ReentrantLock();

	private final Watcher connectionMonitor = new Watcher() {

		@Override
		public void process(final WatchedEvent event) {
			LOG.debug("EVENT: " + event);

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
				getZk().create(parentPath.toString(), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			} catch (final KeeperException e) {
				if (e.code() != KeeperException.Code.NODEEXISTS) {
					// rethrow
					throw e;
				}
			}
		}

		// create node itself
		getZk().create(path.toString(), data, ZooDefs.Ids.OPEN_ACL_UNSAFE, createMode);
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

	public void dumpTree(final String path, final int indent, final StrBuilder string) throws Exception {
		final byte[] data = getZk().getData(path, false, null);
		final List<String> children = getZk().getChildren(path, false);
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

	private ZooKeeper ensureConnect() {
		try {
			final IPreferencesService preferenceService = CloudActivator.getInstance().getPreferenceService();

			// port/address to listen for client connections
			final int clientPort = preferenceService.getInt(CloudActivator.SYMBOLIC_NAME, ZooKeeperServerConfig.PREF_KEY_CLIENT_PORT, 2181, null);

			if (!zkLock.tryLock(5, TimeUnit.SECONDS)) {
				throw new IllegalStateException("unable to connect, connect lock timeout");
			}
			try {
				ZooKeeper keeper = zkRef.get();
				if (keeper != null) {
					return keeper;
				}

				keeper = new ZooKeeper("localhost:" + clientPort, 5000, connectionMonitor);
				zkRef.set(keeper);
				return keeper;
			} finally {
				zkLock.unlock();
			}
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("unable to connect, connect lock interrupted");
		} catch (final IOException e) {
			throw new IllegalStateException("unable to connect, connection error: " + e.getMessage(), e);
		}

	}

	final ZooKeeper getZk() {
		final ZooKeeper zk = zkRef.get();
		if (zk == null) {
			return ensureConnect();
		}
		return zk;
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
			return getZk().getData(path.toString(), false, null);
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
}
