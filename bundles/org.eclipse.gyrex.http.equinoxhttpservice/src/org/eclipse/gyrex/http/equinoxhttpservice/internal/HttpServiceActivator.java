/*******************************************************************************
 * Copyright (c) 2010, 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.equinoxhttpservice.internal;

import java.text.MessageFormat;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.monitoring.diagnostics.IStatusConstants;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.osgi.framework.BundleContext;

public class HttpServiceActivator extends BaseBundleActivator {

	/** bundle symbolic name */
	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.http.equinoxhttpservice";

	private static final String PROP_JETTY_AUTOSTART = "org.eclipse.equinox.http.jetty.autostart";
	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	/**
	 * Creates a new instance.
	 */
	public HttpServiceActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		HttpServiceActivator.context = context;

		// check if Jetty default start is disabled
		final String autostart = context.getProperty(PROP_JETTY_AUTOSTART);
		if ((null == autostart) || !Boolean.FALSE.toString().equals(autostart)) {
			final IStatus status = new Status(IStatus.ERROR, SYMBOLIC_NAME, MessageFormat.format("The Jetty-based HTTP service is configured to startup automatically. However, this is discouraged in Gyrex. Please set the system property ''{0}'' to ''{1}''. Usually, the property is set in the config.ini before startup.", PROP_JETTY_AUTOSTART, Boolean.FALSE.toString()));
			getServiceHelper().registerService(IStatusConstants.SERVICE_NAME, status, "Eclipse Gyrex", "Jetty Auto-Start Error", SYMBOLIC_NAME.concat(".status.jettyautostart"), null);
		}
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		HttpServiceActivator.context = null;
	}

}
