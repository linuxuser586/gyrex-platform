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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.internal.configuration.ContextConfiguration;
import org.eclipse.gyrex.context.internal.provider.ProviderRegistration;
import org.eclipse.gyrex.context.internal.provider.ProviderRegistration.ProviderRegistrationReference;
import org.eclipse.gyrex.context.internal.provider.TypeRegistration;
import org.eclipse.gyrex.context.internal.provider.TypeRegistration.TypeRegistrationReference;

import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.text.StrBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Object which capsulates computation to cache value lookups.
 * <p>
 * Every context object is wired to a provider. This implementation is
 * responsible for selecting the correct provider (based on {@link Filter OSGi
 * filters}). It also ensures that the contract of {@link IContextFunction} is
 * valid. Thus, it computes a value only once and never re-computes it.
 * </p>
 */
final class GyrexContextObject implements IContextDisposalListener, ProviderRegistrationReference, TypeRegistrationReference {

	private static final Logger LOG = LoggerFactory.getLogger(GyrexContextObject.class);

	private final Lock objectCreationLock = new ReentrantLock();
	private final Class<?> type;

	private GyrexContextImpl context;

	private volatile boolean isComputed;
	private volatile Object computedObject;
	private volatile ProviderRegistration computedObjectProvider;

	public GyrexContextObject(final GyrexContextImpl runtimeContext, final Class<?> type) {
		context = runtimeContext;
		this.type = type;
	}

	/**
	 * Releases the computed object.
	 */
	private void clearComputedObject() {
		if (!isComputed)
			return;

		Object object;
		ProviderRegistration provider;
		objectCreationLock.tryLock();
		try {
			if (!isComputed)
				return;

			// get object and its provider
			isComputed = false;
			object = computedObject;
			provider = computedObjectProvider;

			// reset
			computedObject = null;
			computedObjectProvider = null;
		} finally {
			objectCreationLock.unlock();
		}

		if (ContextDebug.objectLifecycle) {
			LOG.debug("Flushing computed object {}", this);
		}

		// inform provider of object disposal (outside the lock)
		try {
			if (null != provider) {
				provider.getProvider().ungetObject(object, context.getHandle());
			}
		} finally {
			// remove from provider
			if (null != provider) {
				provider.removeReference(this);
			}

			// remove from context disposables
			if (null != context) {
				context.removeDisposable(this);
			}
		}
	}

	/**
	 * Computes the actual object to use.
	 * 
	 * @return the actual object
	 */
	public Object compute() {
		// use computed object if available
		if (isComputed) {
			if (ContextDebug.objectLifecycle) {
				LOG.debug("Returning previously computed object {} from {}", computedObject, this);
			}
			return computedObject;
		}

		// lock to ensure that at most one object per context is created
		try {
			if (!objectCreationLock.tryLock(2, TimeUnit.SECONDS))
				throw new IllegalStateException(String.format("Timout waiting for computation lock (%s) in context %s for type %s", objectCreationLock, context.getContextPath(), type.getName()));
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(String.format("Interrupted while waiting for computation lock (%s) in context %s for type %s", objectCreationLock, context.getContextPath(), type.getName()));
		}
		final List<Throwable> errors = new ArrayList<Throwable>(3);
		try {
			// ensure that no other thread created an object instance yet
			if (isComputed) {
				if (ContextDebug.objectLifecycle) {
					LOG.debug("Returning previously computed object {} from {}", computedObject, this);
				}
				return computedObject;
			}

			// check if disposed
			if (context == null)
				throw new IllegalStateException(String.format("Object for type '%s' in context '%s' already disposed!", type.getName(), context.getContextPath()));

			if (ContextDebug.objectLifecycle) {
				LOG.debug("Computing object for {}", this);
			}

			Object object = null;
			ProviderRegistration provider = null;

			// check that there is a type registration
			final String typeName = type.getName();
			final TypeRegistration typeRegistration = context.getContextRegistry().getObjectProviderRegistry().getType(typeName);
			if (null == typeRegistration)
				return null;

			// find the filter
			final String filterString = ContextConfiguration.findFilter(context.getContextPath(), typeName);

			// create filter object
			Filter filter = null;
			if (null != filterString) {
				try {
					filter = FrameworkUtil.createFilter(filterString);
				} catch (final InvalidSyntaxException e) {
					throw new IllegalStateException(String.format("Unable to compute object for type '%s' due to invalid filter '%s' configured in context '%s'. %s", type.getName(), filterString, context.getContextPath(), e.getMessage()), e);
				}
			}

			// get all matching provider
			final ProviderRegistration[] providers = typeRegistration.getMatchingProviders(filter);
			if (null == providers)
				return null;

			// iterate and use the first compatible one
			for (int i = 0; (i < providers.length) && (null == object); i++) {
				provider = providers[i];
				try {
					object = provider.getProvider().getObject(type, context.getHandle());
				} catch (final AssertionError | LinkageError | Exception e) {
					LOG.warn("Error during object computation in context {} with in provider {}: {}", new Object[] { context.getContextPath(), provider, ExceptionUtils.getRootCauseMessage(e), e });
					errors.add(e);
					object = null;
				}
			}

			// give up if no object found
			if (null == object) {
				if (ContextDebug.objectLifecycle) {
					LOG.debug("No object computed for {}, assuming NULL", this);
				}
				return null;
			}

			// we found an object so we ignore any error in failing providers
			errors.clear();

			// remember the object and its provider
			computedObject = object;
			computedObjectProvider = provider;

			// hook with the context for disposal
			context.addDisposable(this);

			// hook with the type registration to get informed of updates
			typeRegistration.addReference(this);

			// hook with the provider registration to get informed of updates
			provider.addReference(this);

			// return the object
			return object;
		} finally {
			// mark computed when no errors occurred
			isComputed = errors.isEmpty();

			// release lock
			objectCreationLock.unlock();

			// re-throw errors
			if (!errors.isEmpty()) {
				final StringBuilder errorMessage = new StringBuilder();
				errorMessage.append("Could not compute context object ").append(type.getName()).append('.');
				for (final Iterator stream = errors.iterator(); stream.hasNext();) {
					final Throwable t = (Throwable) stream.next();
					errorMessage.append(' ').append(ExceptionUtils.getRootCauseMessage(t));
				}
				throw new IllegalStateException(errorMessage.toString());
			}
		}
	}

	@Override
	public void contextDisposed(final IRuntimeContext runtimeContext) {
		if (ContextDebug.objectLifecycle) {
			LOG.debug("Context {} disposed, disposing {}", runtimeContext, this);
		}

		clearComputedObject();

		// unset (also means disposed)
		context = null;
	}

	public void dump(final int ident, final StrBuilder dump) {
		dump.appendPadding(ident, ' ').appendln(computedObject);
	}

	@Override
	public void flushReference(final ProviderRegistration provider) {
		clearComputedObject();
	}

	@Override
	public void flushReference(final TypeRegistration typeRegsitration) {
		clearComputedObject();
	}

	@Override
	public String toString() {
		final GyrexContextImpl context = this.context;
		if (null == context)
			return "GyrexContextObject [DISPOSED]";

		final StringBuilder builder = new StringBuilder();
		builder.append("GyrexContextObject [context=").append(context).append(", type=").append(type).append("]");
		return builder.toString();
	}
}