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
package org.eclipse.gyrex.persistence.tests.internal;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import org.eclipse.gyrex.persistence.internal.storage.RepositoryRegistry;
import org.eclipse.gyrex.persistence.storage.registry.IRepositoryDefinition;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class RepositoryRegistryTests {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testDefineRepository() {
		final RepositoryRegistry registry = new RepositoryRegistry();
		registry.createRepository("test", "blah");
		final IRepositoryDefinition definition = registry.getRepositoryDefinition("test");
		assertNotNull(definition);
		assertEquals("test", definition.getRepositoryId());
		assertEquals("blah", definition.getProviderId());
	}

}
