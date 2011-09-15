/**
 * Copyright (c) 2009, 2011 AGETO Service GmbH and others.
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

import static junit.framework.Assert.assertNotNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.gyrex.cloud.environment.INodeEnvironment;
import org.eclipse.gyrex.context.IRuntimeContext;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.junit.Test;

/**
 *
 */
public class ContextInjectionTests extends BaseContextTest {

	public static class DummyObject {
		final IRuntimeContext context;

		@Inject
		public DummyObject(final IRuntimeContext context) {
			this.context = context;
		}

		@Override
		public String toString() {
			return "DummyObject{ " + context + " }";
		}
	}

	@Singleton
	public static class DummyObjectWithOsgiService {
		final INodeEnvironment environment;

		@Inject
		public DummyObjectWithOsgiService(final INodeEnvironment environment) {
			this.environment = environment;
		}

		@Override
		public String toString() {
			return "DummyObjectWithOsgiService{ " + environment + " }";
		}
	}

	@Override
	protected IPath getPrimaryTestContextPath() {
		return Path.ROOT;
	}

	/**
	 * This test tests registration of a contextual object provider and
	 * retrieval with a particular configuration. It also tests update of the
	 * configuration and relies on service ranking to work properly.
	 */
	@Test
	public void testMakeObject() throws Exception {
		final DummyObject object = getContext().getInjector().make(DummyObject.class);
		assertNotNull("no object created", object);
		assertNotNull("object has no context set", object.context);

		final DummyObjectWithOsgiService object2 = getContext().getInjector().make(DummyObjectWithOsgiService.class);
		assertNotNull("object with osgi service not created", object2);
		assertNotNull("object has no osgi service set", object2.environment);
	}
}
