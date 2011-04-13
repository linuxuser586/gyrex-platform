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

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.equinox.app.IApplication;

import org.eclipse.gyrex.boot.internal.app.ServerApplication;

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
	protected void onApplicationStarted(final Map arguments) {
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

					//  wait for node becoming online
					final long timeout = Long.getLong("gyrex.servertestapp.timeout", 60000l);
					LOG.info("Waiting {}ms for node to become online...", timeout);
					if (!Activator.cloudOnlineWatch.await(timeout, TimeUnit.MILLISECONDS)) {
						LOG.error("Timeout waiting for ZooKeeper connection.");
						ServerApplication.shutdown(new Exception("Timeout while waiting for node to establish connection with cloud. Unable to initialize cloud environment. Test execution aborted!"));
						return Status.CANCEL_STATUS;
					}

					// execute tests
					LOG.info("Executing tests...");
					RemotePluginTestRunner.main(getApplicationArguments(arguments));
					LOG.info("Finished executing tests.");

					// shutdown
					ServerApplication.shutdown(null);
				} catch (final Exception e) {
					LOG.error("Failed executing tests. Signaling shutdown!", e);
					ServerApplication.shutdown(e);
				}
				return Status.OK_STATUS;
			}
		};
		testRunner.schedule();
	}
}
