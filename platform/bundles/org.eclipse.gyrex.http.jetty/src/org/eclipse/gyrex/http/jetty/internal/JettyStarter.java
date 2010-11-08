/**
 * Copyright (c) 2008, 2010 AGETO Service GmbH and others.
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

import org.eclipse.gyrex.http.internal.HttpActivator;
import org.eclipse.gyrex.preferences.PlatformScope;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jetty.http.HttpGenerator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;

final class JettyStarter extends Job {

	static final String ID_DEFAULT = "default";
	private final Server server;

	/**
	 * Creates a new instance.
	 * 
	 * @param server
	 */
	JettyStarter(final Server server) {
		super("Jetty Starter");
		this.server = server;
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		monitor.beginTask("Staring Jetty", 100);
		try {
			final IEclipsePreferences preferences = new PlatformScope().getNode(HttpJettyActivator.SYMBOLIC_NAME);

			final SelectChannelConnector connector = new SelectChannelConnector();
			connector.setPort(preferences.getInt("port", 80));
			connector.setMaxIdleTime(200000);
			connector.setAcceptors(2);
			connector.setStatsOn(false);
			connector.setConfidentialPort(443);
			connector.setLowResourcesConnections(20000);
			connector.setLowResourcesMaxIdleTime(5000);
			connector.setForwarded(true);
			server.addConnector(connector);

			// enable SSL if necessary
			final int sslPort = preferences.getInt("https.port", 0);
			if (sslPort > 0) {
				final SslSelectChannelConnector sslConnector = new SslSelectChannelConnector();
				sslConnector.setPort(sslPort);
				sslConnector.setKeystore(preferences.get("ssl.keystore", null));
				sslConnector.setPassword(preferences.get("ssl.password", null));
				sslConnector.setKeyPassword(preferences.get("ssl.keypassword", null));
				sslConnector.setMaxIdleTime(200000);
				sslConnector.setAcceptors(2);
				sslConnector.setStatsOn(false);
				sslConnector.setConfidentialPort(sslPort);
				sslConnector.setLowResourcesConnections(20000);
				sslConnector.setLowResourcesMaxIdleTime(5000);
				sslConnector.setForwarded(true);
				server.addConnector(sslConnector);
			}

			// start the server
			server.start();

			// don't expose too detailed version info
			// (must be set after server started)
			HttpGenerator.setServerVersion("7");

		} catch (final Exception e) {
			return HttpActivator.getInstance().getStatusUtil().createError(0, "Failed starting Jetty: " + e.getMessage(), e);
		} finally {
			monitor.done();
		}
		return Status.OK_STATUS;
	}
}