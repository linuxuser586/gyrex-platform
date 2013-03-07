/**
 * Copyright (c) 2009, 2012 AGETO Service GmbH and others.
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
import static junit.framework.Assert.assertNotNull;

import java.util.UUID;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.definitions.ContextDefinition;
import org.eclipse.gyrex.context.internal.ContextActivator;
import org.eclipse.gyrex.context.internal.GyrexContextHandle;
import org.eclipse.gyrex.context.internal.registry.ContextRegistryImpl;
import org.eclipse.gyrex.context.preferences.IRuntimeContextPreferences;

import org.eclipse.core.runtime.IPath;
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

	private ContextRegistryImpl contextRegistry;

	private GyrexContextHandle assertContextDefined(final IPath path) {
		final ContextDefinition definition = new ContextDefinition(path);
		definition.setName("Test Context");
		contextRegistry.saveDefinition(definition);
		assertNotNull("context definition must exists after create", contextRegistry.get(path));
		final GyrexContextHandle context = contextRegistry.get(path);
		assertNotNull("context handle must exists", context);
		assertNotNull("context handle must map to real context", context.get());
		return context;
	}

	@Before
	public void setUp() throws Exception {
		contextRegistry = ContextActivator.getInstance().getContextRegistryImpl();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		contextRegistry = null;
	}

	/**
	 * This test tests setting and retrieving preferences.
	 */
	@Test
	public void testPreferencesScenario001() throws Exception {
		// get the root context
		final IRuntimeContext rootContext = contextRegistry.get(Path.ROOT);
		assertNotNull("nmissing context " + Path.ROOT, rootContext);
		final IRuntimeContextPreferences rootPrefs = rootContext.getPreferences();
		assertNotNull("no preferences for context " + rootContext, rootPrefs);

		// get another context
		final IRuntimeContext parentContext = assertContextDefined(new Path("/parent"));
		final IRuntimeContextPreferences parentPrefs = parentContext.getPreferences();

		// get another context
		final IRuntimeContext childContext = assertContextDefined(new Path("/parent/child/child"));
		final IRuntimeContextPreferences childPrefs = childContext.getPreferences();

		// generate key + value for test
		final String key = "testStringConf" + UUID.randomUUID().toString();
		final String value = "avalue" + System.currentTimeMillis();

		// set a default ... should be available in all
		final Preferences defaultNode = DefaultScope.INSTANCE.getNode(Activator.SYMBOLIC_NAME);
		LOG.trace("Setting default for key {} in node {}", key, defaultNode.node(Activator.SYMBOLIC_NAME).absolutePath());
		defaultNode.put(key, value);

		// verify
		assertEquals("root context default preference lookup failed", value, rootPrefs.get(Activator.SYMBOLIC_NAME, key, null));
		assertEquals("parent context default preference lookup failed", value, parentPrefs.get(Activator.SYMBOLIC_NAME, key, null));
		assertEquals("child context default preference lookup failed", value, childPrefs.get(Activator.SYMBOLIC_NAME, key, null));

		// set something on the root ... should inherit to all
		final String ROOT_VALUE = "root-value";
		rootPrefs.put(Activator.SYMBOLIC_NAME, key, ROOT_VALUE, false);
		rootPrefs.flush(Activator.SYMBOLIC_NAME);
		rootPrefs.sync(Activator.SYMBOLIC_NAME);
		assertEquals("root context preference storing failed", ROOT_VALUE, rootPrefs.get(Activator.SYMBOLIC_NAME, key, null));
		assertEquals("parent context stored preference lookup failed", ROOT_VALUE, parentPrefs.get(Activator.SYMBOLIC_NAME, key, null));
		assertEquals("child context stored preference lookup failed", ROOT_VALUE, childPrefs.get(Activator.SYMBOLIC_NAME, key, null));

		// set it different in parent
		final String value3 = "parent-value";
		parentPrefs.put(Activator.SYMBOLIC_NAME, key, value3, false);
		parentPrefs.flush(Activator.SYMBOLIC_NAME);
		parentPrefs.sync(Activator.SYMBOLIC_NAME);
		assertEquals("root context stored preference failed", ROOT_VALUE, rootPrefs.get(Activator.SYMBOLIC_NAME, key, null));
		assertEquals("parent context stored preference lookup failed", value3, parentPrefs.get(Activator.SYMBOLIC_NAME, key, null));
		assertEquals("child context stored preference lookup failed", value3, childPrefs.get(Activator.SYMBOLIC_NAME, key, null));

		// set it different in child
		final String value4 = "child-value";
		childPrefs.put(Activator.SYMBOLIC_NAME, key, value4, false);
		childPrefs.flush(Activator.SYMBOLIC_NAME);
		childPrefs.sync(Activator.SYMBOLIC_NAME);
		assertEquals("root context stored preference failed", ROOT_VALUE, rootPrefs.get(Activator.SYMBOLIC_NAME, key, null));
		assertEquals("parent context stored preference lookup failed", value3, parentPrefs.get(Activator.SYMBOLIC_NAME, key, null));
		assertEquals("child context stored preference lookup failed", value4, childPrefs.get(Activator.SYMBOLIC_NAME, key, null));
	}
}
