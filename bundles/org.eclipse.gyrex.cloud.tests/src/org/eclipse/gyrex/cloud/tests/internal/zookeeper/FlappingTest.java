/*******************************************************************************
 * Copyright (c) 2013 <enter-company-name-here> and others.
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.eclipse.gyrex.cloud.internal.zk.AtomicReferenceWithFlappingDetection;

import org.apache.zookeeper.Watcher.Event.KeeperState;

import org.junit.Test;

public class FlappingTest {

	@Test
	public void test() throws Exception {
		final AtomicReferenceWithFlappingDetection<KeeperState> ref = new AtomicReferenceWithFlappingDetection<>(4);

		ref.set(KeeperState.SyncConnected);
		assertFalse(ref.isFlapping(System.currentTimeMillis() - 60000, 3));
		assertFalse(ref.isFlapping(System.currentTimeMillis() - 60000, 4));
		assertFalse(ref.isFlapping(System.currentTimeMillis() - 60000, 1));
		assertTrue(ref.isFlapping(System.currentTimeMillis() - 60000, 0));

		try {
			ref.isFlapping(System.currentTimeMillis() - 60000, 5);
			fail("IllegalArgumentException expected");
		} catch (final IllegalArgumentException e) {
			// good
		}

		ref.set(KeeperState.SyncConnected);
		assertFalse(ref.isFlapping(System.currentTimeMillis() - 60000, 1));
		assertTrue(ref.isFlapping(System.currentTimeMillis() - 60000, 0));

		ref.set(KeeperState.Disconnected);
		assertFalse(ref.isFlapping(System.currentTimeMillis() - 60000, 4));
		assertFalse(ref.isFlapping(System.currentTimeMillis() - 60000, 3));
		assertFalse(ref.isFlapping(System.currentTimeMillis() - 60000, 2));
		assertTrue(ref.isFlapping(System.currentTimeMillis() - 60000, 1));
		assertTrue(ref.isFlapping(System.currentTimeMillis() - 60000, 0));

		ref.set(KeeperState.Disconnected);
		assertFalse(ref.isFlapping(System.currentTimeMillis() - 60000, 4));
		assertFalse(ref.isFlapping(System.currentTimeMillis() - 60000, 3));
		assertFalse(ref.isFlapping(System.currentTimeMillis() - 60000, 2));
		assertTrue(ref.isFlapping(System.currentTimeMillis() - 60000, 1));
		assertTrue(ref.isFlapping(System.currentTimeMillis() - 60000, 0));

		ref.set(KeeperState.SyncConnected);
		assertFalse(ref.isFlapping(System.currentTimeMillis() - 60000, 4));
		assertFalse(ref.isFlapping(System.currentTimeMillis() - 60000, 3));
		assertTrue(ref.isFlapping(System.currentTimeMillis() - 60000, 2));
		assertTrue(ref.isFlapping(System.currentTimeMillis() - 60000, 1));
		assertTrue(ref.isFlapping(System.currentTimeMillis() - 60000, 0));

		ref.set(KeeperState.SyncConnected);
		assertFalse(ref.isFlapping(System.currentTimeMillis() - 60000, 4));
		assertFalse(ref.isFlapping(System.currentTimeMillis() - 60000, 3));
		assertTrue(ref.isFlapping(System.currentTimeMillis() - 60000, 2));
		assertTrue(ref.isFlapping(System.currentTimeMillis() - 60000, 1));
		assertTrue(ref.isFlapping(System.currentTimeMillis() - 60000, 0));

		ref.set(KeeperState.Disconnected);
		assertFalse(ref.isFlapping(System.currentTimeMillis() - 60000, 4));
		assertTrue(ref.isFlapping(System.currentTimeMillis() - 60000, 3));
		assertTrue(ref.isFlapping(System.currentTimeMillis() - 60000, 2));
		assertTrue(ref.isFlapping(System.currentTimeMillis() - 60000, 1));
		assertTrue(ref.isFlapping(System.currentTimeMillis() - 60000, 0));

		Thread.sleep(1000);
		assertFalse(ref.isFlapping(System.currentTimeMillis(), 4));
		assertFalse(ref.isFlapping(System.currentTimeMillis(), 3));
		assertFalse(ref.isFlapping(System.currentTimeMillis(), 1));
		assertFalse(ref.isFlapping(System.currentTimeMillis(), 0));
	}

}
