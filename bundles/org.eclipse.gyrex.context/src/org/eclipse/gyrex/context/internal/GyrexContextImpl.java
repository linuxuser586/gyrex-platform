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
package org.eclipse.gyrex.context.internal;

import java.util.Map.Entry;
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
import org.eclipse.gyrex.context.internal.services.GyrexContextServiceLocatorImpl;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
// note, this class does intentionally not implement IRuntimeContext directly
public class GyrexContextImpl extends PlatformObject implements BundleListener {

	private static final Logger LOG = LoggerFactory.getLogger(GyrexContextImpl.class);

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
	private final Set<IContextDisposalListener> disposables = new CopyOnWriteArraySet<IContextDisposalListener>();
	private final GyrexContextInjectorImpl injector;
	private final GyrexContextPreferencesImpl preferences;
	private final AtomicLong lastAccessTime = new AtomicLong();
	private final ConcurrentMap<Class<?>, GyrexContextObject> computedObjects = new ConcurrentHashMap<Class<?>, GyrexContextObject>();
	private final ConcurrentMap<Bundle, GyrexContextServiceLocatorImpl> serviceLocators = new ConcurrentHashMap<Bundle, GyrexContextServiceLocatorImpl>();

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
		preferences = new GyrexContextPreferencesImpl(contextRegistry.getHandle(contextPath));
	}

	public void addDisposable(final IContextDisposalListener disposable) {
		checkDisposed();
		if (!disposables.contains(disposable)) {
			disposables.add(disposable);
		}
	}

	@Override
	public void bundleChanged(final BundleEvent event) {
		if (event.getType() == BundleEvent.STOPPED) {
			// dispose service locator (if available)
			final GyrexContextServiceLocatorImpl serviceLocator = serviceLocators.remove(event.getBundle());
			if (null != serviceLocator) {
				// dispose
				serviceLocator.dispose();
				// log debug message
				if (ContextDebug.debug) {
					LOG.debug("Bundle {} has been stopped. Its service locator has been removed from {}.", event.getBundle(), this);
				}
			}
		}

	}

	private void checkDisposed() throws IllegalStateException {
		if (disposed.get()) {
			throw new IllegalStateException("context is disposed");
		}
	}

	/**
	 * Disposes the context.
	 */
	public void dispose() {
		// don't do anything if already disposed; if not mark disposed
		if (disposed.getAndSet(true)) {
			return;
		}

		// notify disposables
		try {
			for (final IContextDisposalListener disposable : disposables) {
				disposable.contextDisposed(getHandle());
			}
		} finally {
			disposables.clear();

			// dispose injector
			injector.dispose();

			// dispose service locators
			try {
				for (final GyrexContextServiceLocatorImpl serviceLocator : serviceLocators.values()) {
					serviceLocator.dispose();
				}
			} finally {
				serviceLocators.clear();
			}

			// dispose preferences
			preferences.dispose();
		}
	}

	public void dump(final StrBuilder dump) {
		dump.appendln(contextPath.toString());
		dump.appendPadding(1, ' ').appendln("Objects");
		if (!computedObjects.isEmpty()) {
			for (final Entry<Class<?>, GyrexContextObject> entry : computedObjects.entrySet()) {
				dump.appendPadding(2, ' ').appendln(entry.getKey());
				final GyrexContextObject value = entry.getValue();
				if (null != value) {
					value.dump(3, dump);
				} else {
					dump.appendPadding(3, ' ').appendln("(no value)");
				}
			}
		} else {
			dump.appendPadding(2, ' ').appendln("(none)");
		}
		dump.appendPadding(1, ' ').appendln("Preferences");
		preferences.dump(2, dump);
	}

	/**
	 * @see IRuntimeContext#get(Class)
	 */
	public <T> T get(final Class<T> type) throws IllegalArgumentException {
		checkDisposed();
		trackAccess();

		// get computed object
		GyrexContextObject contextObject = computedObjects.get(type);
		if (null == contextObject) {
			// check if would be possible to compute an object
			if (null == getContextRegistry().getObjectProviderRegistry().getType(type.getName())) {
				return null;
			}

			// compute object
			computedObjects.putIfAbsent(type, new GyrexContextObject(this, type));
			contextObject = computedObjects.get(type);
		}

		return safeCast(contextObject.compute());
	}

	/**
	 * @see IRuntimeContext#getContextPath()
	 */
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

	/**
	 * @see IRuntimeContext#getInjector()
	 */
	public IRuntimeContextInjector getInjector() {
		checkDisposed();
		trackAccess();

		// return injector
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

	/**
	 * @see IRuntimeContext#getPreferences()
	 */
	public GyrexContextPreferencesImpl getPreferences() {
		checkDisposed();
		trackAccess();

		// return preferences
		return preferences;
	}

	/**
	 * @see IRuntimeContext#getServiceLocator(BundleContext)
	 */
	public GyrexContextServiceLocatorImpl getServiceLocator(final BundleContext bundleContext) {
		checkDisposed();
		trackAccess();

		GyrexContextServiceLocatorImpl serviceLocator = serviceLocators.get(bundleContext.getBundle());
		if (null == serviceLocator) {
			serviceLocator = serviceLocators.putIfAbsent(bundleContext.getBundle(), new GyrexContextServiceLocatorImpl(this, bundleContext));
			if (null == serviceLocator) {
				serviceLocator = serviceLocators.get(bundleContext.getBundle());
			}

			// TODO revisit later, there might be a race condition here (bundle being stopped while this method is called)
			if (null == serviceLocator) {
				throw new IllegalStateException("unable to create service locator; is the bundle stopped concurrently?");
			}
		}
		return serviceLocator;
	}

	/**
	 * Indicates if the context has been disposed.
	 * 
	 * @return <code>true</code> if the context has been disposed
	 */
	public boolean isDisposed() {
		return disposed.get();
	}

	void removeDisposable(final IContextDisposalListener disposable) {
		// ignore after disposal
		if (!disposed.get()) {
			disposables.remove(disposable);
		}
	}

	@Override
	public String toString() {
		if (isDisposed()) {
			return String.format("Gyrex Context [DISPOSED (%s)]", contextPath);
		}

		// look for a context name
		String name;
		try {
			name = preferences.get(ContextActivator.SYMBOLIC_NAME, "contextName", null);
		} catch (final Exception e) {
			name = ExceptionUtils.getRootCauseMessage(e);
		}

		// TODO: should not leak context path here, we may need a story for this
		return String.format("Gyrex Context [%s (%s)]", name, contextPath);
	}

	private void trackAccess() {
		lastAccessTime.set(System.currentTimeMillis());
	}

}
