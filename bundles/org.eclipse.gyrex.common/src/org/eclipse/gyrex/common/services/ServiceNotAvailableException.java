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
package org.eclipse.gyrex.common.services;

import java.text.MessageFormat;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Throw to indicate that a requested OSGi service is currently not available.
 */
public class ServiceNotAvailableException extends IllegalStateException {

	/** serialVersionUID */
	private static final long serialVersionUID = 1487250773530150148L;

	private static String getSymbolicName(final BundleContext bundleContext) {
		// get bundle
		Bundle bundle = null;
		try {
			bundle = null != bundleContext ? bundleContext.getBundle() : null;
		} catch (final IllegalStateException e) {
			// ignore;
		}

		// check if bundle was accessible
		if (null == bundle) {
			return "(destroyed)";
		}

		// return formatted string
		return MessageFormat.format("{0}(id {1})", bundle.getSymbolicName(), bundle.getBundleId());
	}

	/**
	 * Creates a new exception.
	 * 
	 * @param bundleContext
	 *            the {@link BundleContext} used to access the service
	 * @param serviceInterfaceName
	 *            the class name of the requested service interface
	 */
	public ServiceNotAvailableException(final BundleContext bundleContext, final String serviceInterfaceName) {
		this(bundleContext, serviceInterfaceName, null);
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param bundleContext
	 *            the {@link BundleContext} used to access the service
	 * @param serviceInterfaceName
	 *            the class name of the requested service interface
	 * @param cause
	 *            an optional underlying cause
	 */
	public ServiceNotAvailableException(final BundleContext bundleContext, final String serviceInterfaceClass, final Throwable cause) {
		super(MessageFormat.format("Service ''{0}'' is currently not available for bundle ''{1}''", serviceInterfaceClass, getSymbolicName(bundleContext)), cause);
	}
}
