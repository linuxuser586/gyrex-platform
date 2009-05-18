/**
 * Copyright (c) 2009 AGETO Service GmbH and others.
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
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.e4.core.services.IDisposable;
import org.eclipse.e4.core.services.context.EclipseContextFactory;
import org.eclipse.e4.core.services.context.IEclipseContext;
import org.eclipse.e4.core.services.context.spi.IContextConstants;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.internal.registry.ContextRegistryImpl;

/**
 * Internal context implementation.
 * <p>
 * Note, a context must be dynamic, there are potentially long running
 * references to a context. The #get calls need to be dynamic.
 * </p>
 */
public class GyrexContextImpl extends PlatformObject implements IRuntimeContext, IDisposable {

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
	private final Lock initializationLock = new ReentrantLock();

	private volatile IEclipseContext eclipseContext;

	private final AtomicLong lastAccessTime = new AtomicLong();

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
	}

	void addDisposable(final IDisposable disposable) {
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

		// dispose underlying context
		final IEclipseContext context = eclipseContext;
		if (null != context) {
			eclipseContext = null;

			// remove reference
			context.remove(ECLIPSE_CONTEXT_KEY);

			// dispose context
			if (context instanceof IDisposable) {
				((IDisposable) context).dispose();
			}
		}

		// dispose disposables
		for (final IDisposable disposable : disposables) {
			disposable.dispose();
		}
		disposables.clear();

	}

	@Override
	public <T> T get(final Class<T> type) throws IllegalArgumentException {
		trackAccess();

		// lookup from Eclipse context (always provide the context as argument)
		final Object value = getEclipseContext().get(type.getName(), new Object[] { type });
		if (null == value) {
			return null;
		}

		// return value if possible
		if (type.isAssignableFrom(value.getClass())) {
			return safeCast(value);
		}

		// use first possible value for arrays
		if (value.getClass().isArray()) {
			final Object[] values = (Object[]) value;
			for (final Object value2 : values) {
				if (type.isAssignableFrom(value2.getClass())) {
					return safeCast(value2);
				}
			}
		}

		// give up
		return null;
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

	/*package*/IEclipseContext getEclipseContext() {
		checkDisposed();

		IEclipseContext context = eclipseContext;
		if (null != context) {
			return context;
		}

		// determine parent (outside of initialization lock)
		final GyrexContextHandle parent = contextPath.segmentCount() > 0 ? contextRegistry.get(contextPath.removeLastSegments(1)) : null;
		final IEclipseContext parentEclipseContext = null != parent ? parent.get().getEclipseContext() : null;

		// make sure we stay responsive when acquiring the initialization lock
		try {
			initializationLock.tryLock(2, TimeUnit.SECONDS);
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("concurrent initialization in progress time-out for context " + contextPath);
		}

		// initialize the context if necessary
		try {
			// check if another thread created it
			context = eclipseContext;
			if (null != context) {
				return context;
			}

			// create eclipse context
			eclipseContext = EclipseContextFactory.create(parentEclipseContext, GyrexContextStrategy.SINGLETON);

			// set reference to *this* context
			eclipseContext.set(ECLIPSE_CONTEXT_KEY, this);
			eclipseContext.set(IContextConstants.DEBUG_STRING, "Eclipse Context [" + contextPath.toString() + "]");

			return eclipseContext;
		} finally {
			initializationLock.unlock();
		}
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

	/**
	 * Returns the time stamp the last time this context was accessed.
	 * 
	 * @return the time stamp the last time this context was accessed
	 */
	long getLastAccessTime() {
		return lastAccessTime.get();
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
