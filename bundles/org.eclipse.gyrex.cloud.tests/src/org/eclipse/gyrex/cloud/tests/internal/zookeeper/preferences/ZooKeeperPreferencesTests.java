/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
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
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import org.eclipse.gyrex.cloud.internal.CloudDebug;
import org.eclipse.gyrex.cloud.internal.preferences.ZooKeeperBasedPreferences;
import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.tests.internal.CloudTestsActivator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IPreferencesService;

import org.osgi.service.prefs.BackingStoreException;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZKTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Bases tests for {@link ZooKeeperBasedPreferences}
 */
public class ZooKeeperPreferencesTests extends ZKTestCase {

	protected static final String KEY = "key";
	protected static final String DEFAULT_VALUE = "default-value";
	protected static final String VALUE = "value";

	private final String testablePreferenceName = "test";
	private TestablePreferencesService service;
	private IEclipsePreferences preferencesRoot;

	private void assertExists(final TestablePreferences parent, final String childName, final TestablePreferences child) throws BackingStoreException {
		assertEquals(String.format("node %s must have the specified child name", child.absolutePath()), childName, child.name());
		assertFalse(String.format("node %s must not be removed", child.absolutePath()), child.testableRemoved());
		assertTrue(String.format("node %s  must be contained in parent", child.absolutePath()), parent.testableGetChildren().containsKey(child.name()));
		assertTrue(String.format("node %s  must be contained in parent", child.absolutePath()), parent.testableGetChildren().containsValue(child));
		assertTrue(String.format("node %s  must exists", child.absolutePath()), child.nodeExists(""));
		assertTrue(String.format("node %s  must exists", child.absolutePath()), parent.nodeExists(child.name()));
	}

