/**
 * Copyright (c) 2009, 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.context.internal.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.internal.ContextActivator;
import org.eclipse.gyrex.context.internal.ContextDebug;
import org.eclipse.gyrex.context.internal.GyrexContextHandle;
import org.eclipse.gyrex.context.internal.GyrexContextImpl;
import org.eclipse.gyrex.context.internal.preferences.GyrexContextPreferencesImpl;
import org.eclipse.gyrex.context.internal.provider.ObjectProviderRegistry;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.preferences.CloudScope;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.osgi.util.NLS;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link IRuntimeContextRegistry} implementation.
 */
//TODO: this should be a ServiceFactory which knows about the bundle requesting the manager for context access permission checks
public class ContextRegistryImpl implements IRuntimeContextRegistry {

	private static final Logger LOG = LoggerFactory.getLogger(ContextRegistryImpl.class);

	private static final Set<String> forbiddenPathSegments;
	static {
		final HashSet<String> segments = new HashSet<String>(1);
		segments.add(GyrexContextPreferencesImpl.SETTINGS);
		forbiddenPathSegments = Collections.unmodifiableSet(segments);
	}

	/**
	 * Sanitizes a context path.
	 * <p>
	 * A sanitized context path is absolute and includes a trailing separator.
	 * Note, the path is also verified for invalid segments.
	 * </p>
	 * 
	 * @param contextPath
	 *            the context path to sanitize
	 * @return
	 * @throws IllegalArgumentException
	 *             in one of the following cases
	 *             <ul>
	 *             <li>the specified path is <code>null</code></li>
	 *             <li>the specified path has a device</li>
	 *             <li>the specified path contains a not allowed string/segment</li>
	 *             </ul>
	 */
	public static IPath sanitize(final IPath contextPath) throws IllegalArgumentException {
		if (null == contextPath) {
			throw new IllegalArgumentException("context path must not be null");
		}

		if (null != contextPath.getDevice()) {
			throw new IllegalArgumentException("invalid context path; device id must be null; " + contextPath);
		}

		// check for empty path
		if (contextPath.isEmpty()) {
			return Path.ROOT;
		}

		// verify segments
		for (final String segment : contextPath.segments()) {
			if (forbiddenPathSegments.contains(segment)) {
				throw new IllegalArgumentException(NLS.bind("Segment \"{0}\" not allowed in context path \"{1}\"", segment, contextPath));
			}
		}

		return contextPath.makeAbsolute().addTrailingSeparator();
	}

	private final IPreferenceChangeListener flushListener = new IPreferenceChangeListener() {
		@Override
		public void preferenceChange(final PreferenceChangeEvent event) {
			// check the path to flush
			if (!Path.ROOT.isValidPath(event.getKey())) {
				LOG.warn("Ignored attempt to flush hierarcy for invalid path {}.", event.getKey());
				return;
			}

			// flush
			try {
				doFlushHierarchy(sanitize(new Path(event.getKey())));
			} catch (final Exception e) {
				LOG.error("Error flushing context hierarchy {}: {}", new Object[] { event.getKey(), ExceptionUtils.getRootCauseMessage(e), e });
			}
		}
	};

	private final Map<IPath, GyrexContextImpl> contexts;
	private final ConcurrentMap<IPath, GyrexContextHandle> handles;
	private final AtomicBoolean closed = new AtomicBoolean();
	private final ReadWriteLock contextRegistryLock = new ReentrantReadWriteLock();

	private final ObjectProviderRegistry objectProviderRegistry;

	public ContextRegistryImpl(final ObjectProviderRegistry objectProviderRegistry) {
		contexts = new HashMap<IPath, GyrexContextImpl>();
		handles = new ConcurrentHashMap<IPath, GyrexContextHandle>();
		this.objectProviderRegistry = objectProviderRegistry;
	}

	private void checkClosed() throws IllegalStateException {
		if (closed.get()) {
			throw new IllegalStateException("context registry closed");
		}
	}

