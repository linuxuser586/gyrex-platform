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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.e4.core.services.IDisposable;
import org.eclipse.e4.core.services.context.IContextFunction;
import org.eclipse.e4.core.services.context.IEclipseContext;
import org.eclipse.e4.core.services.context.spi.ContextFunction;
import org.eclipse.gyrex.context.internal.provider.ProviderRegistration;
import org.eclipse.gyrex.context.internal.provider.TypeRegistration;
import org.eclipse.gyrex.context.internal.provider.ProviderRegistration.ProviderRegistrationReference;
import org.eclipse.gyrex.context.internal.provider.TypeRegistration.TypeRegistrationReference;
import org.osgi.framework.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the {@link IContextFunction} which gets placed into the e4 context.
 * <p>
 * Every context object is wired to a provider. This implementation is
 * responsible for selecting the correct provider (based on {@link Filter OSGi
 * filters}). It also ensures that the contract of {@link IContextFunction} is
 * valid. Thus, it computes a value only once and never re-computes it.
 * </p>
 */
final class GyrexContextObject extends ContextFunction implements IDisposable, ProviderRegistrationReference, TypeRegistrationReference {

	private static final Logger LOG = LoggerFactory.getLogger(GyrexContextObject.class);

	private final Lock objectCreationLock = new ReentrantLock();
	private final TypeRegistration typeRegistration;
	private final Filter filter;

	private GyrexContextImpl runtimeContext;

	private volatile boolean isComputed;
	private volatile Object computedObject;
	private volatile ProviderRegistration computedObjectProvider;
	private volatile GyrexContextImpl computedObjectContext;
	private volatile IEclipseContext computedObjectEclipseContext;

	public GyrexContextObject(final GyrexContextImpl runtimeContext, final TypeRegistration typeRegistration, final Filter filter) {
		this.runtimeContext = runtimeContext;
		this.typeRegistration = typeRegistration;
		this.filter = filter;
	}

	/**
	 * Releases the computed object.
	 */
	private void clearComputedObject() {
		if (!isComputed) {
			return;
		}

		Object object;
		ProviderRegistration provider;
		GyrexContextImpl context;
		IEclipseContext eclipseContext;
		objectCreationLock.tryLock();
		try {
			if (!isComputed) {
				return;
			}

			// get object and its provider
			isComputed = false;
			object = computedObject;
			provider = computedObjectProvider;
			context = computedObjectContext;
			eclipseContext = computedObjectEclipseContext;

			// reset
			computedObject = null;
			computedObjectProvider = null;
			computedObjectContext = null;
			computedObjectEclipseContext = null;
		} finally {
			objectCreationLock.unlock();
		}

		if (ContextDebug.objectLifecycle) {
			LOG.debug("Flushing computed object {}", this);
		}

		// inform provider of object disposal (outside the lock)
		try {
			if (null != provider) {
				provider.getProvider().ungetObject(object, context);
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

			// remove from the underlying Eclipse context to trigger lookup through the strategy again
			if (null != eclipseContext) {
				eclipseContext.remove(getTypeName());
			}
		}
	}

	@Override
	public Object compute(final IEclipseContext context, final Object[] arguments) {
		// check arguments
		if ((null == arguments) || (arguments.length != 1) || !(arguments[0] instanceof Class)) {
			return null;
		}
		final Class<?> type = (Class) arguments[0];
		if (!type.getName().equals(getTypeName())) {
			return null;
		}

		// get the runtime context
		final GyrexContextImpl runtimeContext = (GyrexContextImpl) context.get(GyrexContextImpl.ECLIPSE_CONTEXT_KEY);
		if (null == runtimeContext) {
			return null;
		}

		// use computed object if available
		if (isComputed) {
			if (ContextDebug.objectLifecycle) {
				LOG.debug("Returning previously computed object {} from {}", computedObject, this);
			}
			return computedObject;
		}

		// lock to ensure that at most one object per context is created
		objectCreationLock.tryLock();
		try {
			// ensure that no other thread created an object instance yet
			if (isComputed) {
				if (ContextDebug.objectLifecycle) {
					LOG.debug("Returning previously computed object {} from {}", computedObject, this);
				}
				return computedObject;
			}

			if (ContextDebug.objectLifecycle) {
				LOG.debug("Computing object for {}", this);
			}

			// get matching provider
			final ProviderRegistration provider = typeRegistration.getProvider(type, filter);

			// TODO: verify that the provider is allowed for automatic availability in this context
			if ((null == filter) && (null != provider)) {
				// ....
			}

			/*
			 * implementation note: we should revisit this;
			 * It might be better to move more of this into the ProviderRegistration.
			 * But at this point it works for now.
			 */

			// get the object
			final Object object = null != provider ? provider.getProvider().getObject(type, runtimeContext.getHandle()) : null;

			// remember the object and its provider
			isComputed = true;
			computedObject = object;
			computedObjectProvider = provider;
			computedObjectContext = runtimeContext;
			computedObjectEclipseContext = context;

			// hook with the context for disposal
			runtimeContext.addDisposable(this);

			// hook with the type registration to get informed of updates
			typeRegistration.addReference(this);

			// hook with the provider registration to get informed of updates
			if (null != provider) {
				provider.addReference(this);
			}

			// for performance reasons set the object directly in the context
			// (note, we even set it to null to explicitly indicate that non is available)
			context.set(getTypeName(), object);

			// return the object
			return object;
		} finally {
			objectCreationLock.unlock();
		}
	}

	@Override
	public void dispose() {
		if (ContextDebug.objectLifecycle) {
			LOG.debug("Disposing {}", this);
		}

		clearComputedObject();

		// dispose
		runtimeContext = null;
	}

	@Override
	public void flushReference(final ProviderRegistration provider) {
		clearComputedObject();
	}

	@Override
	public void flushReference(final TypeRegistration typeRegsitration) {
		clearComputedObject();
	}

	private String getTypeName() {
		return typeRegistration.getTypeName();
	}

	@Override
	public String toString() {
		final GyrexContextImpl context = runtimeContext;
		if (null == context) {
			return "GyrexContextObject [disposed]";
		}

		final StringBuilder builder = new StringBuilder();
		builder.append("GyrexContextObject [runtimeContext=").append(context).append(", typeName=").append(getTypeName()).append(", filter=").append(filter).append("]");
		return builder.toString();
	}
}