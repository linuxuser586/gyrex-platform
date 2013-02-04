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

import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.eclipse.gyrex.cloud.internal.CloudDebug;
import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.tests.internal.CloudTestsActivator;
import org.eclipse.gyrex.cloud.tests.internal.zookeeper.BaseEnsambleTest;
import org.eclipse.gyrex.cloud.tests.internal.zookeeper.EnsembleHelper;
import org.eclipse.gyrex.cloud.tests.internal.zookeeper.ZooKeeperEnsambleTestSuite;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IPreferencesService;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This is not a real test but must be called from inside
 * {@link ZooKeeperEnsambleTestSuite}
 */
public class ZooKeeperPreferencesEnsambleTests extends BaseEnsambleTest {

	protected static final int TIMEOUT = 10000;

	protected static final String KEY = "key";
	protected static final String DEFAULT_VALUE = "default-value";
	protected static final String VALUE = "value";

	private final String testablePreferenceName = "test";
	private IEclipsePreferences preferencesRoot;

	private TestablePreferencesService s1;
	private TestablePreferences r1;
	private TestablePreferencesService s2;
	private TestablePreferences r2;

	private String createNode() throws BackingStoreException, InterruptedException {
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
		assertEquals("children must be consistent", r1.testableGetChildren().keySet(), r2.testableGetChildren().keySet());

		// also compare the children version
		r1.assertChildrenVersionEquals(r2);

		return testNodeName;
	}

	private String randomPath() {
		IPath path = Path.EMPTY;
		final int segments = RandomUtils.nextInt(16);
		for (int i = 0; i < segments; i++) {
			path = path.append(StringUtils.substring(DigestUtils.shaHex(UUID.randomUUID().toString()), 0, 6));
		}
		return path.toString();
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		EnsembleHelper.assertRunningAndConnected();

		// enable debugging
		CloudDebug.debug = true;
		CloudDebug.zooKeeperPreferences = true;
		CloudDebug.zooKeeperPreferencesSync = true;

		// remove any existing data in ZooKeeper
		ZooKeeperGate.get().deletePath(IZooKeeperLayout.PATH_PREFERENCES_ROOT.append(testablePreferenceName));

		preferencesRoot = CloudTestsActivator.getInstance().getService(IPreferencesService.class).getRootNode();

		s1 = new TestablePreferencesService("s1");
		assertTrue("must be connected", s1.isConnected());
		r1 = new TestablePreferences(preferencesRoot, testablePreferenceName, s1);
		assertTrue("must be active", s1.isActive(r1));

		s2 = new TestablePreferencesService("s2");
		assertTrue("must be connected", s2.isConnected());
		r2 = new TestablePreferences(preferencesRoot, testablePreferenceName, s2);
		assertTrue("must be active", s2.isActive(r2));
	}

	private void setValue(final TestablePreferences local, final TestablePreferences remote) throws InterruptedException, TimeoutException, BackingStoreException {
		final PreferenceChangeRecorder recorderLocal = new PreferenceChangeRecorder();
		local.addPreferenceChangeListener(recorderLocal);
		final PreferenceChangeRecorder recorderRemote = new PreferenceChangeRecorder();
		remote.addPreferenceChangeListener(recorderRemote);

		// read old value
		final String oldLocal = local.get(KEY, null);
		final String oldRemote = remote.get(KEY, null);
		assertEquals("current values don't match; inconsistent state", oldLocal, oldRemote);

		// set a value in source
		final String value = VALUE + System.currentTimeMillis();
		local.put(KEY, value);

		// no event should fire in target
		assertNotNull("setting a value in local should fire an event in local", recorderLocal.peek());
		assertNull("setting a value in local should not fire an event in remote", recorderRemote.peek());

		// flush
		local.flush();

		// wait for event and verify
		final PreferenceChangeEvent r2Event = recorderRemote.poll(TIMEOUT);
		assertNotNull("flushing a local node should fire an event in remote after setting a value", r2Event);
		assertSame("not the same node", remote, r2Event.getNode());
		assertEquals("unexpected key", KEY, r2Event.getKey());
		assertEquals("unexpected new value", value, r2Event.getNewValue());
		assertEquals("unexpected old value", oldRemote, r2Event.getOldValue());

		// compare properties
		assertEquals("properties version must be consistent", local.testableGetPropertiesVersion(), remote.testableGetPropertiesVersion());
		assertEquals("properties must be consistent", local.testableGetProperties(), remote.testableGetProperties());
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
	public void test01SetValueRandomPath() throws Exception {
		for (int i = 0; i < 20; i++) {
			// vary source and target
			TestablePreferences source, target;
			if ((i % 2) == 0) {
				source = r1;
				target = r2;
			} else {
				source = r2;
				target = r1;
			}

			// randomize path (but we must use the same for source and target)
			final String path = randomPath();
			source = (TestablePreferences) source.node(path);
			target = (TestablePreferences) target.node(path);

			setValue(source, target);
		}
	}

	@Test
	public void test01SetValueSamePath() throws Exception {
		// get path
		final String path = randomPath();

		// spin loop
		for (int i = 0; i < 30; i++) {
			// vary source and target
			TestablePreferences source, target;
			if ((i % 2) == 0) {
				source = r1;
				target = r2;
			} else {
				source = r2;
				target = r1;
			}

			source = (TestablePreferences) source.node(path);
			target = (TestablePreferences) target.node(path);

			setValue(source, target);
		}
	}

	@Test
	public void test02RemoveValue() throws Exception {
		// set value first
		setValue(r1, r2);
		assertNotNull("must be executed after setValue", r2.get(KEY, null));

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

		// compare properties
		assertEquals("properties version must be consistent", r1.testableGetPropertiesVersion(), r2.testableGetPropertiesVersion());
		assertEquals("properties must be consistent", r1.testableGetProperties(), r2.testableGetProperties());
	}

	@Test
	public void test03CreateNode() throws Exception {
		// we execute this test multiple times in order to detect any timing issues
		for (int i = 0; i < 10; i++) {
			createNode();
		}
	}

	@Test
	public void test04RemoveNode() throws Exception {
		// create node first
		final String testNodeName = createNode();

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
		assertEquals("children must be consistent", r1.testableGetChildren().keySet(), r2.testableGetChildren().keySet());

		// compare children versions
		r1.assertChildrenVersionEquals(r2);
	}
}
