/**
 * Copyright (c) 2009 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.context.tests.internal;

import static junit.framework.Assert.assertEquals;

import java.util.UUID;

import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.preferences.IRuntimeContextPreferences;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.DefaultScope;

import org.osgi.service.prefs.Preferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Black-box style tests for the context preferences.
 */
public class PreferencesBlackBoxTests {

	private static final Logger LOG = LoggerFactory.getLogger(PreferencesBlackBoxTests.class);

	private IServiceProxy<IRuntimeContextRegistry> contextRegistryProxy;
	private IRuntimeContextRegistry contextRegistry;

	@Before
	public void setUp() throws Exception {
		contextRegistryProxy = Activator.getActivator().getServiceHelper().trackService(IRuntimeContextRegistry.class);
		contextRegistry = contextRegistryProxy.getService();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		contextRegistry = null;
		contextRegistryProxy.dispose();
		contextRegistryProxy = null;
	}

	/**
	 * This test tests setting and retrieving preferences.
	 */
	@Test
	public void testPreferencesScenario001() throws Exception {
		// get the root context
		final IRuntimeContext rootContext = contextRegistry.get(Path.ROOT);
		final IRuntimeContextPreferences rootPrefs = rootContext.getPreferences();

		// get another context
		final IRuntimeContext parentContext = contextRegistry.get(new Path("/parent"));
		final IRuntimeContextPreferences parentPrefs = parentContext.getPreferences();

		// get another context
		final IRuntimeContext childContext = contextRegistry.get(new Path("/parent/child/child"));
		final IRuntimeContextPreferences childPrefs = childContext.getPreferences();

		// generate key + value for test
		final String key = "testStringConf" + UUID.randomUUID().toString();
		final String value = "avalue" + System.currentTimeMillis();

		// set a default ... should be available in all
		final Preferences defaultNode = new DefaultScope().getNode(Activator.SYMBOLIC_NAME);
		LOG.trace("Setting default for key {} in node {}", key, defaultNode.node(Activator.SYMBOLIC_NAME).absolutePath());
		defaultNode.node(Activator.SYMBOLIC_NAME).put(key, value);

		// verify
		assertEquals("root context default preference lookup failed", value, rootPrefs.get(Activator.SYMBOLIC_NAME, key, null));
		assertEquals("parent context default preference lookup failed", value, parentPrefs.get(Activator.SYMBOLIC_NAME, key, null));
		assertEquals("child context default preference lookup failed", value, childPrefs.get(Activator.SYMBOLIC_NAME, key, null));

		// set something on the root ... should inherit to all
		final String value2 = "root-value";
		rootPrefs.put(Activator.SYMBOLIC_NAME, key, value2, false);
		rootPrefs.sync(Activator.SYMBOLIC_NAME);
		assertEquals("root context preference storing failed", value2, rootPrefs.get(Activator.SYMBOLIC_NAME, key, null));
		assertEquals("parent context stored preference lookup failed", value2, parentPrefs.get(Activator.SYMBOLIC_NAME, key, null));
		assertEquals("child context stored preference lookup failed", value2, childPrefs.get(Activator.SYMBOLIC_NAME, key, null));

		// set it different in parent
		final String value3 = "parent-value";
		parentPrefs.put(Activator.SYMBOLIC_NAME, key, value3, false);
		assertEquals("root context stored preference failed", value2, rootPrefs.get(Activator.SYMBOLIC_NAME, key, null));
		assertEquals("parent context stored preference lookup failed", value3, parentPrefs.get(Activator.SYMBOLIC_NAME, key, null));
		assertEquals("child context stored preference lookup failed", value3, childPrefs.get(Activator.SYMBOLIC_NAME, key, null));

		// set it different in child
		final String value4 = "child-value";
		childPrefs.put(Activator.SYMBOLIC_NAME, key, value4, false);
		assertEquals("root context stored preference failed", value2, rootPrefs.get(Activator.SYMBOLIC_NAME, key, null));
		assertEquals("parent context stored preference lookup failed", value3, parentPrefs.get(Activator.SYMBOLIC_NAME, key, null));
		assertEquals("child context stored preference lookup failed", value4, childPrefs.get(Activator.SYMBOLIC_NAME, key, null));
	}
}
