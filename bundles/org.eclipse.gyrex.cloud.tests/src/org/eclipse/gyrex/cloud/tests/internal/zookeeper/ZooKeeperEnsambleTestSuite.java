/*******************************************************************************
 * Copyright (c) 2011, 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.tests.internal.zookeeper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.cloud.admin.ICloudManager;
import org.eclipse.gyrex.cloud.admin.INodeConfigurer;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperNodeInfo;
import org.eclipse.gyrex.cloud.tests.internal.CloudTestsActivator;
import org.eclipse.gyrex.cloud.tests.internal.zookeeper.CountdownCloudStateHandler.CloudStateEvent;
import org.eclipse.gyrex.cloud.tests.internal.zookeeper.preferences.AllZooKeeperPreferencesNonEnsembleTests;
import org.eclipse.gyrex.cloud.tests.internal.zookeeper.preferences.ZooKeeperPreferencesEnsambleTests;
import org.eclipse.gyrex.junit.GyrexServerResource;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.IStatus;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Suite.class)
@SuiteClasses({ ZooKeeperEnsambleTest.class, ZooKeeperPreferencesEnsambleTests.class, AllZooKeeperPreferencesNonEnsembleTests.class })
public class ZooKeeperEnsambleTestSuite {

	@ClassRule
	public static final GyrexServerResource server = new GyrexServerResource();

	private static final int CONNECT_TIMEOUT = 30000;

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperEnsambleTestSuite.class);

	@BeforeClass
	public static void setUp() throws Exception {
		assertTrue("not-in-development-mode", Platform.inDevelopmentMode());

		EnsembleHelper.startQuorum();

		final CountdownCloudStateHandler cloudState = new CountdownCloudStateHandler();

		final ICloudManager cloudManager = CloudTestsActivator.getInstance().getService(ICloudManager.class);
		final String myNodeId = cloudManager.getLocalInfo().getNodeId();

		final INodeConfigurer configurer = cloudManager.getNodeConfigurer(myNodeId);
		final String connectString = EnsembleHelper.getQuorumUtil().getConnString(); //ZooKeeperEnsembleHelper.getClientConnectString();

		LOG.info("Reconfiguring node to ensemble {}", connectString);
		final IStatus result = configurer.configureConnection(connectString);
		if (!result.isOK()) {
			fail(String.format("unable to configure ensemble connection using %s: %s", connectString, result));
		}

		// wait for the cloud to become OFFLINE (should be two event)
		final CloudStateEvent s1 = cloudState.events().poll(20, TimeUnit.SECONDS);
		assertEquals("node-not-offline", CloudStateEvent.OFFLINE, s1);
		assertTrue("queue-not-empty-more-state-events-than-expected", cloudState.events().isEmpty());

		// wait for the gate to become UP again
		new CountdownGateListener().waitForUp(CONNECT_TIMEOUT);

		// now sleep a little to allow CloudState to populate ZK correctly
		Thread.sleep(5000);

		assertEquals("No node should be online", 0, cloudManager.getOnlineNodes().size());
		assertEquals("One node should be pending.", 1, cloudManager.getPendingNodes().size());
		assertEquals("This node should be pending.", myNodeId, cloudManager.getPendingNodes().iterator().next().getId());
		assertEquals("No node should be approved.", 0, cloudManager.getApprovedNodes().size());

		// now approve node
		cloudState.reset();
		ZooKeeperNodeInfo.approve(myNodeId, "test-client", "localhost");

		// wait for cloud to become online
		cloudState.waitForOnline(CONNECT_TIMEOUT);

		assertEquals("One node should be online", 1, cloudManager.getOnlineNodes().size());
		assertEquals("This node should be online.", myNodeId, cloudManager.getOnlineNodes().iterator().next());
		assertEquals("One node should be approved.", 1, cloudManager.getApprovedNodes().size());
		assertEquals("This node should be approved.", myNodeId, cloudManager.getApprovedNodes().iterator().next().getId());
		assertEquals("No node should be pending.", 0, cloudManager.getPendingNodes().size());

		// cloud should now become online, i.e. last item on the queue must be an ONLINE event
		final CloudStateEvent s2 = cloudState.events().pollLast(20, TimeUnit.SECONDS);
		assertEquals("node-not-online", CloudStateEvent.ONLINE, s2);

		// note, depending on the situation not always a smart refresh applies
		// this, the node may send OFFLINE events before, we verify those
		while (null != cloudState.events().peek()) {
			assertEquals("only-offline-events-expected-in-stack", CloudStateEvent.OFFLINE, cloudState.events().poll());
		}

		// now check the gate is connected to the correct node
		assertEquals("connect-string-must-match", connectString, ZooKeeperGate.get().getConnectString());
	}

	@AfterClass
	public static void tearDown() throws Exception {
		// shutdown
		EnsembleHelper.shutdown();
	}
}
