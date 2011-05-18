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
package org.eclipse.gyrex.cloud.internal.preferences;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.gyrex.cloud.internal.CloudDebug;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperBasedService;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperMonitor;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ZooKeeper based preferences.
 */
public abstract class ZooKeeperBasedPreferences extends ZooKeeperBasedService implements IEclipsePreferences {

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperBasedPreferences.class);

	private static final String VERSION_KEY = "gyrex.preferences.version"; //$NON-NLS-1$
	private static final String VERSION_VALUE = "1"; //$NON-NLS-1$

	private static final String[] EMPTY_NAMES_ARRAY = new String[0];
	private static final String PATH_SEPARATOR = String.valueOf(IPath.SEPARATOR);
	private static final String EMPTY_STRING = "";
	private static final String FALSE = Boolean.FALSE.toString();
	private static final String TRUE = Boolean.TRUE.toString();

	private final IEclipsePreferences parent;
	private final String name;
	private final IPath path;
	private final IPath zkPath;
	private final ConcurrentMap<String, ZooKeeperBasedPreferences> children = new ConcurrentHashMap<String, ZooKeeperBasedPreferences>(4);
	private final Set<String> pendingChildRemovals = new HashSet<String>(); // guarded by childrenModifyLock
	private final Properties properties = new Properties();

	/** connected state */
	final AtomicBoolean connected = new AtomicBoolean();

	/** prevent concurrent modifications of children */
	private final Lock childrenModifyLock = new ReentrantLock();

	/** prevent concurrent modifications of properties */
	private final Lock propertiesLoadOrSaveLock = new ReentrantLock();

	private final ZooKeeperMonitor monitor = new ZooKeeperMonitor() {

		@Override
		protected void childrenChanged(final String path) {
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Node {} updated remotely: CHILDREN CHANGED", path);
			}

			if (removed) {
				return;
			}

			// process events only for this node
			if (zkPath.toString().equals(path)) {
				try {
					// refresh children and notify listeners (but only if remote is newer)
					loadChildren(false);
				} catch (final NoNodeException e) {
					// this can happen if a whole tree is being removed
					// we don't synchronize so we must expect this
					// handle this gracefully and rely on #pathDeleted being called
					// assume not connected
					connected.set(false);
				} catch (final Exception e) {
					// assume not connected
					connected.set(false);
					// log warning
					LOG.warn("Error refreshing children of node {}. {}", ZooKeeperBasedPreferences.this, e.getMessage());
				}
			}
		}

		@Override
		protected void pathCreated(final String path) {
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Node {} updated remotely: CREATED", path);
			}

			// process events only for this node
			if (zkPath.toString().equals(path)) {

				// unset removed state
				removed = false;

				// note, this can be a result of a flush, therefore we only
				// load properties and children if they are newer

				try {
					// refresh properties and notify listeners (but only if remote is newer)
					loadProperties(false);

					// refresh children and notify listeners (but only if remote is newer)
					loadChildren(false);
				} catch (final Exception e) {
					// assume not connected
					connected.set(false);
					// log warning
					LOG.warn("Error refreshing node {}. {}", ZooKeeperBasedPreferences.this, e.getMessage());
				}
			}
		};

		@Override
		protected void pathDeleted(final String path) {
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Node {} updated remotely: REMOVED", path);
			}

			if (removed) {
				return;
			}

			// process events only for this node
			if (zkPath.toString().equals(path)) {

				// remove the node (will notify listeners)
				try {
					removeNode();
				} catch (final BackingStoreException e) {
					// assume not connected
					connected.set(false);
					// log warning
					LOG.warn("Error removing node {}. {}", ZooKeeperBasedPreferences.this, e.getMessage());
				}
			}
		}

		@Override
		protected void recordChanged(final String path) {
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Node {} updated remotely: PROPERTIES", path);
			}

			if (removed) {
				return;
			}

			// process events only for this node
			if (zkPath.toString().equals(path)) {
				try {
					// refresh properties and notify listeners (but only if remote is newer)
					loadProperties(false);
				} catch (final Exception e) {
					// assume not connected
					connected.set(false);
					// log warning
					LOG.warn("Error refreshing properties of node {}. {}", ZooKeeperBasedPreferences.this, e.getMessage());
				}
			}
		}
	};

	volatile boolean removed;

	/** ZooKeeper version of the properties object */
	private volatile int propertiesVersion;

	/** ZooKeeper version of the children */
	private volatile int childrenVersion;

	/** simple attempt to avoid unnecessary flushes */
	private volatile boolean propertiesDirty;

	private volatile ListenerList nodeListeners;
	private volatile ListenerList preferenceListeners;

	/**
	 * Creates a new instance.
	 * <p>
	 * The preferences will be located at the specified path
	 * </p>
	 * 
	 * @param parent
	 * @param name
	 */
	public ZooKeeperBasedPreferences(final IEclipsePreferences parent, final String name, final IPath zooKeeperParentPath) {
		super(50l, 3); /* experiment with a short retry dely for preferences */
		if (parent == null) {
			throw new IllegalArgumentException("parent must not be null");
		}
		this.parent = parent;
		this.name = name;

		// cache path computed based on parent
		final String parentPath = parent.absolutePath();
		if (parentPath.equals(PATH_SEPARATOR)) {
			path = new Path(name).makeAbsolute();
		} else {
			path = new Path(parentPath).append(name).makeAbsolute();
		}

		// cache ZooKeeper path
		zkPath = zooKeeperParentPath.append(path);
	}

	@Override
	public String absolutePath() {
		return path.toString();
	}

	@Override
	public void accept(final IPreferenceNodeVisitor visitor) throws BackingStoreException {
		ensureConnectedAndLoaded();
		if (!visitor.visit(this)) {
			return;
		}
		final Collection<ZooKeeperBasedPreferences> toVisit = children.values();
		for (final ZooKeeperBasedPreferences child : toVisit) {
			child.accept(visitor);
		}
	}

	@Override
	public void addNodeChangeListener(final INodeChangeListener listener) {
		if (nodeListeners == null) {
			synchronized (this) {
				if (nodeListeners == null) {
					nodeListeners = new ListenerList();
				}
			}
		}
		nodeListeners.add(listener);
	}

	@Override
	public void addPreferenceChangeListener(final IPreferenceChangeListener listener) {
		if (preferenceListeners == null) {
			synchronized (this) {
				if (preferenceListeners == null) {
					preferenceListeners = new ListenerList();
				}
			}
		}
		preferenceListeners.add(listener);
	}

	private IEclipsePreferences calculateRoot() {
		IEclipsePreferences result = this;
		while (result.parent() != null) {
			result = (IEclipsePreferences) result.parent();
		}
		return result;
	}

	private void checkRemoved() {
		if (removed) {
			throw new IllegalStateException(String.format("Node '%s' has been removed.", name));
		}
	}

	/**
	 * Called by children when a child was removed.
	 * 
	 * @param child
	 */
	private void childRemoved(final ZooKeeperBasedPreferences child) {
		// don't do anything if removed
		if (removed) {
			return;
		}

		boolean wasRemoved = false;

		// prevent concurrent modification (eg. remote and local removal)
		childrenModifyLock.lock();
		try {
			if (removed) {
				return;
			}

			// remove child
			if (children.remove(child.name()) != null) {
				wasRemoved = true;
				// remember removals for flush
				pendingChildRemovals.add(child.name());
			}
		} finally {
			childrenModifyLock.unlock();
		}

		// fire events outside of lock
		// TODO we need to understand event ordering better (eg. concurrent remote updates)
		// (this may result in sending events asynchronously through an ordered queue, but for now we do it directly)
		if (wasRemoved) {
			fireNodeEvent(child, false);
		}
	}

	@Override
	public String[] childrenNames() throws BackingStoreException {
		try {
			ensureConnectedAndLoaded();
			final Set<String> names = children.keySet();
			return !names.isEmpty() ? (String[]) names.toArray(EMPTY_NAMES_ARRAY) : EMPTY_NAMES_ARRAY;
		} catch (final Exception e) {
			// assume not connected
			connected.set(false);
			// throw
			throw new BackingStoreException(String.format("Error while reading children from ZooKeeper for node '%s'. %s", this, e.getMessage()), e);
		}
	}

	@Override
	public void clear() throws BackingStoreException {
		ensureConnectedAndLoaded();

		// call each one separately (instead of Properties.clear) so
		// clients get change notification
		final String[] keys = keys();
		for (int i = 0; i < keys.length; i++) {
			remove(keys[i]);
		}
	}

	@Override
	protected void disconnect() {
		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("Disconnecting preference node {}.", this);
		}

		// try to flush any pending changes
		// (just properties here as we are recursively disconnecting the children)
		if (!removed && propertiesDirty) {
			try {
				saveProperties();
			} catch (final Exception ignored) {
				// ignore
			}
		}

		// set disconnected
		connected.set(false);

		// disconnect children
		childrenModifyLock.lock();
		try {
			if (removed) {
				return;
			}

			for (final ZooKeeperBasedPreferences child : children.values()) {
				child.disconnect();
			}
		} finally {
			childrenModifyLock.unlock();
		}
	}

	private void ensureConnectedAndLoaded() throws IllegalStateException {
		checkRemoved();

		if (!connected.get() && connected.compareAndSet(false, true)) {
			try {

				if (CloudDebug.zooKeeperPreferences) {
					LOG.debug("Connecting preference node {}.", this);
				}

				// check if path exists
				if (!ZooKeeperGate.get().exists(zkPath, monitor)) {
					if (CloudDebug.zooKeeperPreferences) {
						LOG.debug("Node {} connected, waiting for remote path to be created ({})", this, zkPath);
					}

					// the preference node has been created
					// we must set it dirty in order to force #flush to create the path if needed
					// and save an empty properties
					propertiesDirty = true;

					// done
					return;
				}

				// load properties (and force sync with remote)
				loadProperties(true);

				// load children (and force sync with remote)
				loadChildren(true);
			} catch (final Exception e) {
				if (CloudDebug.zooKeeperPreferences) {
					LOG.debug("Exception while connecting node {}: {}", this, e.getMessage());
					LOG.debug("Stack for connection request for node {}.", this, new Exception("Stack for preference node connection request."));
				}

				// assume not connected
				connected.set(false);

				// throw
				throw new IllegalStateException(String.format("Error loading node '%s' from ZooKeeper. %s", this, e.getMessage()), e);
			}
		}
	}

	/**
	 * Fires a node change event.
	 * 
	 * @param child
	 *            the child node
	 * @param added
	 *            <code>true</code> if added, <code>false</code> if removed
	 */
	private void fireNodeEvent(final ZooKeeperBasedPreferences child, final boolean added) {
		final NodeChangeEvent event = new NodeChangeEvent(this, child);
		final ListenerList listeners = nodeListeners;
		if (listeners != null) {
			for (final Object listener : listeners.getListeners()) {
				if (added) {
					((INodeChangeListener) listener).added(event);
				} else {
					((INodeChangeListener) listener).removed(event);
				}
			}
		}
	}

	/**
	 * Fires a preference change event
	 * 
	 * @param event
	 *            the event to fire
	 */
	private void firePreferenceEvent(final PreferenceChangeEvent event) {
		final ListenerList listeners = preferenceListeners;
		if (listeners != null) {
			for (final Object listener : listeners.getListeners()) {
				((IPreferenceChangeListener) listener).preferenceChange(event);
			}
		}
	}

	@Override
	public void flush() throws BackingStoreException {
		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("Flushing node {} (version {}, cversion {})", new Object[] { this, propertiesVersion, childrenVersion });
		}

		ensureConnectedAndLoaded();
		try {
			// save properties
			saveProperties();

			// save children
			saveChildren();
		} catch (final Exception e) {
			// assume not connected
			connected.set(false);
			// throw
			throw new BackingStoreException(String.format("Error saving node data '%s' from ZooKeeper. %s", this, e.getMessage()), e);
		}

		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("Flushed node {} (version {}, cversion {})", new Object[] { this, propertiesVersion, childrenVersion });
		}
	}

	@Override
	public String get(final String key, final String def) {
		if (key == null) {
			throw new IllegalArgumentException("key must not be null");
		}

		// ensure that the node is connected
		// we do not allow off-line operation
		ensureConnectedAndLoaded();

		final String value = properties.getProperty(key);
		return value == null ? def : value;
	}

	@Override
	public boolean getBoolean(final String key, final boolean def) {
		final String value = get(key, null);
		return value == null ? def : TRUE.equalsIgnoreCase(value);
	}

	@Override
	public byte[] getByteArray(final String key, final byte[] def) {
		final String value = get(key, null);
		try {
			return value == null ? def : Base64.decodeBase64(value.getBytes(CharEncoding.US_ASCII));
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("Java VM does not support US_ASCII encoding? " + e.getMessage());
		}
	}

	@Override
	public double getDouble(final String key, final double def) {
		final String value = get(key, null);
		return value == null ? def : Double.valueOf(value);
	}

	@Override
	public float getFloat(final String key, final float def) {
		final String value = get(key, null);
		return value == null ? def : Float.valueOf(value);
	}

	@Override
	public int getInt(final String key, final int def) {
		final String value = get(key, null);
		return value == null ? def : Integer.valueOf(value);
	}

	@Override
	public long getLong(final String key, final long def) {
		final String value = get(key, null);
		return value == null ? def : Long.valueOf(value);
	}

	@Override
	protected String getToStringDetails() {
		final StringBuilder details = new StringBuilder();
		details.append(absolutePath());
		if (!connected.get()) {
			details.append(" DISCONNECTED");
		}
		return details.toString();
	}

	@Override
	public String[] keys() throws BackingStoreException {
		ensureConnectedAndLoaded();

		if (properties.isEmpty()) {
			return EMPTY_NAMES_ARRAY;
		}

		final Set<String> names = properties.stringPropertyNames();
		return names.toArray(EMPTY_NAMES_ARRAY);
	}

	/**
	 * Loads children from ZooKeeper and updates internal cache.
	 * <p>
	 * This method reads the nodes from ZooKeeper and only processes
	 * <em>added</em> nodes. Remote removals are handled through the proper
	 * delete watch through the node itself.
	 * </p>
	 * 
	 * @throws KeeperException
	 * @throws InterruptedException
	 * @throws IllegalStateException
	 */
	void loadChildren(final boolean forceSyncWithRemoteVersion) throws IllegalStateException, InterruptedException, KeeperException {
		// don't do anything if removed
		if (removed) {
			return;
		}

		final List<ZooKeeperBasedPreferences> addedNodes = new ArrayList<ZooKeeperBasedPreferences>(3);

		childrenModifyLock.lock();
		try {
			if (removed) {
				return;
			}

			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Loading children for node {} (cversion {}) from {}", new Object[] { this, childrenVersion, zkPath });
			}

			// get list of children
			final Stat stat = new Stat();
			final Collection<String> childrenNames = ZooKeeperGate.get().readChildrenNames(zkPath, monitor, stat);

			// don't load properties if version is in the past
			if (!forceSyncWithRemoteVersion && (childrenVersion >= stat.getCversion())) {
				if (CloudDebug.zooKeeperPreferences) {
					LOG.debug("Not updating children of node {} - local cversion ({}) >= ZooKeeper cversion ({})", new Object[] { this, childrenVersion, stat.getCversion() });
				}
				return;
			}
			childrenVersion = stat.getCversion();

			// process nodes
			for (final String name : childrenNames) {
				// detect added nodes
				if (!children.containsKey(name)) {
					// does not exist locally, assume added in ZooKeeper
					final ZooKeeperBasedPreferences child = newChild(name);
					children.put(name, child);
					addedNodes.add(child);
				}

				// handle delete conflicts
				if (pendingChildRemovals.contains(name)) {
					// does exists locally as well as in ZooKeeper *and* is marked for deletion but not flushed
					// in any case, remote events win, i.e. if a node is deleted locally but not flushed
					// and then ZooKeeper triggered a children change, the local delete likely is stale
					// this can happen on concurrent edits,
					// another example is a local edit followed by a local reload/sync
					// not sure how we would handle this case more gracefully other then relying on the cversion
					pendingChildRemovals.remove(name);
				}

				// TODO: investigate remote delete handling
				/*
				 * Currently, remote deletes are discovered via monitor#pathDeleted. This removes the node
				 * but also adds it to "pendingChildRemovals" list. We might need to rework that so that we
				 * don't rely on "pathDeleted" ZK events but on "childrenChanged".
				 */
			}

			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Loaded children for node {} (now at cversion {})", this, childrenVersion);
			}
		} finally {
			childrenModifyLock.unlock();
		}

		// fire events outside of lock
		// TODO we need to understand event ordering better (eg. concurrent remote updates)
		// (this may result in sending events asynchronously through an ordered queue, but for now we do it directly)
		for (final ZooKeeperBasedPreferences child : addedNodes) {
			fireNodeEvent(child, true);
		}
	}

	void loadProperties(final boolean forceSyncWithRemoteVersion) throws KeeperException, InterruptedException, IOException {
		// don't do anything if removed
		if (removed) {
			return;
		}

		// collect events
		final List<PreferenceChangeEvent> events = new ArrayList<PreferenceChangeEvent>();

		// prevent concurrent property modification (eg. remote _and_ local flush)
		propertiesLoadOrSaveLock.lock();
		try {
			if (removed) {
				return;
			}

			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Reading properties for node {} (version {}) from ZooKeeper {}", new Object[] { this, propertiesVersion, zkPath });
			}

			// read record data
			final Stat stat = new Stat();
			final byte[] bytes = ZooKeeperGate.get().readRecord(zkPath, monitor, stat);

			// don't load properties if version is in the past
			if (!forceSyncWithRemoteVersion && (propertiesVersion >= stat.getVersion())) {
				if (CloudDebug.zooKeeperPreferences) {
					LOG.debug("Not updating properties of node {} - local version ({}) >= ZooKeeper version ({})", new Object[] { this, propertiesVersion, stat.getVersion() });
				}
				return;
			}
			propertiesVersion = stat.getVersion();

			// load remote properties
			final Properties loadedProps = new Properties();
			if (bytes != null) {
				loadedProps.load(new ByteArrayInputStream(bytes));

				// check version
				final Object version = loadedProps.remove(VERSION_KEY);
				if ((version == null) || !VERSION_VALUE.equals(version)) {
					// ignore for now
					LOG.warn("Properties with incompatible storage format version ({}) found for node {}.", version, this);
					return;
				}
			}

			// collect all property names
			final Set<String> propertyNames = new HashSet<String>();
			propertyNames.addAll(loadedProps.stringPropertyNames());
			propertyNames.addAll(properties.stringPropertyNames());

			// discover new, updated and removed properties
			for (final String key : propertyNames) {
				final String newValue = loadedProps.getProperty(key);
				final String oldValue = properties.getProperty(key);
				if (newValue == null) {
					// does not exists in ZooKeeper, assume removed
					properties.remove(key);
					if (CloudDebug.zooKeeperPreferences) {
						LOG.debug("Node {} property removed: {}", key);
					}
					// create event
					events.add(new PreferenceChangeEvent(this, key, oldValue, newValue));
				} else if ((oldValue == null) || !oldValue.equals(newValue)) {
					// assume added or updated in ZooKeeper
					properties.put(key, newValue);
					if (CloudDebug.zooKeeperPreferences) {
						if (oldValue == null) {
							LOG.debug("Node {} property added: {} - {}", key, newValue);
						} else {
							LOG.debug("Node {} property updated: {} - {}", key, newValue);
						}
					}
					// create event
					events.add(new PreferenceChangeEvent(this, key, oldValue, newValue));
				}
			}

			// mark clean
			propertiesDirty = false;

			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Loaded properties for node {} (now at version {})", this, propertiesVersion);
			}
		} finally {
			propertiesLoadOrSaveLock.unlock();
		}

		// fire events outside of lock
		// TODO we need to understand event ordering better (eg. concurrent remote updates)
		// (this may result in sending events asynchronously through an ordered queue, but for now we do it directly)
		for (final PreferenceChangeEvent event : events) {
			firePreferenceEvent(event);
		}
	}

	@Override
	public String name() {
		return name;
	}

	/**
	 * Must be implemented by subclasses to create and return a new child
	 * instance.
	 * 
	 * @param name
	 *            the child name
	 * @return the created child
	 */
	protected abstract ZooKeeperBasedPreferences newChild(final String name);

	@Override
	public Preferences node(final String path) {
		// check removal
		checkRemoved();

		// check if this node is requested
		if (path.length() == 0) {
			return this;
		}

		// use the root relative to this node instead of the global root
		// in case we have a different hierarchy. (e.g. export)
		if (path.charAt(0) == IPath.SEPARATOR) {
			return calculateRoot().node(path.substring(1));
		}

		// ensure that the node is connected with ZooKeeper
		ensureConnectedAndLoaded();

		// TODO: investigate behavior when not connected
		// we should probably allow traversal of *existing* childs but don't allow addition of new nodes

		// get child
		final int index = path.indexOf(IPath.SEPARATOR);
		final String key = index == -1 ? path : path.substring(0, index);
		ZooKeeperBasedPreferences child = children.get(key);

		// create the child locally if it doesn't exist
		boolean added = false;
		if (child == null) {
			childrenModifyLock.lock();
			try {
				child = children.get(key);
				while (child == null) {
					if (!children.containsKey(key)) {
						children.put(key, newChild(key));
						added = true;
						// remove from pending removals list
						pendingChildRemovals.remove(key);
					}
					child = children.get(key);
				}
			} finally {
				childrenModifyLock.unlock();
			}
		}

		// notify listeners if a child was added
		if (added) {
			fireNodeEvent(child, true);
		}
		return child.node(index == -1 ? EMPTY_STRING : path.substring(index + 1));
	}

	@Override
	public boolean nodeExists(final String pathName) throws BackingStoreException {
		// check if this node is requested
		if (pathName.length() == 0) {
			return !removed;
		}

		// illegal state if this node has been removed.
		// do this AFTER checking for the empty string.
		ensureConnectedAndLoaded();

		// use the root relative to this node instead of the global root
		// in case we have a different hierarchy. (e.g. export)
		if (pathName.charAt(0) == IPath.SEPARATOR) {
			return calculateRoot().nodeExists(pathName.substring(1));
		}

		final int index = pathName.indexOf(IPath.SEPARATOR);
		final boolean noSlash = index == -1;

		// if we are looking for a simple child then just look in the table and return
		if (noSlash) {
			return children.get(pathName) != null;
		}

		// otherwise load the parent of the child and then recursively ask
		final String childName = pathName.substring(0, index);
		final ZooKeeperBasedPreferences child = children.get(childName);
		if (child == null) {
			return false;
		}
		return child.nodeExists(pathName.substring(index + 1));
	}

	@Override
	public Preferences parent() {
		return parent;
	}

	@Override
	public void put(final String key, final String value) {
		if (key == null) {
			throw new IllegalArgumentException("key must not be null");
		}
		if (value == null) {
			throw new IllegalArgumentException("value must not be null");
		}

		ensureConnectedAndLoaded();

		final String oldValue = properties.getProperty(key);
		if (value.equals(oldValue)) {
			return;
		}

		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("[PUT] {} - {}: {}", new Object[] { this, key, value });
		}

		properties.setProperty(key, value);
		propertiesDirty = true;
		firePreferenceEvent(new PreferenceChangeEvent(this, key, oldValue, value));
	}

	@Override
	public void putBoolean(final String key, final boolean value) {
		put(key, value ? TRUE : FALSE);
	}

	@Override
	public void putByteArray(final String key, final byte[] value) {
		if (value == null) {
			throw new IllegalArgumentException("value must not be null");
		}
		try {
			put(key, new String(Base64.encodeBase64(value), CharEncoding.US_ASCII));
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("Java VM does not support US_ASCII encoding? " + e.getMessage());
		}
	}

	@Override
	public void putDouble(final String key, final double value) {
		put(key, Double.toString(value));
	}

	@Override
	public void putFloat(final String key, final float value) {
		put(key, Float.toString(value));
	}

	@Override
	public void putInt(final String key, final int value) {
		put(key, Integer.toString(value));
	}

	@Override
	public void putLong(final String key, final long value) {
		put(key, Long.toString(value));
	}

	@Override
	protected void reconnect() {
		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("Reconnecting preference node {}.", this);
		}

		// load but only if remote is different
		ensureConnectedAndLoaded();
	}

	@Override
	public void remove(final String key) {
		if (key == null) {
			throw new IllegalArgumentException("key must not be null");
		}

		ensureConnectedAndLoaded();

		final String oldValue = properties.getProperty(key);
		if (oldValue == null) {
			return;
		}

		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("[REMOVE] {} - {}", new Object[] { this, key });
		}

		properties.remove(key);
		propertiesDirty = true;
		firePreferenceEvent(new PreferenceChangeEvent(this, key, oldValue, null));
	}

	@Override
	public void removeNode() throws BackingStoreException {
		checkRemoved();

		// clear all the property values. do it "the long way" so
		// everyone gets notification
		final String[] keys = keys();
		for (int i = 0; i < keys.length; i++) {
			remove(keys[i]);
		}

		try {
			// don't remove the scope root from the parent
			if ((parent != null) && (parent instanceof ZooKeeperBasedPreferences)) {
				// remove the node from the parent's collection and notify listeners
				removed = true;
				((ZooKeeperBasedPreferences) parent).childRemoved(this);
			}

			// remove all the children (do it "the long way" so everyone gets notified)
			final Collection<ZooKeeperBasedPreferences> childNodes = children.values();
			for (final ZooKeeperBasedPreferences child : childNodes) {
				try {
					child.removeNode();
				} catch (final IllegalStateException e) {
					// ignore since we only get this exception if we have already
					// been removed. no work to do.
				}
			}

		} finally {
			// clear any listeners and caches
			if (removed) {
				connected.set(false);
				nodeListeners = null;
				preferenceListeners = null;
				properties.clear();
				children.clear();
				propertiesVersion = -1;
				childrenVersion = -1;
			}
		}

	}

	@Override
	public void removeNodeChangeListener(final INodeChangeListener listener) {
		if (nodeListeners != null) {
			nodeListeners.remove(listener);
		}
	}

	@Override
	public void removePreferenceChangeListener(final IPreferenceChangeListener listener) {
		if (preferenceListeners != null) {
			preferenceListeners.remove(listener);
		}
	}

	private void saveChildren() throws BackingStoreException, KeeperException, InterruptedException, IOException {
		// don't do anything if removed
		if (removed) {
			return;
		}

		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("Saving children of node {} (cversion {})", this, childrenVersion);
		}

		childrenModifyLock.lock();
		try {
			if (removed) {
				return;
			}

			// recursively flush children
			final Collection<ZooKeeperBasedPreferences> childNodes = children.values();
			for (final ZooKeeperBasedPreferences child : childNodes) {
				child.flush();
			}

			// remove children marked for removal
			for (final String childName : pendingChildRemovals) {
				final IPath childPath = zkPath.append(childName);
				if (CloudDebug.zooKeeperPreferences) {
					LOG.debug("Removing child node {} at {}", childName, childPath);
				}
				ZooKeeperGate.get().deletePath(childPath);
			}
			pendingChildRemovals.clear();

			// note, we hold the childrenModifyLock which prevents concurrent ZK event to cause updates
			// but remote clients still rely on the ZK events to refresh the children version
			// however, we do not reset the cversion here in order to ensure that only *later* events are processed
			//childrenVersion = -1;

		} finally {
			childrenModifyLock.unlock();
		}
	}

	private void saveProperties() throws KeeperException, InterruptedException, IOException {
		// don't do anything if removed
		if (removed) {
			return;
		}

		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("Saving properties of node {} (version {})", this, propertiesVersion);
		}

		// don't flush if not dirty
		if (!propertiesDirty) {
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Aborting property saving of node {} - properties not dirty", this);
			}
			return;
		}

		// prevent concurrent property modification (eg. remote _and_ local flush)
		propertiesLoadOrSaveLock.lock();
		try {
			if (removed) {
				return;
			}

			// collect properties to save
			final Properties toSave = new Properties();
			final Set<Object> keys = properties.keySet();
			for (final Object key : keys) {
				final Object value = properties.get(key);
				if (value != null) {
					toSave.put(key, value);
				}
			}
			toSave.put(VERSION_KEY, VERSION_VALUE);

			// convert to bytes
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			toSave.store(out, null);

			// save record data
			// (note, we do it within the lock in order to get proper stats/version info)
			propertiesVersion = ZooKeeperGate.get().writeRecord(zkPath, CreateMode.PERSISTENT, out.toByteArray()).getVersion();

			// mark clean
			propertiesDirty = false;

			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Saved properties of node {} (now at version {})", this, propertiesVersion);
			}

		} finally {
			propertiesLoadOrSaveLock.unlock();
		}
	}

	@Override
	public void sync() throws BackingStoreException {
		ensureConnectedAndLoaded();

		// sync tree
		syncTree();

		// flush
		flush();
	}

	void syncTree() throws BackingStoreException {
		// load properties & children
		try {
			loadProperties(true);
			loadChildren(true);
		} catch (final Exception e) {
			// assume not connected
			connected.set(false);
			// throw
			throw new BackingStoreException(String.format("Error loading node data '%s' from ZooKeeper. %s", this, ExceptionUtils.getMessage(e)), e);
		}

		// sync children
		childrenModifyLock.lock();
		try {
			if (removed) {
				return;
			}

			for (final ZooKeeperBasedPreferences child : children.values()) {
				child.syncTree();
			}
		} finally {
			childrenModifyLock.unlock();
		}
	}
}
