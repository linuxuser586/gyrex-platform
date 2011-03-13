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
package org.eclipse.gyrex.boot.tests.internal;

import org.eclipse.equinox.app.IApplication;

import org.eclipse.gyrex.boot.internal.app.ServerApplication;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.internal.junit.runtime.RemotePluginTestRunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class ServerTestApplication extends ServerApplication implements IApplication {

	private static final Logger LOG = LoggerFactory.getLogger(ServerTestApplication.class);

	@Override
	protected void onServerStarted(final String[] arguments) {
		// schedule test executions
		final Job testRunner = new Job("PDE JUnit Test Runner") {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				try {
					// test for bogus system properties
					if (Boolean.getBoolean("gyrex.preferences.instancebased")) {
						LOG.warn("Overriding system propert 'gyrex.preferences.instancebased' in order to force ZooKeeper based cloud preferences!");
						System.setProperty("gyrex.preferences.instancebased", Boolean.FALSE.toString());
					}

					// wait for cloud connection
					long timeout = 20000;
					ZooKeeperGate gate = null;
					while ((timeout > 0) && (gate == null)) {
						try {
							gate = ZooKeeperGate.get();
						} catch (final IllegalStateException e) {
							LOG.info("waiting for ZooKeeper connection...");
							timeout -= 250;
							Thread.sleep(250);
						}
					}

					// final test
					if (gate == null) {
						ServerApplication.signalShutdown(new Exception("Timeout while waiting for ZooKeeper connection. Unable to initialize cloud environment. Test execution aborted!"));
						return Status.CANCEL_STATUS;
					}

					// TODO: implement optional wait for node approval (maybe using server role?)

					// execute tests
					RemotePluginTestRunner.main(arguments);

					// shutdown
					ServerApplication.signalShutdown(null);
				} catch (final Exception e) {
					ServerApplication.signalShutdown(e);
				}
				return Status.OK_STATUS;
			}
		};
		testRunner.schedule();
	}

}
