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
package org.eclipse.gyrex.cloud.tests.internal.zookeeper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.cloud.admin.ICloudManager;
import org.eclipse.gyrex.cloud.admin.INodeConfigurer;
import org.eclipse.gyrex.cloud.events.ICloudEventConstants;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperNodeInfo;
import org.eclipse.gyrex.cloud.tests.internal.CloudTestsActivator;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.IStatus;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import org.apache.commons.lang.StringUtils;
import org.apache.zookeeper.ZKTestCase;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.test.QuorumUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ZooKeeperGateTests extends ZKTestCase {

	static enum CloudState {
		ONLINE, OFFLINE, INTERRUPTED
	}

	private static final int CONNECT_TIMEOUT = 30000;

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperGateTests.class);

	static final BlockingDeque<CloudState> cloudStateQueue = new LinkedBlockingDeque<CloudState>();
	private static final EventHandler cloudStateHandler = new EventHandler() {
		@Override
		public void handleEvent(final Event event) {
			LOG.debug("Received cloud event: {}", event);
			if (StringUtils.equals(event.getTopic(), ICloudEventConstants.TOPIC_NODE_ONLINE)) {
				cloudStateQueue.add(CloudState.ONLINE);
			} else if (StringUtils.equals(event.getTopic(), ICloudEventConstants.TOPIC_NODE_OFFLINE)) {
				cloudStateQueue.add(CloudState.OFFLINE);
			} else if (StringUtils.equals(event.getTopic(), ICloudEventConstants.TOPIC_NODE_INTERRUPTED)) {
				cloudStateQueue.add(CloudState.INTERRUPTED);
			}
		}
	};
	private ServiceRegistration<EventHandler> cloudStateHandlerRegistration;

	private int findTargetPeer(final QuorumUtil qu) throws Exception {
		final ZooKeeper zk = getZooKeeperFromGate();

		final Method testableRemoteSocketAddress = ZooKeeper.class.getDeclaredMethod("testableRemoteSocketAddress");
		if (!testableRemoteSocketAddress.isAccessible()) {
			testableRemoteSocketAddress.setAccessible(true);
		}

		final InetSocketAddress address = (InetSocketAddress) testableRemoteSocketAddress.invoke(zk);
		final int port = address.getPort();

		for (int i = 1; i <= qu.ALL; i++) {
			if (qu.getPeer(i).clientPort == port) {
				return i;
			}
		}

		throw new IllegalStateException("no matching target peer found");
	}

	private ZooKeeper getZooKeeperFromGate() throws IllegalStateException {
		try {
			final Method ensureConnected = ZooKeeperGate.class.getDeclaredMethod("ensureConnected");
			if (!ensureConnected.isAccessible()) {
				ensureConnected.setAccessible(true);
			}
			return (ZooKeeper) ensureConnected.invoke(ZooKeeperGate.get());
		} catch (final IllegalStateException e) {
			throw e;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Before
	public void setUp() throws Exception {
		// register listeners
		cloudStateQueue.clear();
		final BundleContext context = CloudTestsActivator.getInstance().getBundle().getBundleContext();
		final Hashtable<String, Object> properties = new Hashtable<String, Object>(1);
		properties.put(EventConstants.EVENT_TOPIC, Arrays.asList(ICloudEventConstants.TOPIC_NODE_ONLINE, ICloudEventConstants.TOPIC_NODE_OFFLINE, ICloudEventConstants.TOPIC_NODE_INTERRUPTED));
		cloudStateHandlerRegistration = context.registerService(EventHandler.class, cloudStateHandler, properties);
	}

	@After
	public void tearDown() throws Exception {
		cloudStateHandlerRegistration.unregister();
		cloudStateQueue.clear();
	}

	/**
	 * The first test ensures that the local node can be reconfigured from
	 * standalone to an ensemble.
	 * <p>
	 * This test must be first because it initiates a state which all following
	 * tests in this method rely on. We might need to refactor this.
	 * </p>
	 * <p>
	 * This test also makes a few assumption about the environment.
	 * </p>
	 */
	@Test
	public void testZooKeeperGateInEnsemble() throws Exception {
		assertTrue("not-in-development-mode", Platform.inDevelopmentMode());

		final QuorumUtil qu = new QuorumUtil(2); // note, 1 is not enough as we need a quorum
		qu.startAll();

		final CountdownCloudStateHandler cloudState = new CountdownCloudStateHandler();

		final ICloudManager cloudManager = CloudTestsActivator.getInstance().getService(ICloudManager.class);
		final String myNodeId = cloudManager.getLocalInfo().getNodeId();

		final INodeConfigurer configurer = cloudManager.getNodeConfigurer(myNodeId);
		final String connectString = qu.getConnString(); //ZooKeeperEnsembleHelper.getClientConnectString();

		LOG.info("Reconfiguring node to ensemble {}", connectString);
		final IStatus result = configurer.configureConnection(connectString);
		if (!result.isOK()) {
			fail(String.format("unable to configure ensemble connection using %s: %s", connectString, result));
		}

		// wait for the cloud to become OFFLINE (should be two event)
		final CloudState s1 = cloudStateQueue.poll(20, TimeUnit.SECONDS);
		assertEquals("node-not-offline", CloudState.OFFLINE, s1);
		assertTrue("queue-not-empty-more-state-events-than-expected", cloudStateQueue.isEmpty());

		// wait for the gate to become UP again
		new CountdownGateListener().waitForUp(CONNECT_TIMEOUT);

		// now sleep a little to allow CloudState to populate ZK correctly
		Thread.sleep(2000);

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
		final CloudState s2 = cloudStateQueue.pollLast(20, TimeUnit.SECONDS);
		assertEquals("node-not-online", CloudState.ONLINE, s2);

		// note, depending on the situation not always a smart refresh applies
		// this, the node may send OFFLINE events before, we verify those
		while (null != cloudStateQueue.peek()) {
			assertEquals("only-offline-events-expected-in-stack", CloudState.OFFLINE, cloudStateQueue.poll());
		}

		// now check the gate is connected to the correct node
		assertEquals("connect-string-must-match", connectString, ZooKeeperGate.get().getConnectString());

		// find the connected server
		final int peer = findTargetPeer(qu);

		// clear queue
		cloudStateQueue.clear();
		cloudState.reset();

		// kill the server the client is connected to
		qu.shutdown(peer);

		// wait for re-connect
		cloudState.waitForOnline(CONNECT_TIMEOUT);

		// events in the queue should not be INTERRUPTED -> ONLINE
		assertEquals("should have two events in queue", 2, cloudStateQueue.size());
		assertEquals("wrong event triggered", CloudState.INTERRUPTED, cloudStateQueue.poll());
		assertEquals("should have triggered", CloudState.ONLINE, cloudStateQueue.poll());

		qu.shutdownAll();
	}

}
