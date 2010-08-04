/**
 * Copyright (c) 2009, 2010 AGETO Service GmbH and others.
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
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.gyrex.common.lifecycle.IShutdownParticipant;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.internal.GyrexContextHandle;
import org.eclipse.gyrex.context.internal.GyrexContextImpl;
import org.eclipse.gyrex.context.internal.preferences.GyrexContextPreferencesImpl;
import org.eclipse.gyrex.context.internal.provider.ObjectProviderRegistry;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;

import org.eclipse.core.runtime.IPath;
import org.eclipse.e4.core.di.IDisposable;
import org.eclipse.osgi.util.NLS;

/**
 * The {@link IRuntimeContextRegistry} implementation.
 */
//TODO: this should be a ServiceFactory which knows about the bundle requesting the manager for context access permission checks
@SuppressWarnings("restriction")
public class ContextRegistryImpl implements IRuntimeContextRegistry, IShutdownParticipant {

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

		// verify segments
		for (final String segment : contextPath.segments()) {
			if (forbiddenPathSegments.contains(segment)) {
				throw new IllegalArgumentException(NLS.bind("Segment \"{0}\" not allowed in context path \"{1}\"", segment, contextPath));
			}
		}

		return contextPath.makeAbsolute().addTrailingSeparator();
	}

	private final Map<IPath, GyrexContextImpl> contexts;
	private final ConcurrentMap<IPath, GyrexContextHandle> handles;
	private final AtomicBoolean closed = new AtomicBoolean();
	private final Lock contextRegistryModifyLock = new ReentrantLock();

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

	/**
	 * Flushes a complete context hierarchy.
	 * 
	 * @param context
	 */
	public void flushContextHierarchy(final IRuntimeContext context) {
		checkClosed();

		// get context path
		final IPath contextPath = context.getContextPath();

		// remove all entries within that path
		final List<GyrexContextImpl> removedContexts = new ArrayList<GyrexContextImpl>();
		contextRegistryModifyLock.lock();
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
			contextRegistryModifyLock.unlock();
		}

		// dispose all removed contexts (outside of lock)
		for (final GyrexContextImpl contextImpl : removedContexts) {
			contextImpl.dispose();
		}
	}

	@Override
	public GyrexContextHandle get(final IPath contextPath) throws IllegalArgumentException {
		// we return a handle only so that clients can hold on the context for a longer time but we can dispose the internal context at any time
		return getHandle(contextPath);
	}

	public GyrexContextHandle getHandle(IPath contextPath) {
		checkClosed();
		contextPath = sanitize(contextPath);
		GyrexContextHandle contextHandle = handles.get(contextPath);
		if (null == contextHandle) {
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
		GyrexContextImpl context = contexts.get(contextPath);
		if (null != context) {
			return context;
		}

		// create & store new context if necessary
		contextRegistryModifyLock.lock();
		try {
			checkClosed();

			context = contexts.get(contextPath);
			if (null != context) {
				return context;
			}

			context = new GyrexContextImpl(contextPath, this);
			contexts.put(contextPath, context);

		} finally {
			contextRegistryModifyLock.unlock();
		}

		return context;
	}

	@Override
	public void shutdown() throws Exception {
		// set closed
		closed.set(true);

		// dispose active contexts
		IRuntimeContext[] activeContexts;
		contextRegistryModifyLock.lock();
		try {
			activeContexts = contexts.values().toArray(new IRuntimeContext[contexts.size()]);
			contexts.clear();
		} finally {
			contextRegistryModifyLock.unlock();
		}

		// dispose all the active contexts
		for (final IRuntimeContext context : activeContexts) {
			if (context instanceof IDisposable) {
				((IDisposable) context).dispose();
			}
		}

		// clear handles
		handles.clear();
	}

}
