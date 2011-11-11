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
package org.eclipse.gyrex.cloud.tests.internal.zookeeper.preferences;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import org.eclipse.gyrex.cloud.internal.CloudDebug;
import org.eclipse.gyrex.cloud.internal.preferences.ZooKeeperBasedPreferences;
import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.tests.internal.CloudTestsActivator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.zookeeper.ZKTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Simple stress tests for {@link ZooKeeperBasedPreferences}
 */
public class ZooKeeperPreferencesSimpleStressTests extends ZKTestCase {

	protected static final String KEY = "key";
	protected static final String DEFAULT_VALUE = "default-value";
	protected static final String VALUE = "value";

	private final String testablePreferenceName = "test";
	private TestablePreferencesService service;
	private IEclipsePreferences preferencesRoot;

	private void doTest01(final TestablePreferences root) throws BackingStoreException {
		final Preferences node = getNode(root, 11);
		final String value = VALUE + System.nanoTime();

		node.put(KEY, value);

		getNode(root, 11).put(KEY, value);
		getNode(root, 9).removeNode();
		getNode(root, 6).put(KEY, value);

		getNode(getNode(root, 6), 2).put(KEY, value);

		root.node(getNodePath(11)).flush();
		root.node(getNodePath(10)).flush();
		root.node(getNodePath(9)).flush();
		root.node(getNodePath(8)).flush();
		getNode(root, 7).flush();
		root.flush();

//		root.sync();

		assertEquals(value, getNode(getNode(root, 6), 2).get(KEY, DEFAULT_VALUE));
	}

	private Preferences getNode(final Preferences root, final int level) {
		Preferences node = root;
		for (int i = 0; i < level; i++) {
			node = node.node("l" + i);
		}
		return node;
	}

	private String getNodePath(final int level) {
		String path = "";
		for (int i = 0; i < level; i++) {
			if (i > 0) {
				path += IPath.SEPARATOR;
			}
			path += "l" + i;
		}
		return path;
	}

	private Preferences getRandomNode(final TestablePreferences root) {
		final int level = RandomUtils.nextInt(10);
		return getNode(root, level);
	}

	@Before
	public void setUp() throws Exception {
		// enable debugging
		CloudDebug.debug = true;
		CloudDebug.zooKeeperPreferences = true;

		// remove any existing data in ZooKeeper
		ZooKeeperGate.get().deletePath(IZooKeeperLayout.PATH_PREFERENCES_ROOT.append(testablePreferenceName));

		service = new TestablePreferencesService("test");
		assertTrue("must be connected", service.isConnected());

		preferencesRoot = CloudTestsActivator.getInstance().getService(IPreferencesService.class).getRootNode();
	}

	@After
	public void tearDown() throws Exception {
		if (null != service) {
			service.shutdown();
			service = null;
		}
	}

	@Test
	public void test01() throws Exception {
		final TestablePreferences root = new TestablePreferences(preferencesRoot, testablePreferenceName, service);

		for (int i = 0; i < 6; i++) {
			doTest01(root);
		}

	}
}
