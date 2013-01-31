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

import org.eclipse.core.runtime.IPath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalContext extends BaseContext implements IModifiableRuntimeContext {

	private static final Logger LOG = LoggerFactory.getLogger(LocalContext.class);

	private final GyrexContextImpl contextImpl;
	private final Map<Class<?>, Object> localObjects = new HashMap<>();

	final AtomicReference<LocalContextInjectorImpl> localInjectorRef = new AtomicReference<>();

	public LocalContext(final IPath contextPath, final ContextRegistryImpl contextRegistry) {
		super(contextPath, contextRegistry);

		// create real local context
		contextImpl = new GyrexContextImpl(contextPath, contextRegistry) {
			@Override
			public IRuntimeContext getHandle() {
				return LocalContext.this;
			}

		};
	}

	@Override
	public IModifiableRuntimeContext createWorkingCopy() throws IllegalStateException {
		throw new IllegalStateException("Creation of nested modifiable contexts is not supported!");
	}

	@Override
	public void dispose() {
		localObjects.clear();
		localInjectorRef.set(null);
		contextImpl.dispose();
	}

	@Override
	public GyrexContextImpl get() {
		// note, we do not use the shared context instance here
		// each local context creates its own object tree; there is no shared
		// state between a local context and the original context other than
		// the configuration data
		return contextImpl;
	}

	@Override
	public <T> T get(final Class<T> type) throws IllegalArgumentException {
		// check local objects first;
		synchronized (localObjects) {
			if (localObjects.containsKey(type)) {
				final T result = type.cast(localObjects.get(type));
				LOG.trace("[{}] GET LOCAL: {} -> {}", new Object[] { this, type, result });
				return result;
			}
		}
		// now check real context
		return get().get(type);
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
		// simply return the global handle
		return getContextRegistry().getHandle(getContextPath());
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
