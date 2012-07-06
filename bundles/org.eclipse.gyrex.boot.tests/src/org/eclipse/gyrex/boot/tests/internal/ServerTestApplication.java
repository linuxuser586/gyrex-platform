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

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.equinox.app.IApplication;

import org.eclipse.gyrex.boot.internal.app.ServerApplication;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class ServerTestApplication extends ServerApplication implements IApplication {

	private static final Logger LOG = LoggerFactory.getLogger(ServerTestApplication.class);

	private boolean looksLikeTychoSurefireEnvironment(final String[] applicationArguments) {
		// quick check for '-testproperties' argument
		for (final String arg : applicationArguments) {
			if (StringUtils.equalsIgnoreCase("-testproperties", arg)) {
				return true;
			}
		}
		return false;
	}

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
					runTests(getApplicationArguments(arguments));
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

	@Override
	protected void onBeforeStart(final Map arguments) throws Exception {
		// clean-up ZooKeeper
		final File zooKeeperDir = Platform.getInstanceLocation().append("zookeeper").toFile();
		if (zooKeeperDir.isDirectory()) {
			FileUtils.deleteDirectory(zooKeeperDir);
		}

		// remove any custom state
		final File stateArea = Platform.getInstanceLocation().append(".metadata/.plugins").toFile();
		if (stateArea.isDirectory()) {
			FileUtils.deleteDirectory(stateArea);
		}
	}

	void runTests(final String[] args) {
		// note, we use reflection here in order to avoid compile-time dependencies as well
		if (looksLikeTychoSurefireEnvironment(args)) {
			LOG.info("Running in Tycho OSGi Surefire environment.");
			try {
				final Class<?> osgiSurefireBooterClass = Activator.getInstance().getBundle().loadClass("org.eclipse.tycho.surefire.osgibooter.OsgiSurefireBooter");
				osgiSurefireBooterClass.getMethod("run", String[].class).invoke(null, new Object[] { args });
			} catch (final Exception e) {
				throw new IllegalStateException("Unable to execute tests using Tycho OSGi Surefire booter. Please verify bundle 'org.eclipse.tycho.surefire.osgibooter' is available!", e);
			}
		} else {
			LOG.info("Running in PDE JUnit Plug-in Test environment.");
			try {
				final Class<?> pdeJunitRunnerClass = Activator.getInstance().getBundle().loadClass("org.eclipse.pde.internal.junit.runtime.RemotePluginTestRunner");
				pdeJunitRunnerClass.getMethod("main", String[].class).invoke(null, new Object[] { args });
			} catch (final Exception e) {
				throw new IllegalStateException("Unable to execute tests using PDE JUnit Plug-in Test runner. Please verify bundle 'org.eclipse.pde.junit.runtime' is available!", e);
			}
		}
	}
}
