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

import org.eclipse.gyrex.preferences.CloudScope;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class CloudPreferencesBlackBoxTests {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetNode() throws Exception {
		final CloudScope cloudScope = new CloudScope();

		final IEclipsePreferences node = cloudScope.getNode("tests");
		node.put("test2", "test");
		node.flush();

		assertEquals("preference value not persisted", "test", cloudScope.getNode("tests").get("test2", null));
	}

}
