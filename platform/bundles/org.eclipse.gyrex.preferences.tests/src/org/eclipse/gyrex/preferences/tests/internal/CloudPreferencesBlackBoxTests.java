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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;

import org.eclipse.gyrex.preferences.CloudScope;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class CloudPreferencesBlackBoxTests {

	private static final String PATH_5 = "tests/with/some/deep/path";
	private static final String PATH_1 = "tests";
	private static final String VALUE_1 = "test";
	private static final String KEY_1 = "key_1";

	private void remove(final IEclipsePreferences node) throws BackingStoreException {
		final Preferences parent = node.parent();
		node.removeNode();
		parent.flush();

		assertFalse("node removal failed", node.nodeExists(""));
		assertFalse("node removal failed at parent", parent.nodeExists(PATH_1));

		try {
			node.get(KEY_1, null);
			fail("node method get should have thrown exception");
		} catch (final IllegalStateException e) {
			// expected
		}
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test001() throws Exception {
		final IEclipsePreferences node = CloudScope.INSTANCE.getNode(PATH_1);
		node.put(KEY_1, VALUE_1);

		node.flush();

		assertEquals("preference value lost on persisted", VALUE_1, node.get(KEY_1, null));

		node.sync();

		assertEquals("preference value lost on sync", VALUE_1, node.get(KEY_1, null));

		// cleanup
		remove(node);
	}

	@Test
	public void test002() throws Exception {
		final IEclipsePreferences node = CloudScope.INSTANCE.getNode(PATH_1 + "/" + PATH_5);
		node.put(KEY_1, VALUE_1);

		CloudScope.INSTANCE.getNode(PATH_1).flush();

		assertEquals("preference value lost on persisted", VALUE_1, node.get(KEY_1, null));

		CloudScope.INSTANCE.getNode(PATH_1).sync();

		assertEquals("preference value lost on sync", VALUE_1, node.get(KEY_1, null));

		// cleanup
		remove(CloudScope.INSTANCE.getNode(PATH_1));
	}

}
