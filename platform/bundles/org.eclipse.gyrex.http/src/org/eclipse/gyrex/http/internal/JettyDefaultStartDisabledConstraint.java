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
package org.eclipse.gyrex.http.internal;

import java.text.MessageFormat;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.gyrex.configuration.constraints.PlatformConfigurationConstraint;
import org.osgi.framework.BundleContext;

/**
 * Checks that the Jetty-based HTTP Service auto start is disabled.
 */
public class JettyDefaultStartDisabledConstraint extends PlatformConfigurationConstraint {

	private static final String PROP_JETTY_AUTOSTART = "org.eclipse.equinox.http.jetty.autostart";

	private final BundleContext context;

	JettyDefaultStartDisabledConstraint(final BundleContext context) {
		if (null == context) {
			throw new IllegalArgumentException("context must not be null");
		}
		this.context = context;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.configuration.service.ConfigurationConstraint#evaluateConfiguration(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public IStatus evaluateConfiguration(final IProgressMonitor progressMonitor) {
		final String autostart = context.getProperty(PROP_JETTY_AUTOSTART);
		if ((null == autostart) || !Boolean.FALSE.toString().equals(autostart)) {
			return new Status(IStatus.ERROR, HttpActivator.PLUGIN_ID, MessageFormat.format("The Jetty-based HTTP is configured to startup automatically. However, this is discouraged on Gyrex. Please set the system property ''{0}'' to ''{1}''. Usually, the property is set in the config.ini before startup.", PROP_JETTY_AUTOSTART, Boolean.FALSE.toString()));
		}
		return Status.OK_STATUS;
	}
}
