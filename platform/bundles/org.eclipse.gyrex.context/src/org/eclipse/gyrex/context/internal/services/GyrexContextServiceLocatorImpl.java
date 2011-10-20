/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context.internal.services;

import org.eclipse.gyrex.common.services.BundleServiceHelper;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.context.internal.GyrexContextImpl;
import org.eclipse.gyrex.context.internal.configuration.ContextConfiguration;
import org.eclipse.gyrex.context.services.IRuntimeContextServiceLocator;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

/**
 *
 */
public class GyrexContextServiceLocatorImpl implements IRuntimeContextServiceLocator {

	private final BundleServiceHelper serviceHelper;
	private final GyrexContextImpl context;
	private final BundleContext bundleContext;

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 * @param bundleContext
	 */
	public GyrexContextServiceLocatorImpl(final GyrexContextImpl context, final BundleContext bundleContext) {
		this.context = context;
		this.bundleContext = bundleContext;
		serviceHelper = new BundleServiceHelper(bundleContext);
	}

	/**
	 * Disposes the service locator.
	 */
	public void dispose() {
		serviceHelper.dispose();
	}

	/**
	 * @see org.eclipse.gyrex.common.services.BundleServiceHelper#trackService(java.lang.Class)
	 */
	public <T> IServiceProxy<T> trackService(final Class<T> serviceInterface) {
		try {
			return trackService(serviceInterface, null);
		} catch (final InvalidSyntaxException e) {
			// impossible because we specified "null", but you'll never know
			throw new IllegalStateException("invalid filter syntax", e);
		}
	}

	/**
	 * @see org.eclipse.gyrex.common.services.BundleServiceHelper#trackService(java.lang.Class)
	 * @throws InvalidSyntaxException
	 */
	public <T> IServiceProxy<T> trackService(final Class<T> serviceInterface, final String additionalFilter) throws InvalidSyntaxException {
		// parse additional filter
		Filter filter = null;
		if (null != additionalFilter) {
			filter = bundleContext.createFilter(additionalFilter);
		}

		// combine with context filter
		final Filter contextFilter = ContextConfiguration.findFilter(context.getContextPath(), serviceInterface.getName());
		if (null != contextFilter) {
			if (null != filter) {
				// combine with addition filter
				filter = bundleContext.createFilter(String.format("(&(%s)(%s))", contextFilter.toString(), filter.toString()));
			} else {
				// use as is
				filter = contextFilter;
			}
		} else if (null != filter) {
			// combine with object class condition
			filter = bundleContext.createFilter(String.format("(&(objectClass=%s)(%s))", serviceInterface.getName(), filter.toString()));
		}

		// track service
		if (null != filter) {
			return serviceHelper.trackService(serviceInterface, filter);
		} else {
			// don't use filter
			// TODO: need to understand and implement behavior when filter is updated (current assumption is that a flush is necessary)
			return serviceHelper.trackService(serviceInterface);
		}
	}
}
