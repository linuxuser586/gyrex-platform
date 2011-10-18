/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.tests.internal.zookeeper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.cloud.admin.ICloudManager;
import org.eclipse.gyrex.cloud.admin.INodeConfigurer;
import org.eclipse.gyrex.cloud.events.ICloudEventConstants;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGateListener;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class ZooKeeperGateTests {

	static enum CloudState {
		ONLINE, OFFLINE, INTERRUPTED
	}

	static final BlockingDeque<CloudState> cloudStateQueue = new LinkedBlockingDeque<CloudState>();
	private static final EventHandler cloudStateHandler = new EventHandler() {
		@Override
		public void handleEvent(final Event event) {
			if (StringUtils.equals(event.getTopic(), ICloudEventConstants.TOPIC_NODE_ONLINE)) {
				cloudStateQueue.add(CloudState.ONLINE);
			}
			if (StringUtils.equals(event.getTopic(), ICloudEventConstants.TOPIC_NODE_OFFLINE)) {
				cloudStateQueue.add(CloudState.OFFLINE);
			}
			if (StringUtils.equals(event.getTopic(), ICloudEventConstants.TOPIC_NODE_INTERRUPTED)) {
				cloudStateQueue.add(CloudState.INTERRUPTED);
			}
		}
	};
	private ServiceRegistration<EventHandler> cloudStateHandlerRegistration;

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
	public void test001_configureGateToEnsemble() throws Exception {
		assertTrue("not-in-development-mode", Platform.inDevelopmentMode());

		final ICloudManager cloudManager = CloudTestsActivator.getInstance().getService(ICloudManager.class);
		final String myNodeId = cloudManager.getLocalInfo().getNodeId();

		assertEquals("only-one-node-should-be-online", 1, cloudManager.getOnlineNodes().size());
		assertEquals("only-this-node-should-be-online", myNodeId, cloudManager.getOnlineNodes().iterator().next());

		final INodeConfigurer configurer = cloudManager.getNodeConfigurer(myNodeId);
		final String connectString = ZooKeeperEnsembleHelper.getClientConnectString();

		final IStatus result = configurer.configureConnection(connectString);
		if (!result.isOK()) {
			fail(String.format("unable to configure ensemble connection using %s: %s", connectString, result));
		}

		// wait for the cloud to become OFFLINE (should be two event)
		final CloudState s1 = cloudStateQueue.poll(20, TimeUnit.SECONDS);
		assertEquals("node-not-offline", CloudState.OFFLINE, s1);
		assertTrue("queue-not-empty-more-state-events-than-expected", cloudStateQueue.isEmpty());

		// wait for the gate to become UP again
		waitForGate(20, TimeUnit.SECONDS);

		// now approve node
		ZooKeeperNodeInfo.approve(myNodeId, "test-client", "localhost");

		// sleep a little to allow event propagate
		Thread.sleep(5000);

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
	}

	private void waitForGate(final int timeout, final TimeUnit unit) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		final ZooKeeperGateListener listener = new ZooKeeperGateListener() {

			@Override
			public void gateDown(final ZooKeeperGate gate) {
				// empty
			}

			@Override
			public void gateRecovering(final ZooKeeperGate gate) {
				// empty
			}

			@Override
			public void gateUp(final ZooKeeperGate gate) {
				latch.countDown();
			}
		};

		ZooKeeperGate.addConnectionMonitor(listener);
		try {
			ZooKeeperGate.get();
		} catch (final IllegalStateException e) {
			if (!latch.await(timeout, unit)) {
				throw new IllegalStateException("timeout waiting for gate to come up");
			}
		} finally {
			ZooKeeperGate.removeConnectionMonitor(listener);
		}

	}

}