	public void close() throws Exception {
		// set closed
		closed.set(true);

		// remove preference listener
		getContextFlushNode().removePreferenceChangeListener(flushListener);

		// dispose active contexts
		GyrexContextImpl[] activeContexts;
		final Lock lock = contextRegistryLock.writeLock();
		lock.lock();
		try {
			activeContexts = contexts.values().toArray(new GyrexContextImpl[contexts.size()]);
			contexts.clear();
		} finally {
			lock.unlock();
		}

		// dispose all the active contexts
		for (final GyrexContextImpl context : activeContexts) {
			context.dispose();
		}

		// clear handles
		handles.clear();
	}

	void doFlushHierarchy(final IPath contextPath) {
		// log debug message
		if (ContextDebug.debug) {
			LOG.debug("Flushing context hierarchy {}...", contextPath);
		}

		// remove all entries within that path
		final List<GyrexContextImpl> removedContexts = new ArrayList<GyrexContextImpl>();
		final Lock lock = contextRegistryLock.writeLock();
		lock.lock();
		try {
			checkClosed();
			final Entry[] entrySet = contexts.entrySet().toArray(new Entry[0]);
			for (final Entry entry : entrySet) {
				final IPath entryPath = (IPath) entry.getKey();
				if (contextPath.isPrefixOf(entryPath)) {
					final GyrexContextImpl contextImpl = contexts.remove(entryPath);
					if (null != contextImpl) {
						removedContexts.add(contextImpl);
					}
				}
			}
		} finally {
			lock.unlock();
		}

		// dispose all removed contexts (outside of lock)
		for (final GyrexContextImpl contextImpl : removedContexts) {
			if (ContextDebug.debug) {
				LOG.debug("Disposing context {}...", contextImpl);
			}
			contextImpl.dispose();
		}

		// log info message
		LOG.info("Flushed context hierarchy {}.", contextPath);
	}

	/**
	 * Flushes a complete context hierarchy.
	 * 
	 * @param contextPath
	 *            the path of the hierarchy to flush
	 * @throws Exception
	 *             if an error occurred flushing the node
	 */
	public void flushContextHierarchy(final IPath contextPath) throws Exception {
		checkClosed();

		// log debug message
		if (ContextDebug.debug) {
			LOG.debug("Sending flush event for context hierarchy {} to all nodes in the cloud...", contextPath);
		}

		// get node
		final IEclipsePreferences node = getContextFlushNode();

		// sync
		node.sync();

		// build the preference key
		final String key = sanitize(contextPath).toString();

		// trigger an update to the preference value
		node.putLong(key, node.getLong(key, 0) + 1);

		// flush
		getContextFlushNode().flush();

		// log info message
		LOG.debug("Flush event for context hierarchy {} sent to all nodes in the cloud ({} flush events so far).", contextPath, node.getLong(key, 0));
	}

	/**
	 * Flushes a complete context hierarchy.
	 * 
	 * @param context
	 * @deprecated just flushes locally
	 */
	@Deprecated
	public void flushContextHierarchy(final IRuntimeContext context) {
		checkClosed();
		doFlushHierarchy(context.getContextPath());
	}

	@Override
	public GyrexContextHandle get(final IPath contextPath) throws IllegalArgumentException {
		// we return a handle only so that clients can hold on the context for a longer time but we can dispose the internal context at any time
		return getHandle(contextPath);
	}

	private Preferences getContextDefinitionStore() {
		return CloudScope.INSTANCE.getNode(ContextActivator.SYMBOLIC_NAME).node("definedContexts");
	}

	private IEclipsePreferences getContextFlushNode() {
		return (IEclipsePreferences) CloudScope.INSTANCE.getNode(ContextActivator.SYMBOLIC_NAME).node("contextFlushes");
	}

	public Collection<ContextDefinition> getDefinedContexts() {
		checkClosed();
		try {
			final Preferences node = getContextDefinitionStore();
			final String[] keys = node.keys();
			final List<ContextDefinition> contexts = new ArrayList<ContextDefinition>(keys.length + 1);
			contexts.add(getRootDefinition());
			for (final String path : keys) {
				final ContextDefinition definition = new ContextDefinition(new Path(path));
				definition.setName(node.get(path, null));
				contexts.add(definition);
			}
			return Collections.unmodifiableList(contexts);
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Error reading context definitions. %s", ExceptionUtils.getRootCauseMessage(e)), e);
		}
	}

