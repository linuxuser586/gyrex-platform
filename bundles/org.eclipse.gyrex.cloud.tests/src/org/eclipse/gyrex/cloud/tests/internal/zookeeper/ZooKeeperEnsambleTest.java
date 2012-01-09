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

import org.eclipse.gyrex.cloud.tests.internal.zookeeper.CountdownCloudStateHandler.CloudStateEvent;

import org.junit.Test;

public class ZooKeeperEnsambleTest extends BaseEnsambleTest {

	@Test
	public void testReconnect() throws Exception {
		final CountdownCloudStateHandler cloudState = new CountdownCloudStateHandler();

		// kill the server the client is connected to
		EnsembleHelper.shutdownConnectedPeer();

		// wait for re-connect
		cloudState.waitForOnline(CONNECT_TIMEOUT);

		// events in the queue should be INTERRUPTED -> ONLINE
		assertEquals("should have two events in queue", 2, cloudState.events().size());
		assertEquals("wrong event triggered", CloudStateEvent.INTERRUPTED, cloudState.events().poll());
		assertEquals("should have triggered", CloudStateEvent.ONLINE, cloudState.events().poll());
	}

}
