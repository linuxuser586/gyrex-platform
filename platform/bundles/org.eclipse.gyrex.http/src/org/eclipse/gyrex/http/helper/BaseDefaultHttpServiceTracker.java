/*******************************************************************************
 * Copyright (c) 2008, 2009 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.helper;


import org.eclipse.gyrex.http.internal.HttpActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Base HTTP service tracker for Gyrex default HTTP service.
 */
public abstract class BaseDefaultHttpServiceTracker extends ServiceTracker {

	/**
	 * filter string for the default http service (value
	 * <code>(&(objectClass=org.osgi.service.http.HttpService)(other.info=org.eclipse.gyrex.http.default))</code>)
	 */
	private static final String FILTER_DEFAULT_HTTP_SERVICE = "(&(objectClass=" + HttpService.class.getName() + ")(other.info=" + HttpActivator.TYPE_WEB + "))";

	private static Filter createFilter(final BundleContext context) {
		try {
			return context.createFilter(FILTER_DEFAULT_HTTP_SERVICE);
		} catch (final InvalidSyntaxException e) {
			// this should never happen because we tested the filter
			throw new IllegalStateException("error in implementation: " + e);
		}
	}

	/**
	 * Creates and returns new default HTTP service tracker instance.
	 * 
	 * @param context
	 *            the bundle context (may not be <code>null</code>)
	 */
	public BaseDefaultHttpServiceTracker(final BundleContext context) {
		super(context, createFilter(context), null);
	}

}
