/**
 * Copyright (c) 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.preferences.tests.internal;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.preferences.CloudScope;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
@SuppressWarnings("restriction")
public class ZooKeeperPreferencesTests {

	/** TESTS */
	private static final String TESTS = "tests";

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testEmptyNodes() throws Exception {
		final IEclipsePreferences rootNode = CloudScope.INSTANCE.getNode(TESTS);
		final String testNodeName = "testNode" + System.currentTimeMillis();
		if (rootNode.nodeExists(testNodeName)) {
			rootNode.node(testNodeName).removeNode();
			rootNode.flush();
		}

		assertFalse("test node must not exists after flush", rootNode.nodeExists(testNodeName));

		// also check that the path does not exists in ZooKeeper
		final IPath testNodeZkPath = IZooKeeperLayout.PATH_PREFERENCES_ROOT.append("cloud").append(TESTS).append(testNodeName);
		assertFalse("test node path must not exists in ZooKeeper after flush", ZooKeeperGate.get().exists(testNodeZkPath));

		// create an empty node and flush
		rootNode.node(testNodeName);
		rootNode.flush();

		assertTrue("test node must exists after flush", rootNode.nodeExists(testNodeName));
		assertTrue("test node path must exists in ZooKeeper after flush", ZooKeeperGate.get().exists(testNodeZkPath));

	}
}
