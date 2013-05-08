/*******************************************************************************
 * Copyright (c) 2010, 2012 AGETO Service GmbH and others.
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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.gyrex.cloud.internal.CloudDebug;
import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;

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
import org.apache.zookeeper.data.Stat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ZooKeeper based preferences.
 */
public abstract class ZooKeeperBasedPreferences implements IEclipsePreferences {

	private static final class SortedProperties extends Properties {
		private static final long serialVersionUID = 1L;

		@Override
		public synchronized Enumeration<Object> keys() {
			return Collections.enumeration(keySet());
		}

		@Override
		public Set<Object> keySet() {
			return new TreeSet<Object>(super.keySet());
		}
	}

	private static final long RELOAD_AGE = Long.getLong("gyrex.preferences.reloadAfter", 3000L);

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperBasedPreferences.class);

	private static final String VERSION_KEY = "gyrex.preferences.version"; //$NON-NLS-1$
	private static final String VERSION_VALUE = "1"; //$NON-NLS-1$

	private static final String[] EMPTY_NAMES_ARRAY = new String[0];
	private static final String PATH_SEPARATOR = String.valueOf(IPath.SEPARATOR);
	private static final String EMPTY_STRING = "";
	private static final String FALSE = Boolean.FALSE.toString();
	private static final String TRUE = Boolean.TRUE.toString();

	private final ZooKeeperPreferencesService service;
	private final IEclipsePreferences parent;
	private final String name;
	private final IPath path;

	/** the path in ZooKeeper */
	final String zkPath;

	/** the list of children */
	private final ConcurrentMap<String, ZooKeeperBasedPreferences> children = new ConcurrentHashMap<String, ZooKeeperBasedPreferences>(4);

	/** list of children removed locally but not yet flushed to ZooKeeper */
	private final Map<String, ZooKeeperBasedPreferences> pendingChildRemovals = new HashMap<String, ZooKeeperBasedPreferences>(); // guarded by childrenModifyLock

	/** properties of the node */
	private final Properties properties = new Properties();

	/** prevent concurrent modifications of children */
	final Lock childrenModifyLock = new ReentrantLock();

	/** prevent concurrent modifications of properties */
	final Lock propertiesModificationLock = new ReentrantLock();

	/** indicates if the node has been removed */
	volatile boolean removed;

	/** ZooKeeper version of the properties object */
	volatile int propertiesVersion = -1;

	/** ZooKeeper version of the children */
	volatile int childrenVersion = -1;

	/** last time properties have been loaded */
	volatile long propertiesLoadTimestamp;

	/** last time children have been loaded */
	volatile long childrenLoadTimestamp;

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
	public ZooKeeperBasedPreferences(final IEclipsePreferences parent, final String name, final ZooKeeperPreferencesService service) {
		if (parent == null) {
			throw new IllegalArgumentException("parent must not be null");
		}
		if (name == null) {
			throw new IllegalArgumentException("name must not be null");
		}
		if (service == null) {
			throw new IllegalArgumentException("service must not be null");
		}
		this.service = service;
		this.parent = parent;
		this.name = name;

		// cache path computed based on parent
		if (parent.absolutePath().equals(PATH_SEPARATOR)) {
			path = new Path(name).makeAbsolute();
		} else {
			path = new Path(parent.absolutePath()).append(name).makeAbsolute();
		}

		// pre-compute and cache ZooKeeper path
		zkPath = IZooKeeperLayout.PATH_PREFERENCES_ROOT.append(path).toString();

		// immediately activate the node if this is the scope root
		// all others will be activated in createChild
		if (!(parent instanceof ZooKeeperBasedPreferences)) {
			if (null != service.activeNodesByPath.putIfAbsent(zkPath, this)) {
				throw new IllegalStateException(String.format("programming/concurrency error: created a new scope root although one already existed (new=%s) (%s)", this, service));
			}
		}
	}

	@Override
	public String absolutePath() {
		return path.toString();
	}

	@Override
	public void accept(final IPreferenceNodeVisitor visitor) throws BackingStoreException {
		ensureLoaded();
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

		// make an attempt to load the node if a listener is added
		// (this ensures that any watch is properly hooked with ZooKeeper)
		ensureLoadedIfPossible();

		// add listener
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

		// make an attempt to load the node if a listener is added
		// (this ensures that any watch is properly hooked with ZooKeeper)
		ensureLoadedIfPossible();

		// add listener
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
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Node {} has been removed! Operation will fail.", new Object[] { this });
			}
			throw new IllegalStateException(String.format("Node '%s' has been removed.", name));
		}
	}

	@Override
	public String[] childrenNames() throws BackingStoreException {
		try {
			ensureLoaded();
			final Set<String> names = children.keySet();
			return !names.isEmpty() ? (String[]) names.toArray(EMPTY_NAMES_ARRAY) : EMPTY_NAMES_ARRAY;
		} catch (final Exception e) {
			// re-throw any exception as BackingStoreException
			throw createBackingStoreException("reading children", e);
		}
	}

	@Override
	public void clear() throws BackingStoreException {
		// ensure loaded
		ensureLoaded();

		try {

			// call each one separately (instead of Properties.clear) so
			// clients get change notification
			final String[] keys = keys();
			for (int i = 0; i < keys.length; i++) {
				remove(keys[i]);
			}
		} catch (final Exception e) {
			// re-throw any exception as BackingStoreException
			throw createBackingStoreException("removing properties", e);
		}
	}

	protected BackingStoreException createBackingStoreException(final String action, final Exception cause) {
		return new BackingStoreException(String.format("Error %s (node %s). %s", action, absolutePath(), null != cause.getMessage() ? cause.getMessage() : ExceptionUtils.getMessage(cause)), cause);
	}

	private ZooKeeperBasedPreferences createChild(final String name, final List<ZooKeeperBasedPreferences> added) {
		// prevent concurrent modification
		childrenModifyLock.lock();
		try {
			// create child only if necessary
			ZooKeeperBasedPreferences child = children.get(name);
			if (null != child) {
				return child;
			}

			// create new child
			child = newChild(name);
			final ZooKeeperBasedPreferences existingChild = children.put(name, child);
			if ((null != existingChild) && (existingChild != child)) {
				// this shouldn't happen (because of the lock)
				// but let's be really sure
				throw new AssertionError(String.format("programming/concurrency error: created a new child although one still existed (new=%s %d) (old=%s %d)", child, System.identityHashCode(child), existingChild, System.identityHashCode(existingChild)));
			}

			// log message
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Node {} child created: {} ", this, name);
			}

			// register with service
			service.activateNode(child);

			// remove from pending removals list
			pendingChildRemovals.remove(name);

			// trigger event
			if (null != added) {
				added.add(child);
			}
			return child;
		} finally {
			childrenModifyLock.unlock();
		}
	}

	final void dispose() {
		// reset data and listeners
		nodeListeners = null;
		preferenceListeners = null;
		properties.clear();
		children.clear();

		// but do not reset versions, they are required for
		// resolving conflicts when loading children
	}

	/**
	 * Implementation of {@link #remove(String)} without failing if the node has
	 * been removed.
	 * 
	 * @param key
	 */
	private void doRemove(final String key) {
		final String oldValue = properties.getProperty(key);
		if (oldValue == null) {
			return;
		}

		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("[REMOVE] {} - {}", new Object[] { this, key });
		}

		// prevent concurrent property modification (eg. remote _and_ local flush)
		propertiesModificationLock.lock();
		try {
			if (null == properties.remove(key)) {
				// had been removed concurrently
				if (CloudDebug.zooKeeperPreferences) {
					LOG.debug("[REMOVE] Aborted due to concurrent removal. {} - {}", new Object[] { this, key });
				}
				return;
			}
		} finally {
			propertiesModificationLock.unlock();
		}

		// fire change event outside of lock
		firePreferenceEvent(new PreferenceChangeEvent(this, key, oldValue, null));
	}

	/**
	 * Ensures a node that should be loaded is loaded.
	 * 
	 * @throws BackingStoreException
	 *             if the node could not be loaded (eg. the system is not
	 *             connected)
	 */
	private void ensureLoaded() throws BackingStoreException {
		// check removed
		checkRemoved();

		// prevent too frequent load attempts
		if ((propertiesLoadTimestamp > (System.currentTimeMillis() - RELOAD_AGE)) && (childrenLoadTimestamp > (System.currentTimeMillis() - RELOAD_AGE))) {
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Node had been loaded recently. Skipping load request for node {}!", this);
			}
			return;
		}

		try {
			// load (if necessary)
			if (shouldLoad()) {
				if (CloudDebug.zooKeeperPreferences) {
					LOG.debug("Ensuring that node {} (version {}, cversion {}) is loaded!", new Object[] { this, propertiesVersion, childrenVersion });
				}
				service.loadNode(zkPath, true);
			}

			// update load timestamps
			propertiesLoadTimestamp = childrenLoadTimestamp = System.currentTimeMillis();
		} catch (final Exception e) {
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Exception while connecting node {}: {}", new Object[] { this, ExceptionUtils.getRootCauseMessage(e), e });
			}

			// throw BackingStoreException
			throw createBackingStoreException("loading node", e);
		}
	}

	private void ensureLoadedIfPossible() throws IllegalStateException {
		// check removed
		checkRemoved();

		// prevent too frequent load attempts
		if ((propertiesLoadTimestamp > (System.currentTimeMillis() - RELOAD_AGE)) && (childrenLoadTimestamp > (System.currentTimeMillis() - RELOAD_AGE))) {
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Node had been loaded recently. Skipping load request for node {}!", this);
			}
			return;
		}

		try {
			// load (if possible and necessary)
			if (shouldLoad()) {
				if (CloudDebug.zooKeeperPreferences) {
					LOG.debug("Ensuring (if possible) that node {} (version {}, cversion {}) is loaded!", new Object[] { this, propertiesVersion, childrenVersion });
				}
				service.loadNode(zkPath, false);

				// update load timestamps
				propertiesLoadTimestamp = childrenLoadTimestamp = System.currentTimeMillis();
			}
		} catch (final Exception e) {
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Exception while loading node {}: {}", new Object[] { this, ExceptionUtils.getRootCauseMessage(e), e });
			}

			// throw
			throw new IllegalStateException(String.format("Error loading node '%s'. %s", this, e.getMessage()), e);
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
				try {
					if (added) {
						((INodeChangeListener) listener).added(event);
					} else {
						((INodeChangeListener) listener).removed(event);
					}
				} catch (final AssertionError e) {
					LOG.error("Removing bogus node listener ({}) after exception.", listener, e);
					listeners.remove(listener);
				} catch (final LinkageError e) {
					LOG.error("Removing bogus node listener ({}) after exception.", listener, e);
					listeners.remove(listener);
				} catch (final Exception e) {
					LOG.error("Removing bogus node listener ({}) after exception.", listener, e);
					listeners.remove(listener);
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
		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("Sending event {}.", event);
		}
		final ListenerList listeners = preferenceListeners;
		if (listeners != null) {
			for (final Object listener : listeners.getListeners()) {
				try {
					if (CloudDebug.zooKeeperPreferences) {
						LOG.debug("Sending event to {}.", listener);
					}
					((IPreferenceChangeListener) listener).preferenceChange(event);
				} catch (final AssertionError e) {
					LOG.error("Removing bogus preference listener ({}) after exception.", listener, e);
					listeners.remove(listener);
				} catch (final LinkageError e) {
					LOG.error("Removing bogus preference listener ({}) after exception.", listener, e);
					listeners.remove(listener);
				} catch (final Exception e) {
					LOG.warn("Removing bogus preference listener ({}) after exception. ", listener, e);
					listeners.remove(listener);
				}
			}
		}
	}

	@Override
	public void flush() throws BackingStoreException {
		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("Flushing node {} (version {}, cversion {})", new Object[] { this, propertiesVersion, childrenVersion });
		}

		// ensure active
		ensureLoaded();

		// prevent concurrent children modification (eg. remote _and_ local flush)
		// (note, it is important to do this early; in case a node does not exist
		// the saveProperties will create it which will trigger ZooKeeper watches that
		// may result in refreshing children for this node)
		childrenModifyLock.lock();
		try {
			checkRemoved();

			// prevent concurrent property modification (eg. remote _and_ local flush)
			propertiesModificationLock.lock();
			try {
				checkRemoved();

				// save properties
				saveProperties();
			} finally {
				propertiesModificationLock.unlock();
			}

			// initialize children version
			// (this is required in order to work around a concurrency issue in ZooKeeper;
			// when saving properties the node might be created; in this case the cversion start
			// with 0; this conflicts with our loadChildren logic which)
			// TODO: we may be able to solve this when upgrading to ZooKeeper 3.4 and implementing multi-operations
			childrenVersion = Math.max(0, childrenVersion);

			// save children
			saveChildren();
		} catch (final Exception e) {
			// re-throw any exception as BackingStoreException
			throw createBackingStoreException("flushing node", e);
		} finally {
			childrenModifyLock.unlock();
		}

		// log a message that the node has been flushed
		if (CloudDebug.zooKeeperPreferences) {
			LOG.info("Flushed node {} (version {}, cversion {})", new Object[] { this, propertiesVersion, childrenVersion });
		}

	}

	@Override
	public String get(final String key, final String def) {
		if (key == null) {
			throw new IllegalArgumentException("key must not be null");
		}

		// make an attempt to load the node if this node is accessed
		// for read purposes without having any values; this it likely
		// indicates that someone wants to just "read" a value; thus
		// we try to be smart and load any remote data that is available
		if (shouldLoad() && properties.isEmpty()) {
			ensureLoadedIfPossible();
		} else {
			// just check if removed
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

	/**
	 * Returns the preference service.
	 * 
	 * @return the preference service
	 */
	protected ZooKeeperPreferencesService getService() {
		return service;
	}

	@Override
	public String[] keys() throws BackingStoreException {
		// ensure active
		ensureLoaded();

		if (properties.isEmpty()) {
			return EMPTY_NAMES_ARRAY;
		}

		final Set<String> names = properties.stringPropertyNames();
		return names.toArray(EMPTY_NAMES_ARRAY);
	}

	/**
	 * Updates the local node children with children from ZooKeeper.
	 * <p>
	 * This method is called by {@link ZooKeeperPreferencesService} when
	 * children have been loaded from ZooKeeper.
	 * <p>
	 * However, in contrast to {@link #loadProperties(byte[], int)} this method
	 * will only process children which don't exists locally. This is necessary
	 * because of the way we interact with ZooKeeper and the functionality
	 * provided by ZooKeeper.
	 * </p>
	 * <p>
	 * When a preference node is created locally it's not written to ZooKeeper
	 * immediately. Instead, it's only created in ZooKeeper when someone
	 * actually calls {@link #flush()}. When this happens, ZooKeeper may trigger
	 * a children-change on the node parents. But because of the distributed
	 * nature the flush may still be ongoing. As a result, any children
	 * refreshing would have to be deferred till everything in the tree has been
	 * flushed. But a user may explicitly only flush a specific child node and
	 * <em>not</em> a sibling. In this case, a replace implementation would
	 * silently remove the sibling when the children-change is triggered by
	 * ZooKeeper for the parent. To avoid that we ould have to implement
	 * additional state that keeps track of this. Thus, a design decision was
	 * made to only process added nodes when a children-change is triggered.
	 * This also fits nicely into the {@link #flush()} & {@link #sync()}
	 * contract.
	 * </p>
	 * <p>
	 * Local, unflushed removals are handled internally by remembering nodes
	 * which has been removed ({@link #pendingChildRemovals}). When this method
	 * is called, unflushed local removals will be restored <em>if</em> the
	 * remote child changed. This decision was made because we don't want to let
	 * nodes diverge at runtime. The child's {@link #propertiesVersion} will be
	 * used to resolve conflicts. For example, when a child node is removed
	 * locally and some other child node of the same parent is updated remotely
	 * we must abort the local removal because we cannot reliably guarantee
	 * which change should take precedence.
	 * </p>
	 * 
	 * @param remoteChildrenNames
	 * @param childrenVersion
	 */
	final void loadChildren(final Collection<String> remoteChildrenNames, final int childrenVersion) throws Exception {
		// don't do anything if removed
		if (removed) {
			return;
		}

		// collect events
		final List<ZooKeeperBasedPreferences> addedNodes = new ArrayList<ZooKeeperBasedPreferences>(3);
		final List<ZooKeeperBasedPreferences> removedNodes = new ArrayList<ZooKeeperBasedPreferences>(3);

		childrenModifyLock.lock();
		try {
			if (removed) {
				return;
			}

			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Loading children for node {} (cversion {})", this, childrenVersion);
			}

			// update children version
			this.childrenVersion = childrenVersion;
			childrenLoadTimestamp = System.currentTimeMillis();

			// note, the policy here is very simple: we completely
			// replace the local children with the loaded children;
			// this keeps the implementation simple and also delegates
			// the coordination of concurrent updates in a distributed
			// system a layer higher to the clients of preferences API

			// discover added children
			for (final String name : remoteChildrenNames) {

				// detect existing nodes
				if (children.containsKey(name)) {
					// all fine, nothing changes
					// (well, maybe the remote child changed as well but there are separate events for that)
					continue;
				}

				// detect conflicting local removals
				final ZooKeeperBasedPreferences removedNode = pendingChildRemovals.get(name);
				if (null != removedNode) {
					final Stat versionInfo = service.getVersionInfo(removedNode.zkPath);
					if (null == versionInfo) {
						// already removed remotely (the removal doesn't need to flushed anymore)
						// we simply drop it from the pendingChildRemovals silently
						for (final String child : pendingChildRemovals.keySet()) {
							LOG.debug("Node {} child also removed remotely: {}", this, child);
						}
						pendingChildRemovals.remove(name);
					} else if ((versionInfo.getVersion() == removedNode.propertiesVersion) && (versionInfo.getCversion() == removedNode.childrenVersion)) {
						// we keep the pending flush in this case because the remote didn't change
						// thus, we are still good for a flush, i.e. keep it in the pendingChildRemovals
						for (final String child : pendingChildRemovals.keySet()) {
							LOG.debug("Node {} keeping unflushed local removal for child: {}", this, child);
						}
						continue;
					} else {
						for (final String child : pendingChildRemovals.keySet()) {
							LOG.debug("Node {} restored local removal for child due to newer remot version: {}", this, child);
						}
						pendingChildRemovals.remove(name);
					}
				}

				// does not exist locally, assume added in ZooKeeper
				final ZooKeeperBasedPreferences child = createChild(name, addedNodes);

				// log message
				if (CloudDebug.zooKeeperPreferences) {
					LOG.debug("Node {} child added: {} ", this, child);
				}
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
		for (final ZooKeeperBasedPreferences child : removedNodes) {
			fireNodeEvent(child, false);
		}
	}

	/**
	 * Updates the local node properties with properties from ZooKeeper.
	 * <p>
	 * This method is called by {@link ZooKeeperPreferencesService} when
	 * properties have been loaded from ZooKeeper.
	 * <p>
	 * The local properties will be completely replaced with the properties
	 * loaded from the specified bytes. Properties that exist locally but not
	 * remotely will be removed locally. Properties that exist remotely but not
	 * locally will be added locally. Proper events will be fired.
	 * </p>
	 * <p>
	 * The replace strategy relies on the node version provided by ZooKeeper.
	 * The version of a ZooKeeper node is used as the properties version. When
	 * writing properties to ZooKeeper we'll receive a response
	 * </p>
	 * 
	 * @param remotePropertyBytes
	 * @param propertiesVersion
	 * @throws IOException
	 */
	final void loadProperties(final byte[] remotePropertyBytes, final int propertiesVersion) throws IOException {
		// don't do anything if removed
		if (removed) {
			return;
		}

		// collect events
		final List<PreferenceChangeEvent> events = new ArrayList<PreferenceChangeEvent>();

		// prevent concurrent property modification (eg. remote _and_ local flush)
		propertiesModificationLock.lock();
		try {
			if (removed) {
				return;
			}

			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Loading properties for node {} (version {})", this, propertiesVersion);
			}

			// load remote properties
			// (note, can be null if there is a node in ZooKeeper but without data)
			final Properties loadedProps = new Properties();
			if (remotePropertyBytes != null) {
				loadedProps.load(new ByteArrayInputStream(remotePropertyBytes));

				// check version
				final Object formatVersion = loadedProps.remove(VERSION_KEY);
				if ((formatVersion == null) || !VERSION_VALUE.equals(formatVersion)) {
					// ignore for now
					LOG.warn("Properties with incompatible storage format version ({}) found for node {}.", formatVersion, this);
					return;
				}
			}

			// update properties version (after they were de-serialized successfully)
			this.propertiesVersion = propertiesVersion;
			propertiesLoadTimestamp = System.currentTimeMillis();

			// collect all property names
			final Set<String> propertyNames = new HashSet<String>();
			propertyNames.addAll(loadedProps.stringPropertyNames());
			propertyNames.addAll(properties.stringPropertyNames());

			// note, the policy here is very simple: we completely
			// replace the local properties with the loaded properties;
			// this keeps the implementation simple and also delegates
			// the coordination of concurrent updates in a distributed
			// system a layer higher to the clients of preferences API

			// discover new, updated and removed properties
			for (final String key : propertyNames) {
				final String newValue = loadedProps.getProperty(key);
				final String oldValue = properties.getProperty(key);
				if (newValue == null) {
					// does not exists in ZooKeeper, assume removed
					properties.remove(key);
					if (CloudDebug.zooKeeperPreferences) {
						LOG.debug("Node {} property removed: {}", this, key);
					}
					// create event
					events.add(new PreferenceChangeEvent(this, key, oldValue, newValue));
				} else if ((oldValue == null) || !oldValue.equals(newValue)) {
					// assume added or updated in ZooKeeper
					properties.put(key, newValue);
					if (CloudDebug.zooKeeperPreferences) {
						if (oldValue == null) {
							LOG.debug("Node {} property added: {}={}", new Object[] { this, key, newValue });
						} else {
							LOG.debug("Node {} property updated: {}={}", new Object[] { this, key, newValue });
						}
					}
					// create event
					events.add(new PreferenceChangeEvent(this, key, oldValue, newValue));
				}
			}

			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Loaded properties for node {} (now at version {})", this, propertiesVersion);
			}
		} finally {
			propertiesModificationLock.unlock();
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
		if (null == path) {
			throw new IllegalArgumentException("path name must not be null");
		}

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

		// get existing child (even if not connected)
		final int index = path.indexOf(IPath.SEPARATOR);
		final String key = index == -1 ? path : path.substring(0, index);
		ZooKeeperBasedPreferences child = children.get(key);

		// create the child locally if it doesn't exist
		final List<ZooKeeperBasedPreferences> added = new ArrayList<ZooKeeperBasedPreferences>(1);
		if (child == null) {
			// make an attempt to load the node if this node is accessed
			// for read purposes without having any children; this likely
			// indicates that someone wants to traverse the tree; thus
			// we try to be smart and load any remote data that is available
			if (shouldLoad() && children.isEmpty()) {
				ensureLoadedIfPossible();
			}

			// create child
			child = createChild(key, added);
		}

		// notify listeners if a child was added
		if (!added.isEmpty()) {
			for (final ZooKeeperBasedPreferences addedChild : added) {
				fireNodeEvent(addedChild, true);
			}
		}
		return child.node(index == -1 ? EMPTY_STRING : path.substring(index + 1));
	}

	@Override
	public boolean nodeExists(final String pathName) throws BackingStoreException {
		if (null == pathName) {
			throw new IllegalArgumentException("path name must not be null");
		}

		// if removed it is still legal to invoke this method, but only with the pathname ""
		// (note, if not removed and the pathname is "" check again after ensuring the node is connected)
		if (removed && (pathName.length() == 0)) {
			// as per contract, the only valid result is FALSE
			return false;
		}

		// in all other cases we must throw an IllegalStateException if the node has been removed
		checkRemoved();

		// use the root relative to this node instead of the global root
		// in case we have a different hierarchy. (e.g. during export)
		if ((pathName.length() > 0) && (pathName.charAt(0) == IPath.SEPARATOR)) {
			return calculateRoot().nodeExists(pathName.substring(1));
		}

		// in order to properly check if the node exists we ensure its fully loaded
		ensureLoaded();

		// now check again if a check for this node is requested
		if (pathName.length() == 0) {
			return !removed;
		}

		final int index = pathName.indexOf(IPath.SEPARATOR);
		final boolean noSlash = index == -1;

		// if we are looking for a simple child then just look in the table and return
		if (noSlash) {
			return children.containsKey(pathName);
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

		// make an attempt to load the node if this node is accessed
		// for write purposes without having any values; this likely
		// indicates that someone wants to set some values; thus
		// we try to be smart and load any remote data that is available
		if (shouldLoad() && properties.isEmpty()) {
			ensureLoadedIfPossible();
		} else {
			// just check if removed
			checkRemoved();
		}

		final String oldValue = properties.getProperty(key);
		if (value.equals(oldValue)) {
			return;
		}

		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("[PUT] {} - {}: {}", new Object[] { this, key, value });
		}

		// prevent concurrent property modification (eg. remote _and_ local flush)
		propertiesModificationLock.lock();
		try {
			if (value.equals(properties.setProperty(key, value))) {
				// had been update concurrently to the same value
				if (CloudDebug.zooKeeperPreferences) {
					LOG.debug("[PUT] Aborted due to concurrent modification to the same value. {} - {}", new Object[] { this, key });
				}
				return;
			}
		} finally {
			propertiesModificationLock.unlock();
		}

		// fire change event outside of lock
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

		// make an attempt to load the node if this node is accessed
		// for write purposes without having any values; this likely
		// indicates that someone wants to update some values; thus
		// we try to be smart and load any remote data that is available
		if (shouldLoad() && properties.isEmpty()) {
			ensureLoadedIfPossible();
		} else {
			// just check if removed
			checkRemoved();
		}

		doRemove(key);
	}

	/**
	 * Called by children when a child was removed.
	 * <p>
	 * The child will be deleted from the list of children. If the delete was
	 * not triggered remotely, it will also be recorded for remote removal on
	 * flush.
	 * </p>
	 * 
	 * @param child
	 * @param triggeredRemotely
	 */
	private boolean removeChild(final ZooKeeperBasedPreferences child, final boolean triggeredRemotely) {
		// prevent concurrent modification (eg. remote and local removal)
		childrenModifyLock.lock();
		try {
			// remove child (only if possible)
			if (!children.remove(child.name(), child)) {
				return false;
			}

			// immediatly mark the child removed
			child.removed = true;

			// log message
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Node {} child removed: {} ", this, child.name());
			}

			// remove from the service
			service.deactivateNode(child);

			// remember removals for flush (if this is not a remote triggered removal)
			if (!triggeredRemotely) {
				pendingChildRemovals.put(child.name(), child);
			}
		} finally {
			childrenModifyLock.unlock();
		}

		// trigger event outside of lock
		fireNodeEvent(child, false);

		// report success
		return true;
	}

	@Override
	public void removeNode() throws BackingStoreException {
		removeNode(false);
	}

	/**
	 * Implements {@link #removeNode() local removal} but also allows to remove
	 * a node after the fact, i.e. when it has been removed remotely.
	 * 
	 * @param triggeredRemotely
	 * @throws BackingStoreException
	 */
	final void removeNode(final boolean triggeredRemotely) {
		// check if already removed (but abort silently if this was triggered remotely)
		if (triggeredRemotely && removed) {
			return;
		} else {
			checkRemoved();
		}

		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("Removing node {} (version {}, cversion {})", new Object[] { this, propertiesVersion, childrenVersion });
		}

		// remove from the parent (but only if this is not the scope root)
		if (parent instanceof ZooKeeperBasedPreferences) {
			// remove the node from the parent's collection and notify listeners
			if (!((ZooKeeperBasedPreferences) parent).removeChild(this, triggeredRemotely)) {
				// sanity check
				if (!removed) {
					throw new AssertionError(String.format("programming/concurrency error: node (%s) must be removed at this point (parent %s)", this, parent));
				}
				return;
			}
		}

		// at this point the node was successfully removed from the parent
		// we continue removing all the keys and children outside of any
		// locks and clean-up afterwards
		try {

			// clear all the property values. do it "the long way" so
			// everyone gets notification
			while (!properties.isEmpty()) {
				final String[] keys = properties.stringPropertyNames().toArray(new String[0]);
				for (int i = 0; i < keys.length; i++) {
					doRemove(keys[i]);
				}
			}

			// remove all the children (do it "the long way" so everyone gets notified)
			final Collection<ZooKeeperBasedPreferences> childNodes = children.values();
			for (final ZooKeeperBasedPreferences child : childNodes) {
				try {
					child.removeNode(triggeredRemotely);
				} catch (final IllegalStateException e) {
					// ignore since we only get this exception if we have already
					// been removed. no work to do.
				}
			}

		} finally {
			// clear any listeners and caches
			dispose();
		}

		if (CloudDebug.zooKeeperPreferences) {
			LOG.info("Removed node {} (version {}, cversion {}{})", new Object[] { this, propertiesVersion, childrenVersion, triggeredRemotely ? ", TRIGGERED REMOTELY" : "" });
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
	};

	private void saveChildren() throws Exception {
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

			// recursively flush children (which will create any new path in ZooKeeper)
			for (final ZooKeeperBasedPreferences child : children.values()) {
				child.flush();
			}

			// remove children marked for removal
			for (final ZooKeeperBasedPreferences child : pendingChildRemovals.values()) {
				if (CloudDebug.zooKeeperPreferences) {
					LOG.debug("Removing child node {}", child);
				}
				service.removeNode(child.zkPath, child.propertiesVersion, child.childrenVersion);
			}
			pendingChildRemovals.clear();

			// there is an issue with childrenVersion; ZooKeeper has no atomic way to set/get/sync
			// children; for example, when creating an empty node in ZooKeeper the childrenVersion is 0;
			// this conflicts with a new node with children and #loadChildren call triggered by a watcher
			// which would remove all children (after childrenModifyLock is released) because this nodes
			// childrenVersion is still -1;
			// the only thing we can do in order to prevent watchers on the same node to remove children
			// while we are adding them is to ensure that the childrenModifyLock is properly set
		} finally {
			childrenModifyLock.unlock();
		}
	}

	private void saveProperties() throws Exception {
		// don't do anything if removed
		if (removed) {
			return;
		}

		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("Saving properties of node {} (version {})", this, propertiesVersion);
		}

		// prevent concurrent property modification (eg. remote _and_ local flush)
		propertiesModificationLock.lock();
		try {
			if (removed) {
				return;
			}

			// collect properties to save
			final Properties toSave = new SortedProperties();
			for (final String key : properties.stringPropertyNames()) {
				final String value = properties.getProperty(key);
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
			propertiesVersion = service.writeProperties(zkPath, out.toByteArray(), propertiesVersion);
			propertiesLoadTimestamp = System.currentTimeMillis();

			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Saved properties of node {} (now at version {})", this, propertiesVersion);
			}
		} finally {
			propertiesModificationLock.unlock();
		}
	}

	/**
	 * Indicates if the node should been loaded.
	 * <p>
	 * A node that has not been loaded previously should be loaded. The "loaded"
	 * state is determined based on remote versions. If the node has no remote
	 * versions we must consider the node NOT loaded. This may not be entirely
	 * true if it doesn't exists remotely.
	 * </p>
	 * 
	 * @return <code>true</code> if the node should be loaded,
	 *         <code>false</code> otherwise
	 */
	private boolean shouldLoad() {
		// in order to prevent from missing watcher events we only allow a certain age
		// after that we also want to re-load a node in order to not miss any remote changes
		return (propertiesVersion == -1) || (childrenVersion == -1) || (propertiesLoadTimestamp < (System.currentTimeMillis() - RELOAD_AGE)) || (childrenLoadTimestamp < (System.currentTimeMillis() - RELOAD_AGE));
	}

	@Override
	public void sync() throws BackingStoreException {
		// sync the ZooKeeper client with the leader
		// (note, we do this here and not in #syncTree because ZooKeeper implements this recursively already)
		try {
			checkRemoved();
			if (CloudDebug.zooKeeperPreferences) {
				LOG.debug("Syncing ZooKeeper client with leader for node {}", new Object[] { this });
			}
			service.sync(zkPath);
		} catch (final Exception e) {
			createBackingStoreException("synchronizing ZooKeeper client", e);
		}

		// sync tree
		syncTree();

		// flush
		flush();
	}

	/**
	 * This is the actual sync implementation.
	 * <p>
	 * It's called by {@link #sync()} in order to sync the whole tree first
	 * before flushing any content.
	 * </p>
	 * 
	 * @throws BackingStoreException
	 */
	void syncTree() throws BackingStoreException {
		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("Syncing node {} (version {}, cversion {})", new Object[] { this, propertiesVersion, childrenVersion });
		}

		// check connection
		ensureLoaded();

		// prevent concurrent children modification (eg. remote _and_ local flush)
		childrenModifyLock.lock();
		try {
			checkRemoved();

			// prevent concurrent property modification (eg. remote _and_ local flush)
			propertiesModificationLock.lock();
			try {
				checkRemoved();

				// check that the node exists in ZooKeeper, i.e. was flushed at least once
				if (propertiesVersion == -1) {
					final Stat versionInfo = service.getVersionInfo(zkPath);
					if (null == versionInfo) {
						// this is a new node, it was never flushed so there is no way we can refresh properties and children
						if (CloudDebug.zooKeeperPreferences) {
							LOG.debug("Sync aborted for node {} (version {}, cversion {}): it was never flushed and does not exists in ZooKeeper.", new Object[] { this, propertiesVersion, childrenVersion });
						}
						return;
					}
				}

				// refresh properties & children (override any local changes)
				service.refreshProperties(zkPath, true);
				service.refreshChildren(zkPath, true);
			} catch (final Exception e) {
				// throw
				throw createBackingStoreException("refreshing node data", e);
			} finally {
				propertiesModificationLock.unlock();
			}

			// sync children
			for (final ZooKeeperBasedPreferences child : children.values()) {
				child.syncTree();
			}
		} finally {
			childrenModifyLock.unlock();
		}

		if (CloudDebug.zooKeeperPreferences) {
			LOG.debug("Synced node {} (version {}, cversion {})", new Object[] { this, propertiesVersion, childrenVersion });
		}
	}

	/**
	 * Returns the children.
	 * 
	 * @return the children
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected ConcurrentMap<String, ZooKeeperBasedPreferences> testableGetChildren() {
		return children;
	}

	/**
	 * Returns the childrenVersion.
	 * 
	 * @return the childrenVersion
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected int testableGetChildrenVersion() {
		return childrenVersion;
	}

	/**
	 * Returns the properties.
	 * 
	 * @return the properties
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected Properties testableGetProperties() {
		return properties;
	}

	/**
	 * Returns the propertiesVersion.
	 * 
	 * @return the propertiesVersion
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected int testableGetPropertiesVersion() {
		return propertiesVersion;
	}

	/**
	 * Returns the ZooKeeper path of this preference.
	 * 
	 * @return the ZooKeeper path
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected String testableGetZooKeeperPath() {
		return zkPath;
	}

	/**
	 * Indicates if the node has been removed
	 * 
	 * @return <code>true</code> if removed, <code>false</code> otherwise
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected boolean testableRemoved() {
		return removed;
	}

	@Override
	public String toString() {
		final StringBuilder toString = new StringBuilder();
		toString.append(absolutePath());
		if (removed) {
			toString.append(" REMOVED");
		}
		if (!service.isActive(this)) {
			toString.append(" INACTIVE");
		}
		if (!service.isConnected()) {
			toString.append(" DISCONNECTED");
		}
		toString.append(" [").append(propertiesVersion).append('/').append(childrenVersion).append(']');
		return toString.toString();
	}
}
