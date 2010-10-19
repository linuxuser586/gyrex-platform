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
package org.eclipse.gyrex.context.internal;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.di.IRuntimeContextInjector;
import org.eclipse.gyrex.context.internal.di.GyrexContextInjectorImpl;
import org.eclipse.gyrex.context.internal.preferences.GyrexContextPreferencesImpl;
import org.eclipse.gyrex.context.internal.registry.ContextRegistryImpl;
import org.eclipse.gyrex.context.preferences.IRuntimeContextPreferences;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.e4.core.di.IDisposable;

/**
 * Internal context implementation.
 * <p>
 * Note, a context must be dynamic, there are potentially long running
 * references to a context. The #get calls need to be dynamic.
 * </p>
 * <p>
 * The context implementation relies on preferences to store context
 * configuration and on the e4 {@link IEclipseContext} to implement the context
 * behavior na dot allow injection capabilities. There are various elements
 * which glue the pieces together.
 * </p>
 */
@SuppressWarnings("restriction")
public class GyrexContextImpl extends PlatformObject implements IRuntimeContext, IDisposable {

	/** key for looking up a GyrexContextImpl from an IEclipseContext */
	static final String ECLIPSE_CONTEXT_KEY = GyrexContextImpl.class.getName();

	@SuppressWarnings("unchecked")
	private static <T> T safeCast(final Object object) {
		try {
			return (T) object;
		} catch (final ClassCastException e) {
			// TODO should debug/trace this
			return null;
		}
	}

	private final IPath contextPath;
	private final AtomicBoolean disposed = new AtomicBoolean();
	private final ContextRegistryImpl contextRegistry;
	private final Set<IDisposable> disposables = new CopyOnWriteArraySet<IDisposable>();
	private final GyrexContextInjectorImpl injector;
	private final GyrexContextPreferencesImpl preferences;
	private final AtomicLong lastAccessTime = new AtomicLong();
	private final ConcurrentMap<Class<?>, GyrexContextObject> computedObjects = new ConcurrentHashMap<Class<?>, GyrexContextObject>();

	/**
	 * Creates a new instance.
	 * 
	 * @param contextPath
	 */
	public GyrexContextImpl(final IPath contextPath, final ContextRegistryImpl contextRegistry) {
		if (null == contextPath) {
			throw new IllegalArgumentException("context path may not be null");
		}
		if (null == contextRegistry) {
			throw new IllegalArgumentException("context registry may not be null");
		}
		this.contextPath = contextPath;
		this.contextRegistry = contextRegistry;
		injector = new GyrexContextInjectorImpl(this);
		preferences = new GyrexContextPreferencesImpl(this);
	}

	public void addDisposable(final IDisposable disposable) {
		checkDisposed();
		if (!disposables.contains(disposable)) {
			disposables.add(disposable);
		}
	}

	private void checkDisposed() throws IllegalStateException {
		if (disposed.get()) {
			throw new IllegalStateException("context is disposed");
		}
	}

	@Override
	public void dispose() {
		// don't do anything if already disposed; if not mark disposed
		if (disposed.getAndSet(true)) {
			return;
		}

		// dispose injector
		if (injector instanceof IDisposable) {
			((IDisposable) injector).dispose();
		}

		// dispose preferences
		preferences.dispose();

		// dispose disposables
		for (final IDisposable disposable : disposables) {
			disposable.dispose();
		}
		disposables.clear();
	}

	@Override
	public <T> T get(final Class<T> type) throws IllegalArgumentException {
		checkDisposed();
		trackAccess();

		// get computed object
		GyrexContextObject contextObject = computedObjects.get(type);
		if (null == contextObject) {
			computedObjects.putIfAbsent(type, new GyrexContextObject(this, type));
			contextObject = computedObjects.get(type);
		}

		return safeCast(contextObject.compute());
	}

	@Override
	public IPath getContextPath() {
		trackAccess();
		return contextPath;
	}

	/**
	 * Returns the contextRegistry.
	 * 
	 * @return the contextRegistry
	 */
	ContextRegistryImpl getContextRegistry() {
		checkDisposed();
		return contextRegistry;
	}

	/**
	 * Returns a handle to this context which should be passed to external API
	 * clients.
	 * 
	 * @return the {@link GyrexContextHandle}
	 */
	public GyrexContextHandle getHandle() {
		return contextRegistry.getHandle(contextPath);
	}

	@Override
	public IRuntimeContextInjector getInjector() {
		checkDisposed();

		return injector;
	}

	/**
	 * Returns the time stamp the last time this context was accessed.
	 * 
	 * @return the time stamp the last time this context was accessed
	 */
	long getLastAccessTime() {
		return lastAccessTime.get();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.context.IRuntimeContext#getPreferences()
	 */
	@Override
	public IRuntimeContextPreferences getPreferences() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Indicates if the context has been disposed.
	 * 
	 * @return <code>true</code> if the context has been disposed
	 */
	public boolean isDisposed() {
		return disposed.get();
	}

	void removeDisposable(final IDisposable disposable) {
		// ignore after disposal
		if (!disposed.get()) {
			disposables.remove(disposable);
		}
	}

	@Override
	public String toString() {
		// TODO: should not leak context path here, we may need a story for this
		return "Gyrex Context [" + contextPath.toString() + "]";
	}

	private void trackAccess() {
		lastAccessTime.set(System.currentTimeMillis());
	}

}