	public ContextDefinition getDefinition(IPath contextPath) {
		checkClosed();
		contextPath = sanitize(contextPath);

		// the root definition is always defined
		if (contextPath.isRoot()) {
			return getRootDefinition();
		}

		final Preferences node = getContextDefinitionStore();
		final String name = node.get(contextPath.toString(), null);
		if (name == null) {
			return null;
		}
		final ContextDefinition definition = new ContextDefinition(contextPath);
		definition.setName(name);
		return definition;
	}

	public GyrexContextHandle getHandle(IPath contextPath) {
		checkClosed();
		contextPath = sanitize(contextPath);
		GyrexContextHandle contextHandle = handles.get(contextPath);
		if (null == contextHandle) {
			final ContextDefinition definition = getDefinition(contextPath);
			if (definition == null) {
				return null;
			}
			contextHandle = handles.putIfAbsent(contextPath, new GyrexContextHandle(contextPath, this));
			if (null == contextHandle) {
				contextHandle = handles.get(contextPath);
			}
		}
		return contextHandle;
	}

	/**
	 * Returns the objectProviderRegistry.
	 * 
	 * @return the objectProviderRegistry
	 */
	public ObjectProviderRegistry getObjectProviderRegistry() {
		return objectProviderRegistry;
	}

	/**
	 * Returns the real context implementation
	 * 
	 * @param contextPath
	 * @return
	 * @throws IllegalArgumentException
	 */
	public GyrexContextImpl getRealContext(IPath contextPath) throws IllegalArgumentException {
		checkClosed();
		contextPath = sanitize(contextPath);

		// get existing context
		GyrexContextImpl context = null;
		final Lock readLock = contextRegistryLock.readLock();
		readLock.lock();
		try {
			context = contexts.get(contextPath);
			if (null != context) {
				return context;
			}
		} finally {
			readLock.unlock();
		}

		// hook with preferences
		getContextFlushNode().addPreferenceChangeListener(flushListener);

		// create & store new context if necessary
		final Lock lock = contextRegistryLock.writeLock();
		lock.lock();
		try {
			checkClosed();

			context = contexts.get(contextPath);
			if (null != context) {
				return context;
			}

			final ContextDefinition definition = getDefinition(contextPath);
			if (definition == null) {
				throw new IllegalStateException(String.format("Context '%s' does not exists.", contextPath.toString()));
			}

			context = new GyrexContextImpl(contextPath, this);
			contexts.put(contextPath, context);

		} finally {
			lock.unlock();
		}

		return context;
	}

	private ContextDefinition getRootDefinition() {
		final ContextDefinition rootDefinition = new ContextDefinition(Path.ROOT);
		rootDefinition.setName("ROOT");
		return rootDefinition;
	}

	public boolean hasRealContext(IPath contextPath) throws IllegalArgumentException {
		checkClosed();
		contextPath = sanitize(contextPath);

		final Lock readLock = contextRegistryLock.readLock();
		readLock.lock();
		try {
			return contexts.containsKey(contextPath);
		} finally {
			readLock.unlock();
		}

	}

	public void removeDefinition(final ContextDefinition contextDefinition) {
		checkClosed();
		final IPath path = sanitize(contextDefinition.getPath());

		// prevent root modification
		if (path.isRoot()) {
			throw new IllegalArgumentException("cannot remove root context");
		}

		try {
			final Preferences node = getContextDefinitionStore();
			node.remove(path.toString());
			node.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Error removing context definition %s. %s", path, ExceptionUtils.getRootCauseMessage(e)), e);
		}
	}

	public void saveDefinition(final ContextDefinition contextDefinition) {
		checkClosed();
		final IPath path = sanitize(contextDefinition.getPath());

		// prevent root modification
		if (path.isRoot()) {
			throw new IllegalArgumentException("cannot modify root context");
		}

		try {
			final Preferences node = getContextDefinitionStore();
			final String name = contextDefinition.getName();
			node.put(path.toString(), StringUtils.isNotBlank(name) ? name : "");
			node.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(String.format("Error saving context definition %s. %s", path, ExceptionUtils.getRootCauseMessage(e)), e);
		}
	}

}
