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
package org.eclipse.gyrex.context.tests.internal;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.Collection;

import org.eclipse.gyrex.context.internal.ContextActivator;
import org.eclipse.gyrex.context.internal.GyrexContextHandle;
import org.eclipse.gyrex.context.internal.registry.ContextDefinition;
import org.eclipse.gyrex.context.internal.registry.ContextRegistryImpl;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class ContextRegistryTests {

	private ContextRegistryImpl contextRegistry;

	private void assertDefined(final IPath path) {
		final GyrexContextHandle handle = contextRegistry.get(path);
		if (null == handle) {
			fail(String.format("context %s was added but no context returned!", path.toString()));
		}
	}

	private void defineContext(final IPath path) {
		if (null == contextRegistry.getDefinition(path)) {
			final ContextDefinition definition = new ContextDefinition(path);
			definition.setName(path.toPortableString());
			contextRegistry.saveDefinition(definition);
		} else {
			fail(String.format("Context %s already exists!", path));
		}
	}

	@Before
	public void setUp() throws Exception {
		contextRegistry = ContextActivator.getInstance().getContextRegistryImpl();
	}

	@After
	public void tearDown() throws Exception {
		contextRegistry = null;
	}

	@Test
	public void test001_DefinedContexts() {
		// test removal
		testRemoval();

		// now add a few contexts
		final String prefix = String.valueOf(System.currentTimeMillis());
		final int contextsToAdd = 10;
		for (int i = 0; i < contextsToAdd; i++) {
			final IPath path = new Path("/test").append(prefix).append(String.valueOf(i));
			defineContext(path);
			assertDefined(path);
		}

		// also try some hierarchy levels
		final IPath base = new Path("/some/level/more");

		final IPath p1 = base.append("t1").append("l1");
		final IPath p2 = base.append("t2").append("l1");
		final IPath p3 = base.append("t2").append("l1").append("b3");
		final IPath p4 = base.append("t1").append("l2");
		final IPath p5 = base.append("t2").append("l2");
		final IPath p6 = base.append("t2").append("l2").append("b3");

		defineContext(p1);
		defineContext(p2);
		defineContext(p3);
		defineContext(p4);
		defineContext(p5);
		defineContext(p6);

		assertDefined(p1);
		assertDefined(p2);
		assertDefined(p3);
		assertDefined(p4);
		assertDefined(p5);
		assertDefined(p6);

		// test removal
		testRemoval();
	}

	private void testRemoval() {
		Collection<ContextDefinition> contexts = contextRegistry.getDefinedContexts();
		assertNotNull(contexts);

		// should never be empty
		assertFalse("root contexts must always be defined", contexts.isEmpty());

		// remove all contexts
		if (contexts.size() > 1) {
			for (final ContextDefinition contextDefinition : contexts) {
				final IPath path = contextDefinition.getPath();
				if (!path.isRoot()) {
					contextRegistry.removeDefinition(contextDefinition);
					final ContextDefinition definition = contextRegistry.getDefinition(path);
					if (null != definition) {
						fail(String.format("context '%s' still exists after removal", path.toString()));
					}
				}
			}

			// must now be empty (well, only root)
			contexts = contextRegistry.getDefinedContexts();
			assertNotNull(contexts);
			assertTrue("wrong size, only root context should be in there", contexts.size() == 1);
			assertTrue("only root context should be in there", contexts.iterator().next().getPath().isRoot());
		}
	}

}
