/*******************************************************************************
 * Copyright (c) 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation in Jetty
 *******************************************************************************/
package org.eclipse.gyrex.boot.internal.jmx;

import javax.management.remote.JMXServiceURL;

import org.eclipse.gyrex.boot.internal.BootActivator;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.jetty.jmx.ConnectorServer;
import org.eclipse.osgi.service.environment.EnvironmentInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper around Jetty {@link ConnectorServer} for easier JMX access through
 * firewalls.
 */
public class JettyJmxConnector {

	private static final String PROPERTY_HOST = "gyrex.jmxrmi.host";
	private static final String PROPERTY_PORT = "gyrex.jmxrmi.port";

	private static final Logger LOG = LoggerFactory.getLogger(JettyJmxConnector.class);

	private static ConnectorServer connectorServer;

	public static synchronized void start() throws Exception {
		if (connectorServer != null) {
			throw new IllegalStateException("already started");
		}

		// do not start if running in production mode and not explicitly set
		final EnvironmentInfo info = BootActivator.getEnvironmentInfo();
		if (info.getProperty("gyrex.jmxrmi.skip") != null) {
			return;
		}

		// use defaults from http://wiki.eclipse.org/Jetty/Tutorial/JMX#Enabling_JMXConnectorServer_for_Remote_Access
		String host = "localhost";
		int port = Platform.getInstancePort(1099);

		// allow port and host override through arguments
		if (info.getProperty(PROPERTY_PORT) != null) {
			try {
				port = Integer.parseInt(info.getProperty(PROPERTY_PORT));
			} catch (final Exception e) {
				throw new IllegalArgumentException(String.format("Invalid JMX port (%s).", info.getProperty(PROPERTY_PORT)), e);
			}
		}
		if (info.getProperty(PROPERTY_HOST) != null) {
			host = info.getProperty(PROPERTY_HOST);
		}

		// TODO: may want to support protected access using <instance-location>/etc/jmx/... files

		LOG.info("Enabling JMX remote connections on port {} (host {}).", new Object[] { port, host });

		final JMXServiceURL url = new JMXServiceURL("rmi", host, port, String.format("/jndi/rmi://%s:%d/jmxrmi", host, port));
		connectorServer = new ConnectorServer(url, null, "org.eclipse.gyrex.jmx:name=rmiconnectorserver");
		connectorServer.start();
	}

	public static synchronized void stop() throws Exception {
		if (connectorServer == null) {
			return;
		}

		connectorServer.stop();
		connectorServer = null;
	}
}
