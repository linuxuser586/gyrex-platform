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
package org.eclipse.gyrex.preferences.internal;

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
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate.IConnectionMonitor;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperMonitor;
import org.eclipse.gyrex.preferences.PlatformScope;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ZooKeeper based preferences.
 */
public class ZooKeeperBasedPreferences implements IEclipsePreferences {

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperBasedPreferences.class);

	private static final String VERSION_KEY = "gyrex.preferences.version"; //$NON-NLS-1$
	private static final String VERSION_VALUE = "1"; //$NON-NLS-1$

	private static final String[] EMPTY_NAMES_ARRAY = new String[0];
	private static final String PATH_SEPARATOR = String.valueOf(IPath.SEPARATOR);
	private static final String EMPTY_STRING = "";
	private static final String FALSE = Boolean.FALSE.toString();
	private static final String TRUE = Boolean.TRUE.toString();

	/** connection monitor to sync the loaded preference tree */
	static final IConnectionMonitor connectionMonitor = new IConnectionMonitor() {
		private final AtomicReference<Job> connectJobRef = new AtomicReference<Job>();

		@Override
		public void connected() {
			final Job job = new Job("Activating preferences hierarchy.") {
				private static final int MAX_CONNECT_DELAY = 240000;
				private volatile int delay = 1000;

				private int nextDelay() {
					return delay = delay < MAX_CONNECT_DELAY ? delay * 2 : MAX_CONNECT_DELAY;
				}

				@Override
				protected IStatus run(final IProgressMonitor monitor) {
					try {
						final Preferences node = PreferencesActivator.getInstance().getPreferencesService().getRootNode().node(PlatformScope.NAME);

						// activate
						LOG.info("Activating preferences hierarchy for node {}.", node.absolutePath());
						((ZooKeeperBasedPreferences) node).syncTree();

						// clear job references
						connectJobRef.compareAndSet(this, null);

						// done
						return Status.OK_STATUS;
					} catch (final BackingStoreException e) {
						// get back-off delay
						final int nextDelay = nextDelay();

						// log error
						LOG.error("Unable to activate platform preferences. Will retry in {} seconds. {}", new Object[] { nextDelay, e.getMessage(), e });

						// re-try later
						schedule(nextDelay);

						// indicate not successful
						return Status.CANCEL_STATUS;
					}
				}
			};
			job.setSystem(true);
			if (connectJobRef.compareAndSet(null, job)) {
				job.schedule();
			}
		}

		@Override
		public void disconnected() {
			// cancel activation job
			final Job job = connectJobRef.getAndSet(null);
			if (job != null) {
				job.cancel();
			}

			// set dis-connected
			try {
				final Preferences node = PreferencesActivator.getInstance().getPreferencesService().getRootNode().node(PlatformScope.NAME);
				((ZooKeeperBasedPreferences) node).connected.set(false);
			} catch (final Exception e) {
				// ignore (maybe already disconnected)
			}

			LOG.info("De-activated ZooKeeper preferences.");
		}
	};

	private final IEclipsePreferences parent;
	private final String name;
	private final IPath path;
	private final IPath zkPath;
	private final ConcurrentMap<String, ZooKeeperBasedPreferences> children = new ConcurrentHashMap<String, ZooKeeperBasedPreferences>(4);
	private final Properties properties = new Properties();

	/** connected state */
	final AtomicBoolean connected = new AtomicBoolean();

	/** prevent concurrent modifications of children */
	private final Lock childrenModifyLock = new ReentrantLock();

	/** prevent concurrent modifications of properties */
	private final Lock propertiesLoadOrSaveLock = new ReentrantLock();

	private final ZooKeeperMonitor monitor = new ZooKeeperMonitor() {

		@Override
		protected void childCreated(final String path) {
			if (PreferencesDebug.debug) {
				LOG.debug("Node {} updated remotely: NEW CHILD", path);
			}

			if (removed) {
				return;
			}

			try {
				// refresh children and notify listeners
				loadChildren(true);
			} catch (final Exception e) {
				// assume not connected
				connected.set(false);
				// log warning
				LOG.warn("Error refreshing children of node {}. {}", this, e.getMessage());
			}
		}

		@Override
		protected void closing(final Code reason) {
			// this is pretty bad, we need to re-establish the connection somehow
			// for now, simply mark as dis-connected in order to try to recover later
			// TODO we may need to hook with the gate to be informed about connection state
			connected.set(false);
		};

		@Override
		protected void pathCreated(final String path) {
			if (PreferencesDebug.debug) {
				LOG.debug("Node {} updated remotely: CREATED", path);
			}

			// process events only for this node
			if (zkPath.toString().equals(path)) {

				// unset removed state
				removed = false;

				try {
					// refresh properties (and force sync with remote)
					loadProperties(true);

					// refresh children
					loadChildren(true);
				} catch (final Exception e) {
					// assume not connected
					connected.set(false);
					// log warning
					LOG.warn("Error refreshing node {}. {}", this, e.getMessage());
				}
			}
		};

		@Override
		protected void pathDeleted(final String path) {
			if (PreferencesDebug.debug) {
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
					LOG.warn("Error removing node {}. {}", this, e.getMessage());
				}
			}
		}

		@Override
		protected void recordChanged(final String path) {
			if (PreferencesDebug.debug) {
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
					LOG.warn("Error refreshing properties of node {}. {}", this, e.getMessage());
				}
			}
		}
	};

	volatile boolean removed;

	/** ZooKeeper version of the properties object */
	private volatile int propertiesVersion;

	/** simple attempt to avoid unnecessary flushes */
	private volatile boolean propertiesDirty;

	private volatile ListenerList nodeListeners;
	private volatile ListenerList preferenceListeners;

	/**
	 * Creates a new instance.
	 * 
	 * @param parent
	 * @param name
	 */
	public ZooKeeperBasedPreferences(final IEclipsePreferences parent, final String name) {
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
		zkPath = IZooKeeperLayout.PATH_PREFERENCES_ROOT.append(path);
	}

	@Override
	public String absolutePath() {
		return path.toString();
	}

	@Override
	public void accept(final IPreferenceNodeVisitor visitor) throws BackingStoreException {
		ensureConnected();
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
			wasRemoved = children.remove(child.name()) != null;
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
			ensureConnected();
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
		ensureConnected();

		// call each one separately (instead of Properties.clear) so
		// clients get change notification
		final String[] keys = keys();
		for (int i = 0; i < keys.length; i++) {
			remove(keys[i]);
		}
	}

	private void ensureConnected() throws IllegalStateException {
		checkRemoved();

		if (!connected.get() && connected.compareAndSet(false, true)) {
			try {

				if (PreferencesDebug.debug) {
					LOG.debug("Connecting preference node {}.", this);
				}

				// check if path exists
				if (!ZooKeeperGate.get().exists(zkPath, monitor)) {
					if (PreferencesDebug.debug) {
						LOG.debug("Node {} connected, waiting for remote path to be created ({})", this, zkPath);
					}
					return;
				}

				// load properties (and force sync with remote)
				loadProperties(true);

				// load children (and notify listeners in case we were already active)
				loadChildren(true);
			} catch (final Exception e) {
				if (PreferencesDebug.debug) {
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
		ensureConnected();
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
	}

	@Override
	public String get(final String key, final String def) {
		if (key == null) {
			throw new IllegalArgumentException("key must not be null");
		}

		try {
			ensureConnected();
		} catch (final IllegalStateException e) {
			// the node may be off-line
			// double check for removal but don't fail if off-line
			checkRemoved();
		}

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
		try {
			final String value = get(key, null);
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
	public String[] keys() throws BackingStoreException {
		ensureConnected();

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
	void loadChildren(final boolean sendEvents) throws IllegalStateException, InterruptedException, KeeperException {
		// don't do anything if removed
		if (removed) {
			return;
		}

		final List<ZooKeeperBasedPreferences> addedNodes = sendEvents ? new ArrayList<ZooKeeperBasedPreferences>(3) : null;

		childrenModifyLock.lock();
		try {
			if (removed) {
				return;
			}

			if (PreferencesDebug.debug) {
				LOG.debug("Loading children for node {} from {}", this, zkPath);
			}

			// get list of children
			final Collection<String> childrenNames = ZooKeeperGate.get().readChildrenNames(zkPath, monitor);
			if (childrenNames == null) {
				return;
			}

			// process new nodes
			for (final String name : childrenNames) {
				if (!children.containsKey(name)) {
					final ZooKeeperBasedPreferences child = new ZooKeeperBasedPreferences(this, name);
					children.put(name, child);
					if (sendEvents) {
						addedNodes.add(child);
					}
				}
			}
		} finally {
			childrenModifyLock.unlock();
		}

		// fire events outside of lock
		// TODO we need to understand event ordering better (eg. concurrent remote updates)
		// (this may result in sending events asynchronously through an ordered queue, but for now we do it directly)
		if (sendEvents) {
			for (final ZooKeeperBasedPreferences child : addedNodes) {
				fireNodeEvent(child, true);
			}
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

			if (PreferencesDebug.debug) {
				LOG.debug("Reading properties for node {} (version {}) from ZooKeeper {}", new Object[] { this, propertiesVersion, zkPath });
			}

			// read record data
			final Stat stat = new Stat();
			final byte[] bytes = ZooKeeperGate.get().readRecord(zkPath, monitor, stat);
			if (bytes == null) {
				// node doesn't exist
				// don't do anything here, complete removal is handled elsewhere
				if (PreferencesDebug.debug) {
					LOG.debug("Node {} doesn't exist in ZooKeeper", this);
				}
				return;
			}

			// don't load properties if version is in the past
			if (!forceSyncWithRemoteVersion && (propertiesVersion >= stat.getVersion())) {
				if (PreferencesDebug.debug) {
					LOG.debug("Not updating properties of node {} - local version ({}) >= ZooKeeper version ({})", new Object[] { this, propertiesVersion, stat.getVersion() });
				}
				return;
			}
			propertiesVersion = stat.getVersion();

			// load remote properties
			final Properties loadedProps = new Properties();
			loadedProps.load(new ByteArrayInputStream(bytes));

			// check version
			final Object version = loadedProps.remove(VERSION_KEY);
			if ((version == null) || !VERSION_VALUE.equals(version)) {
				// ignore for now
				LOG.warn("Properties with incompatible storage format version ({}) found for node {}.", version, this);
				return;
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
					if (PreferencesDebug.debug) {
						LOG.debug("Node {} property removed: {}", key);
					}
					// create event
					events.add(new PreferenceChangeEvent(this, key, oldValue, newValue));
				} else if ((oldValue == null) || !oldValue.equals(newValue)) {
					// assume added or updated in ZooKeeper
					properties.put(key, newValue);
					if (PreferencesDebug.debug) {
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

			if (PreferencesDebug.debug) {
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

	@Override
	public Preferences node(final String path) {
		ensureConnected();

		// check if this node is requested
		if (path.length() == 0) {
			return this;
		}

		// use the root relative to this node instead of the global root
		// in case we have a different hierarchy. (e.g. export)
		if (path.charAt(0) == IPath.SEPARATOR) {
			return calculateRoot().node(path.substring(1));
		}

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
						children.put(key, new ZooKeeperBasedPreferences(this, key));
						added = true;
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
		checkRemoved();

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

		ensureConnected();

		final String oldValue = properties.getProperty(key);
		if (value.equals(oldValue)) {
			return;
		}

		if (PreferencesDebug.debug) {
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
	public void remove(final String key) {
		if (key == null) {
			throw new IllegalArgumentException("key must not be null");
		}

		ensureConnected();

		final String oldValue = properties.getProperty(key);
		if (oldValue == null) {
			return;
		}

		if (PreferencesDebug.debug) {
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

			// remove children (will notify listeners)
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

	private void saveChildren() throws BackingStoreException {
		// don't do anything if removed
		if (removed) {
			return;
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
		} finally {
			childrenModifyLock.unlock();
		}
	}

	private void saveProperties() throws KeeperException, InterruptedException, IOException {
		// don't do anything if removed
		if (removed) {
			return;
		}

		if (PreferencesDebug.debug) {
			LOG.debug("Saving properties of node {} (version {})", this, propertiesVersion);
		}

		// don't flush if not dirty
		if (!propertiesDirty) {
			if (PreferencesDebug.debug) {
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

			if (PreferencesDebug.debug) {
				LOG.debug("Saved properties of node {} (now at version {})", this, propertiesVersion);
			}

		} finally {
			propertiesLoadOrSaveLock.unlock();
		}
	}

	@Override
	public void sync() throws BackingStoreException {
		ensureConnected();

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

	@Override
	public String toString() {
		return absolutePath();
	}
}