	private TestablePreferences create(final TestablePreferences parent, final String childName) throws Exception {
		final TestablePreferences node = (TestablePreferences) parent.node(childName);
		assertExists(parent, childName, node);
		return node;
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

		// preferences root
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
	public void test01ScopeRootActivation() throws Exception {
		// create preference tree
		final TestablePreferences r1 = new TestablePreferences(preferencesRoot, testablePreferenceName, service);
		assertTrue("must be active", service.isActive(r1));

		// another one must fail
		try {
			new TestablePreferences(preferencesRoot, testablePreferenceName, service);
			fail("must not be possible to create a second scope root");
		} catch (final IllegalStateException e) {
			// good
		}
	}

	@Test
	public void test02SetAndGetDataWithEvents() throws Exception {
		// create preference tree
		final TestablePreferences r1 = new TestablePreferences(preferencesRoot, testablePreferenceName, service);

		// access any data
		assertNotNull("children never null", r1.childrenNames());
		assertEquals("children must be empty", 0, r1.childrenNames().length);
		assertNotNull("keys never null", r1.keys());
		assertEquals("keys must be empty", 0, r1.keys().length);

		// hook listeners for capturing event
		final PreferenceChangeRecorder preferenceEvents = new PreferenceChangeRecorder();
		r1.addPreferenceChangeListener(preferenceEvents);
		final NodeChangeRecorder nodeEvents = new NodeChangeRecorder();
		r1.addNodeChangeListener(nodeEvents);

		// put some data
		r1.put(KEY, VALUE);
		assertEquals(VALUE, r1.get(KEY, DEFAULT_VALUE));

		// check event
		final PreferenceChangeEvent event = preferenceEvents.poll();
		assertNotNull("no event", event);
		assertSame("not the same node", r1, event.getNode());
		assertEquals("unexpected key", KEY, event.getKey());
		assertEquals("unexpected new value", VALUE, event.getNewValue());
		assertNull("unexpected old value", event.getOldValue());

		// no node events
		nodeEvents.assertEmpty();

		// zookeeper must not exists
		assertFalse("path must not exist", ZooKeeperGate.get().exists(new Path(r1.testableGetZooKeeperPath())));

		// flush
		r1.flush();

		// zookeeper should exist
		assertTrue("path must exist", ZooKeeperGate.get().exists(new Path(r1.testableGetZooKeeperPath())));

		// no events should be triggered
		preferenceEvents.assertEmpty("unexpected preference event during flush");
		nodeEvents.assertEmpty();
	}

	@Test
	public void test03CreateNodeFlushAtNode() throws Exception {
		testCreateNode(false);
	}

	@Test
	public void test03CreateNodeFlushAtParent() throws Exception {
		testCreateNode(true);
	}

	@Test
	public void test04RemoveNode() throws Exception {
		// create preference tree
		final TestablePreferences rootNode = new TestablePreferences(preferencesRoot, testablePreferenceName, service);

		// try multiple times
		for (int i = 0; i < 10; i++) {
			testRemoveNode(rootNode);
		}
	}

	@Test
	public void test05SyncNode() throws Exception {
		// create preference tree
		final TestablePreferences rootNode = new TestablePreferences(preferencesRoot, testablePreferenceName, service);

		final String testNodeName = "testNode" + System.currentTimeMillis();
		assertFalse("test node must not exists", rootNode.nodeExists(testNodeName));

		// create an empty node
		final TestablePreferences testNode = create(rootNode, testNodeName);

		// check that the path does not exists in ZooKeeper
		final IPath testNodeZkPath = new Path(rootNode.testableGetZooKeeperPath()).append(testNodeName);
		assertFalse("test node path must not exists in ZooKeeper before sync", ZooKeeperGate.get().exists(testNodeZkPath));

		// call sync
		testNode.sync();

		// node must be created at this point
		assertExists(rootNode, testNodeName, testNode);

		// path must exists as well
		assertTrue("test node path must exists in ZooKeeper after sync", ZooKeeperGate.get().exists(testNodeZkPath));
	}

	private void testCreateNode(final boolean flushUsingParent) throws Exception {
		// create preference tree
		final TestablePreferences rootNode = new TestablePreferences(preferencesRoot, testablePreferenceName, service);

		// hook listeners for capturing event
		final NodeChangeRecorder nodeEvents = new NodeChangeRecorder();
		rootNode.addNodeChangeListener(nodeEvents);

		final String testNodeName = "testNode" + System.currentTimeMillis();
		assertFalse("test node must not exists", rootNode.nodeExists(testNodeName));

		// also check that the path does not exists in ZooKeeper
		final IPath testNodeZkPath = new Path(rootNode.testableGetZooKeeperPath()).append(testNodeName);
		assertFalse("test node path must not exists in ZooKeeper after flush", ZooKeeperGate.get().exists(testNodeZkPath));

		// create an empty node
		final TestablePreferences testNode = create(rootNode, testNodeName);

		// check ZooKeeper does not exists
		assertFalse("test node path must not exists in ZooKeeper before flush", ZooKeeperGate.get().exists(new Path(testNode.testableGetZooKeeperPath())));

		// must be active
		assertTrue("must be active", service.isActive(testNode));

		// check parent and access
		assertSame("parent not the same object", rootNode, testNode.parent());
		assertSame("must return the same object", testNode, testNode.node(""));
		assertSame("must return the same object", testNode, rootNode.node(testNodeName));

		// check event
		final NodeChangeEvent event = nodeEvents.pollAdded();
		assertNotNull("no event", event);
		assertSame("not the same node", testNode, event.getChild());
		assertSame("not the same node", rootNode, event.getParent());

		// flush at level
		if (flushUsingParent) {
			assertSame("parent not the same object", rootNode, testNode.parent());
			rootNode.flush();
		} else {
			testNode.flush();
		}

		// paths must be there
		assertTrue("test node must exists after flush", rootNode.nodeExists(testNodeName));
		assertTrue("test node path must exists in ZooKeeper after flush", ZooKeeperGate.get().exists(testNodeZkPath));

		// no event
		nodeEvents.assertEmpty();
	}

	private void testRemoveNode(final TestablePreferences rootNode) throws BackingStoreException, InterruptedException, KeeperException {
		final String testNodeName = "testNode" + System.currentTimeMillis();

		// create node
		final TestablePreferences testNode = (TestablePreferences) rootNode.node(testNodeName);
		rootNode.flush();
		assertTrue("must exist", rootNode.nodeExists(testNodeName));

		// hook listeners for capturing event
		final NodeChangeRecorder nodeEvents = new NodeChangeRecorder();
		rootNode.addNodeChangeListener(nodeEvents);

		// remove node
		testNode.removeNode();

		// check existence
		assertTrue("test node must be removed", testNode.testableRemoved());
		assertFalse("test node must be removed from parent", rootNode.testableGetChildren().containsKey(testNodeName));
		assertFalse("test node must be removed from parent", rootNode.testableGetChildren().containsValue(testNode));
		assertFalse("test node must not exists anymore", testNode.nodeExists(""));
		assertFalse("test node must not exists anymore", rootNode.nodeExists(testNodeName));
		assertTrue("test node path must still exists in ZooKeeper before flush", ZooKeeperGate.get().exists(new Path(testNode.testableGetZooKeeperPath())));

		// must be in active
		assertFalse("must not be active after removal", service.isActive(testNode));

		// check event
		final NodeChangeEvent event = nodeEvents.pollRemoved();
		assertNotNull("no event", event);
		assertSame("not the same node", testNode, event.getChild());
		assertSame("not the same node", rootNode, event.getParent());

		// flush at test node must fail
		try {
			testNode.flush();
			fail("must not be able to flush on removed node");
		} catch (final Exception e) {
			// assume ok
		}

		// flush at parent
		rootNode.flush();

		// paths must be removed in ZooKeeper
		assertFalse("test node path must not exists in ZooKeeper after flush", ZooKeeperGate.get().exists(new Path(testNode.testableGetZooKeeperPath())));
	}
}
