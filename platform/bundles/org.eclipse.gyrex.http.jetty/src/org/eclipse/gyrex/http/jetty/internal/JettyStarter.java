/**
 * Copyright (c) 2008 AGETO Service GmbH and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.http.jetty.internal;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.equinox.http.jetty.JettyConfigurator;
import org.eclipse.equinox.http.jetty.JettyConstants;
import org.eclipse.gyrex.configuration.PlatformConfiguration;
import org.eclipse.gyrex.configuration.service.IConfigurationService;
import org.eclipse.gyrex.http.internal.HttpActivator;

final class JettyStarter extends Job {

	static final String ID_DEFAULT = "default";

	/**
	 * Creates a new instance.
	 */
	JettyStarter() {
		super("Jetty Starter");
	}

	Dictionary createSettings() {
		final IConfigurationService configurationService = PlatformConfiguration.getConfigurationService();

		final Dictionary<String, Object> settings = new Hashtable<String, Object>(4);
		settings.put(JettyConstants.OTHER_INFO, HttpActivator.TYPE_WEB);
		settings.put(JettyConstants.HTTP_ENABLED, Boolean.TRUE);
		settings.put(JettyConstants.HTTP_PORT, new Integer(configurationService.getInt(HttpJettyActivator.SYMBOLIC_NAME, "port", 80, null)));
		// note, we use the string here to not depend on inofficial API
		settings.put("customizer.class", "org.eclipse.gyrex.http.internal.GyrexJettyCustomizer");

		// enable SSL if necessary
		final int sslPort = configurationService.getInt(HttpJettyActivator.SYMBOLIC_NAME, JettyConstants.HTTPS_PORT, 0, null);
		if (sslPort > 0) {
			settings.put(JettyConstants.HTTPS_ENABLED, Boolean.TRUE);
			settings.put(JettyConstants.HTTPS_PORT, new Integer(sslPort));
			putOptionalSetting(settings, JettyConstants.SSL_KEYSTORE);
			putOptionalSetting(settings, JettyConstants.SSL_PASSWORD);
			putOptionalSetting(settings, JettyConstants.SSL_KEYPASSWORD);
		}

		return settings;
	}

	private void putOptionalSetting(final Dictionary<String, Object> settings, final String key) {
		final String value = PlatformConfiguration.getConfigurationService().getString(HttpJettyActivator.SYMBOLIC_NAME, key, null, null);
		if (null != value) {
			settings.put(key, value);
		}
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		monitor.beginTask("Staring Jetty", 100);
		try {
			try {
				JettyConfigurator.stopServer(ID_DEFAULT);
			} catch (final Exception e) {
				// ignore;
			}

			final Dictionary settings = createSettings();
			JettyConfigurator.startServer(ID_DEFAULT, settings);
		} catch (final Exception e) {
			return HttpActivator.getInstance().getStatusUtil().createError(0, "Failed starting Jetty: " + e.getMessage(), e);
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}
}