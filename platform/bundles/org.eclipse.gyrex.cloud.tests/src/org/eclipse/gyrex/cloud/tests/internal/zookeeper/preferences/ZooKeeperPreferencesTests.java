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

import org.osgi.service.prefs.Preferences;

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
	public void testActivationOnChildren() throws Exception {
		// create preference tree
		final TestablePreferences r1 = new TestablePreferences(preferencesRoot, testablePreferenceName, service);
		assertFalse("must be inactive", service.isActive(r1));

		// accessing children must activate
		assertNotNull("children never null", r1.childrenNames());

		// must be active now
		assertTrue("must be active now", service.isActive(r1));
	}

	@Test
	public void testActivationOnExists() throws Exception {
		// create preference tree
		final TestablePreferences r1 = new TestablePreferences(preferencesRoot, testablePreferenceName, service);
		assertFalse("must be inactive", service.isActive(r1));

		// exists check must activate
		assertTrue("node does not exist?", r1.nodeExists(""));

		// must be active now
		assertTrue("must be active now", service.isActive(r1));
	}

	@Test
	public void testActivationOnFlush() throws Exception {
		// create preference tree
		final TestablePreferences r1 = new TestablePreferences(preferencesRoot, testablePreferenceName, service);
		assertFalse("must be inactive", service.isActive(r1));

		// flushing data must activate
		r1.flush();

		// must be active now
		assertTrue("must be active now", service.isActive(r1));
	}

	@Test
	public void testActivationOnGet() throws Exception {
		// create preference tree
		final TestablePreferences r1 = new TestablePreferences(preferencesRoot, testablePreferenceName, service);
		assertFalse("must be inactive", service.isActive(r1));

		// accessing data must activate
		assertEquals("unexpected data", DEFAULT_VALUE, r1.get(KEY, DEFAULT_VALUE));

		// must be active now
		assertTrue("must be active now", service.isActive(r1));
	}

	@Test
	public void testActivationOnKeys() throws Exception {
		// create preference tree
		final TestablePreferences r1 = new TestablePreferences(preferencesRoot, testablePreferenceName, service);
		assertFalse("must be inactive", service.isActive(r1));

		// accessing keys must activate
		assertNotNull("keys never null", r1.keys());

		// must be active now
		assertTrue("must be active now", service.isActive(r1));
	}

	@Test
	public void testActivationOnNodeListener() throws Exception {
		// create preference tree
		final TestablePreferences r1 = new TestablePreferences(preferencesRoot, testablePreferenceName, service);
		assertFalse("must be inactive", service.isActive(r1));

		// hook listeners for capturing event
		final NodeChangeRecorder nodeEvents = new NodeChangeRecorder();
		r1.addNodeChangeListener(nodeEvents);

		// must be active now
		assertTrue("must be active now", service.isActive(r1));
	}

	@Test
	public void testActivationOnPreferenceListener() throws Exception {
		// create preference tree
		final TestablePreferences r1 = new TestablePreferences(preferencesRoot, testablePreferenceName, service);
		assertFalse("must be inactive", service.isActive(r1));

		// hook listeners for capturing event
		final PreferenceChangeRecorder preferenceEvents = new PreferenceChangeRecorder();
		r1.addPreferenceChangeListener(preferenceEvents);

		// must be active now
		assertTrue("must be active now", service.isActive(r1));
	}

	@Test
	public void testActivationOnPut() throws Exception {
		// create preference tree
		final TestablePreferences r1 = new TestablePreferences(preferencesRoot, testablePreferenceName, service);
		assertFalse("must be inactive", service.isActive(r1));

		// putting data must activate
		r1.put(KEY, VALUE);

		// must be active now
		assertTrue("must be active now", service.isActive(r1));
	}

	@Test
	public void testActivationOnRemove() throws Exception {
		// create preference tree
		final TestablePreferences r1 = new TestablePreferences(preferencesRoot, testablePreferenceName, service);
		assertFalse("must be inactive", service.isActive(r1));

		// removing data must activate
		r1.remove(KEY);

		// must be active now
		assertTrue("must be active now", service.isActive(r1));
	}

	@Test
	public void testActivationOnSync() throws Exception {
		// create preference tree
		final TestablePreferences r1 = new TestablePreferences(preferencesRoot, testablePreferenceName, service);
		assertFalse("must be inactive", service.isActive(r1));

		// syncing data must activate
		r1.sync();

		// must be active now
		assertTrue("must be active now", service.isActive(r1));
	}

	private void testCreateNode(final boolean flushUsingParent) throws Exception {
		// create preference tree
		final TestablePreferences rootNode = new TestablePreferences(preferencesRoot, testablePreferenceName, service);
		assertFalse("must be inactive", service.isActive(rootNode));

		// hook listeners for capturing event
		final NodeChangeRecorder nodeEvents = new NodeChangeRecorder();
		rootNode.addNodeChangeListener(nodeEvents);

		final String testNodeName = "testNode" + System.currentTimeMillis();
		assertFalse("test node must not exists", rootNode.nodeExists(testNodeName));

		// also check that the path does not exists in ZooKeeper
		final IPath testNodeZkPath = new Path(rootNode.testableGetZooKeeperPath()).append(testNodeName);
		assertFalse("test node path must not exists in ZooKeeper after flush", ZooKeeperGate.get().exists(testNodeZkPath));

		// create an empty node
		final Preferences testNode = rootNode.node(testNodeName);
		assertSame("parent not the same object", rootNode, testNode.parent());

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

	@Test
	public void testCreateNodeFlushAtNode() throws Exception {
		testCreateNode(false);
	}

	@Test
	public void testCreateNodeFlushAtParent() throws Exception {
		testCreateNode(true);
	}

	@Test
	public void testSetAndGetDataWithEvents() throws Exception {
		// create preference tree
		final TestablePreferences r1 = new TestablePreferences(preferencesRoot, testablePreferenceName, service);
		assertFalse("must be inactive", service.isActive(r1));

		// access any data
		assertNotNull("children never null", r1.childrenNames());
		assertEquals("children must be empty", 0, r1.childrenNames().length);
		assertNotNull("keys never null", r1.keys());
		assertEquals("keys must be empty", 0, r1.keys().length);

		// must be active now
		assertTrue("must be active now", service.isActive(r1));

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
}
