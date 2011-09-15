/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context.internal.services;

import org.eclipse.gyrex.common.services.BundleServiceHelper;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.context.internal.GyrexContextImpl;
import org.eclipse.gyrex.context.internal.configuration.ContextConfiguration;
import org.eclipse.gyrex.context.services.IRuntimeContextServiceLocator;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;

/**
 *
 */
public class GyrexContextServiceLocatorImpl implements IRuntimeContextServiceLocator {

	private final BundleServiceHelper serviceHelper;
	private final GyrexContextImpl context;

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 * @param bundleContext
	 */
	public GyrexContextServiceLocatorImpl(final GyrexContextImpl context, final BundleContext bundleContext) {
		this.context = context;
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
		// find the filter
		final Filter filter = ContextConfiguration.findFilter(context.getContextPath(), serviceInterface.getName());
		if (null != filter) {
			return serviceHelper.trackService(serviceInterface, filter);
		}

		// don't use filter
		// TODO: need to understand and implement behavior when filter is updated (current assumption is that a flush is necessary)
		return serviceHelper.trackService(serviceInterface);
	}

}
