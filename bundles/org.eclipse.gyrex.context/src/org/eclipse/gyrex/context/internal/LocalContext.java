/*******************************************************************************
 * Copyright (c) 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.context.IModifiableRuntimeContext;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.di.IRuntimeContextInjector;
import org.eclipse.gyrex.context.internal.di.LocalContextInjectorImpl;
import org.eclipse.gyrex.context.internal.registry.ContextRegistryImpl;
import org.eclipse.gyrex.context.preferences.IRuntimeContextPreferences;
import org.eclipse.gyrex.context.services.IRuntimeContextServiceLocator;

import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalContext extends BaseContext implements IModifiableRuntimeContext {

	private static final Logger LOG = LoggerFactory.getLogger(LocalContext.class);

	private final IRuntimeContext originalContext;
	private final Map<Class<?>, Object> localObjects = new HashMap<>();

	final AtomicReference<LocalContextInjectorImpl> localInjectorRef = new AtomicReference<>();

	public LocalContext(final IRuntimeContext originalContext, final ContextRegistryImpl contextRegistry) {
		super(originalContext.getContextPath(), contextRegistry);
		this.originalContext = originalContext;
	}

	@Override
	public IModifiableRuntimeContext createWorkingCopy() {
		return new LocalContext(this, getContextRegistry());
	}

	@Override
	public void dispose() {
		localObjects.clear();
		localInjectorRef.set(null);
	}

	@Override
	public <T> T get(final Class<T> type) throws IllegalArgumentException {
		synchronized (localObjects) {
			if (localObjects.containsKey(type)) {
				final T result = type.cast(localObjects.get(type));
				LOG.trace("[{}] GET LOCAL: {} -> {}", new Object[] { this, type, result });
				return result;
			}
		}
		return originalContext.get(type);
	}

	@Override
	public Object getAdapter(final Class adapter) {
		return getOriginalContext().getAdapter(adapter);
	}

	@Override
	public IRuntimeContextInjector getInjector() {
		LocalContextInjectorImpl injector = localInjectorRef.get();
		while (injector == null) {
			final GyrexContextImpl contextImpl = get();
			final LocalContextInjectorImpl newInjector = new LocalContextInjectorImpl(contextImpl, this);
			if (localInjectorRef.compareAndSet(null, newInjector)) {
				if (ContextDebug.injection) {
					LOG.debug("Created local context injector ({}) for local context ({})", newInjector, LocalContext.this);
				}
				// hook disposal listener to unset local injector when context is destroyed
				contextImpl.addDisposable(new IContextDisposalListener() {
					@Override
					public void contextDisposed(final IRuntimeContext runtimeContext) {
						if (localInjectorRef.compareAndSet(newInjector, null)) {
							if (ContextDebug.injection) {
								LOG.debug("Destroyed local context injector ({}) for local context ({}).", newInjector, LocalContext.this);
							}
						} else {
							if (ContextDebug.injection) {
								LOG.debug("Unabled to destroy local context injector ({}), different one active for local context ({}).", newInjector, LocalContext.this);
							}
						}
					}
				});
				// new injector was set so while loop is satisfied
				return newInjector;
			} else {
				// concurrent access -> next try
				injector = localInjectorRef.get();
			}
		}
		return injector;
	}

	public <T> T getLocal(final Class<T> type) {
		LOG.trace("[{}] GET LOCAL: {}", this, type);
		return type.cast(localObjects.get(type));
	}

	@Override
	public IRuntimeContext getOriginalContext() {
		return originalContext;
	}

	@Override
	public IRuntimeContextPreferences getPreferences() {
		return getOriginalContext().getPreferences();
	}

	@Override
	public IRuntimeContextServiceLocator getServiceLocator(final BundleContext bundleContext) {
		return getOriginalContext().getServiceLocator(bundleContext);
	}

	@Override
	public boolean isLocal(final Class<?> type) throws IllegalArgumentException {
		return localObjects.containsKey(type);
	}

	@Override
	public <T> void setLocal(final Class<T> type, final T value) throws IllegalArgumentException {
		LOG.trace("[{}] SET LOCAL: {}", this, type);
		synchronized (localObjects) {
			localObjects.put(type, value);
		}
	}

	@Override
	public String toString() {
		return String.format("%s [WC]", super.toString());
	}

	@Override
	public void unsetLocal(final Class<?> type) throws IllegalArgumentException {
		LOG.trace("[{}] UNSET LOCAL: {}", this, type);
		synchronized (localObjects) {
			localObjects.remove(type);
		}
	}

}
