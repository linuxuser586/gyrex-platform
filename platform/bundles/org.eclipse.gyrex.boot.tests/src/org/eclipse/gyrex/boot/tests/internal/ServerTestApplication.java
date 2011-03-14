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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.equinox.app.IApplication;

import org.eclipse.gyrex.boot.internal.app.ServerApplication;
import org.eclipse.gyrex.cloud.internal.CloudState;
import org.eclipse.gyrex.cloud.internal.NodeInfo;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate.IConnectionMonitor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.internal.junit.runtime.RemotePluginTestRunner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class ServerTestApplication extends ServerApplication implements IApplication {

	private static final long SLEEP_TIME = 500l;
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

					// prepare wait for cloud connection
					final long timeout = Long.getLong("gyrex.servertestapp.timeout", 60000l);
					final CountDownLatch connectedSignal = new CountDownLatch(1);
					ZooKeeperGate.addConnectionMonitor(new IConnectionMonitor() {
						@Override
						public void connected(final ZooKeeperGate gate) {
							connectedSignal.countDown();
						}

						@Override
						public void disconnected(final ZooKeeperGate gate) {
							LOG.warn("ZooKeeper disconnected! No more tests should be run now.");
						}
					});

					// now wait for cloud connection
					LOG.info("waiting for ZooKeeper connection...");
					if (!connectedSignal.await(timeout, TimeUnit.MILLISECONDS)) {
						LOG.error("Timeout waiting for ZooKeeper connection.");
						ServerApplication.signalShutdown(new Exception("Timeout while waiting for ZooKeeper connection. Unable to initialize cloud environment. Test execution aborted!"));
						return Status.CANCEL_STATUS;
					}

					//  wait for node approval
					long approvalTimeout = 5000;
					NodeInfo nodeInfo = CloudState.getNodeInfo();
					while (((nodeInfo == null) || !nodeInfo.isApproved()) && (approvalTimeout > 0)) {
						LOG.info("Waiting for automatic approval of node...");
						approvalTimeout -= SLEEP_TIME;
						Thread.sleep(SLEEP_TIME);
						nodeInfo = CloudState.getNodeInfo();
					}
					if ((nodeInfo == null) || !nodeInfo.isApproved()) {
						LOG.error("Timeout waiting for automatic node approval.");
						ServerApplication.signalShutdown(new Exception("Timeout while waiting for automatic node approval. Unable to initialize cloud environment. Test execution aborted!"));
						return Status.CANCEL_STATUS;
					}

					// execute tests
					LOG.info("Executing tests...");
					RemotePluginTestRunner.main(arguments);
					LOG.info("Finished executing tests.");

					// shutdown
					ServerApplication.signalShutdown(null);
				} catch (final Exception e) {
					LOG.error("Failed executing tests. Signaling shutdown!", e);
					ServerApplication.signalShutdown(e);
				}
				return Status.OK_STATUS;
			}
		};
		testRunner.schedule();
	}
}
