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
package org.eclipse.gyrex.cloud.tests.internal.zookeeper.preferences;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;

import org.eclipse.gyrex.cloud.internal.CloudDebug;
import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.tests.internal.CloudTestsActivator;
import org.eclipse.gyrex.cloud.tests.internal.zookeeper.BaseEnsambleTest;
import org.eclipse.gyrex.cloud.tests.internal.zookeeper.EnsembleHelper;
import org.eclipse.gyrex.cloud.tests.internal.zookeeper.ZooKeeperEnsambleTestSuite;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IPreferencesService;

import org.osgi.service.prefs.Preferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This is not a real test but must be called fomr insie
 * {@link ZooKeeperEnsambleTestSuite}
 */
public class ZooKeeperPreferencesEnsambleTests extends BaseEnsambleTest {

	protected static final int TIMEOUT = 5000;

	protected static final String KEY = "key";
	protected static final String DEFAULT_VALUE = "default-value";
	protected static final String VALUE = "value";

	private final String testablePreferenceName = "test";
	private IEclipsePreferences preferencesRoot;

	private TestablePreferencesService s1;
	private TestablePreferences r1;
	private TestablePreferencesService s2;
	private TestablePreferences r2;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		EnsembleHelper.assertRunningAndConnected();

		// enable debugging
		CloudDebug.debug = true;
		CloudDebug.zooKeeperPreferences = true;

		// remove any existing data in ZooKeeper
		ZooKeeperGate.get().deletePath(IZooKeeperLayout.PATH_PREFERENCES_ROOT.append(testablePreferenceName));

		preferencesRoot = CloudTestsActivator.getInstance().getService(IPreferencesService.class).getRootNode();

		s1 = new TestablePreferencesService("s1");
		assertTrue("must be connected", s1.isConnected());
		r1 = new TestablePreferences(preferencesRoot, testablePreferenceName, s1);
		assertFalse("must be inactive", s1.isActive(r1));

