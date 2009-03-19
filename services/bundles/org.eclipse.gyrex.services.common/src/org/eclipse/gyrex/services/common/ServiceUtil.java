/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.services.common;

import java.text.MessageFormat;


import org.eclipse.gyrex.common.context.IContext;
import org.eclipse.gyrex.services.common.internal.ServicesActivator;

/**
 * This class must be used to obtain any service implementation.
 * <p>
 * This class is not intended to be subclassed or instantiated. It provides
 * static methods to streamline the service access.
 * </p>
 */
public final class ServiceUtil {

	@SuppressWarnings("unchecked")
	private static <M> M castObject(final Object object, final Class<M> type) throws ClassCastException {
		// we use a direct cast for performance reason
		//return serviceType.cast(service);
		return (M) object;
	}

	/**
	 * Returns the contributed service implementation of the specified type for
	 * a given context.
	 * <p>
	 * This implementation asks the specified context for an adapter of the
	 * specified service type. This will ensure that always the correct service
	 * for the specified context will be used. If no service is available, an
	 * attempt will be made to load contributed services from bundles which
	 * haven't been started yet.
	 * </p>
	 * <p>
	 * An {@link IllegalStateException} will be thrown if no service
	 * implementation is available. The reason is simplicity and convenience.
	 * Any callers can expect that they get the correct service at any time. If
	 * they don't get the service they want, it basically means that the system
	 * is in an unusable state anyway (because of upgrades or missing
	 * dependencies) and the current operation should be aborted an re-tried
	 * later.
	 * </p>
	 * 
	 * @param serviceType
	 *            the service type (may not be <code>null</code>)
	 * @param context
	 *            the context for service lookup (may not be <code>null</code>)
	 * @return the service implementation
	 * @throws IllegalStateException
	 *             if no suitable service implementation is currently available
	 */
	public static <M extends IService> M getService(final Class<M> serviceType, final IContext context) throws IllegalStateException {
		if (null == serviceType) {
			throw new IllegalArgumentException("service type must not be null");
		}
		if (null == context) {
			throw new IllegalArgumentException("context must not be null");
		}

		// we simply ask the context for an adapter
		Object service = context.getAdapter(serviceType);

		// check if we can load an adapter if necessary
		if (null == service) {
			service = ServicesActivator.getAdapterManager().loadAdapter(context, serviceType.getName());
		}

		// at this point check if a service was found
		if (null == service) {
			throw new IllegalStateException(MessageFormat.format("No service implementation available for type ''{0}'' in context ''{1}''", serviceType.getName(), context.getContextPath()));
		}

		try {
			return castObject(service, serviceType);
		} catch (final ClassCastException e) {
			// we catch ClassCastException to throw a better error message
			throw new IllegalStateException(MessageFormat.format("The service implementation available for type ''{0}'' in context ''{1}'' is invalid", serviceType.getName(), context.getContextPath()), e);
		}
	}

	/**
	 * Hidden constructor.
	 */
	private ServiceUtil() {
		//empty
	}
}