		s2 = new TestablePreferencesService("s2");
		assertTrue("must be connected", s2.isConnected());
		r2 = new TestablePreferences(preferencesRoot, testablePreferenceName, s2);
		assertFalse("must be inactive", s2.isActive(r2));
	}

	@Override
	@After
	public void tearDown() throws Exception {
		if (null != s1) {
			s1.shutdown();
		}
		if (null != s2) {
			s2.shutdown();
		}

		super.tearDown();
	}

	@Test
	public void test01SetValue() throws Exception {
		final PreferenceChangeRecorder recorder1 = new PreferenceChangeRecorder();
		r1.addPreferenceChangeListener(recorder1);
		final PreferenceChangeRecorder recorder2 = new PreferenceChangeRecorder();
		r2.addPreferenceChangeListener(recorder2);

		// set a value in r1
		r1.put(KEY, VALUE);

		// no event should fire in r2
		assertNotNull("removing a value in tree 1 should fire an event in tree 1", recorder1.peek());
		assertNull("setting a value in tree 1 should not fire an event in tree 2", recorder2.poll(TIMEOUT));

		// flush
		r1.flush();

		// wait for event and verify
		final PreferenceChangeEvent r2Event = recorder2.poll(TIMEOUT);
		assertNotNull("flushing a node should fire an event in tree 2 after setting a value", r2Event);
		assertSame("not the same node", r2, r2Event.getNode());
		assertEquals("unexpected key", KEY, r2Event.getKey());
		assertEquals("unexpected new value", VALUE, r2Event.getNewValue());
		assertNull("unexpected old value", r2Event.getOldValue());

		// compare properties
		assertEquals("properties version must be consistent", r1.testableGetPropertiesVersion(), r2.testableGetPropertiesVersion());
		assertEquals("properties must be consistent", r1.testableGetProperties(), r2.testableGetProperties());

	}

	@Test
	public void test02RemoveValue() throws Exception {
		// set value first
		test01SetValue();
		assertNotNull("must be executed after test01SetValue", r2.get(KEY, null));

		final PreferenceChangeRecorder recorder1 = new PreferenceChangeRecorder();
		r1.addPreferenceChangeListener(recorder1);
		final PreferenceChangeRecorder recorder2 = new PreferenceChangeRecorder();
		r2.addPreferenceChangeListener(recorder2);

		// remove in r2
		r2.remove(KEY);

		// no event should fire in r1
		assertNotNull("removing a value in tree 2 should fire an event in tree 2", recorder2.peek());
		assertNull("removing a value in tree 2 should not fire an event in tree 1", recorder1.poll(TIMEOUT));

		// flush
		r2.flush();

		// wait for event and verify
		final PreferenceChangeEvent r1Event = recorder1.poll(TIMEOUT);
		assertNotNull("flushing a node should fire an event in tree 1 after removing a value in tree 2", r1Event);
		assertSame("not the same node", r1, r1Event.getNode());
		assertEquals("unexpected key", KEY, r1Event.getKey());
		assertNull("unexpected new value", r1Event.getNewValue());
		assertEquals("unexpected old value", VALUE, r1Event.getOldValue());

		// compare properties
		assertEquals("properties version must be consistent", r1.testableGetPropertiesVersion(), r2.testableGetPropertiesVersion());
		assertEquals("properties must be consistent", r1.testableGetProperties(), r2.testableGetProperties());
	}

	@Test
	public void test03CreateNode() throws Exception {
		final NodeChangeRecorder recorder1 = new NodeChangeRecorder();
		r1.addNodeChangeListener(recorder1);
		final NodeChangeRecorder recorder2 = new NodeChangeRecorder();
		r2.addNodeChangeListener(recorder2);

		final String testNodeName = "testNode" + System.currentTimeMillis();
		assertFalse("test node must not exists in tree 1", r1.nodeExists(testNodeName));
		assertFalse("test node must not exists in tree 2", r2.nodeExists(testNodeName));

		recorder1.assertEmpty();
		recorder2.assertEmpty();

		// create node in r1
		final Preferences testNode = r1.node(testNodeName);
		assertTrue("test node must exist in tree 1", r1.nodeExists(testNodeName));
		assertFalse("test node must not exist in tree 2", r2.nodeExists(testNodeName));

		final NodeChangeEvent added = recorder1.pollAdded();
		assertNotNull("no local added event", added);
		assertSame("not the same child node", testNode, added.getChild());
		assertSame("not the same parent node", r1, added.getParent());

		// no event should fire in r2
		recorder2.assertEmpty();

		// flush at root level
		assertSame("parent not the same object", r1, testNode.parent());
		r1.flush();

		// wait for event and verify
		final NodeChangeEvent r2Added = recorder2.pollAdded(TIMEOUT);
		assertNotNull("flushing a node should fire an event in tree 2", r2Added);
		assertSame("not the same node", r2, r2Added.getParent());

		// check that node exists
		assertTrue("test node must exist in tree 1", r1.nodeExists(testNodeName));
		assertTrue("test node must exist in tree 2", r2.nodeExists(testNodeName));

		// compare children (note, we cannot compare real node instances)
		assertEquals("children version must be consistent", r1.testableGetChildrenVersion(), r2.testableGetChildrenVersion());
		assertEquals("children must be consistent", r1.testableGetChildren().keySet(), r2.testableGetChildren().keySet());
	}

	@Test
	public void test04RemoveNode() throws Exception {
		// create node first
		test03CreateNode();
		assertEquals("must be executed after test03CreateNode", 1, r1.childrenNames().length);
		final String testNodeName = r1.childrenNames()[0];

		assertTrue("test node must exist in tree 1", r1.nodeExists(testNodeName));
		assertTrue("test node must exist in tree 2", r2.nodeExists(testNodeName));
		final Preferences r1Child = r1.node(testNodeName);
		final Preferences r2Child = r2.node(testNodeName);

		final NodeChangeRecorder recorder1 = new NodeChangeRecorder();
		r1.addNodeChangeListener(recorder1);
		final NodeChangeRecorder recorder2 = new NodeChangeRecorder();
		r2.addNodeChangeListener(recorder2);

		// remove node in r2
		r2Child.removeNode();

		// check exists
		assertFalse("test node must not exist", r2Child.nodeExists(""));
		assertFalse("test node must not exist in tree 2", r2.nodeExists(testNodeName));
		assertTrue("test node must exist in tree 1", r1.nodeExists(testNodeName));

		final NodeChangeEvent removed = recorder2.pollRemoved();
		assertNotNull("no local added event", removed);
		assertSame("not the same child node", r2Child, removed.getChild());
		assertSame("not the same parent node", r2, removed.getParent());

		// no event should fire in r1
		recorder1.assertEmpty();

		// flush at root level
		r2.flush();

		// wait for event and verify
		final NodeChangeEvent r1Remove = recorder1.pollRemoved(TIMEOUT);
		assertNotNull("flushing a node should fire an event in tree 1", r1Remove);
		assertSame("not the same node", r1, r1Remove.getParent());

		// check that node exists
		assertFalse("test node not must exist in tree 1", r1.nodeExists(testNodeName));
		assertFalse("test node not must exist in tree 2", r2.nodeExists(testNodeName));
		assertFalse("r1Child must not exist", r1Child.nodeExists(""));
		assertFalse("r2Child must not exist", r2Child.nodeExists(""));

		// compare children (note, we cannot compare real node instances)
		assertEquals("children version must be consistent", r1.testableGetChildrenVersion(), r2.testableGetChildrenVersion());
		assertEquals("children must be consistent", r1.testableGetChildren().keySet(), r2.testableGetChildren().keySet());
	}
}
