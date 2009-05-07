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
import static junit.framework.Assert.assertNotNull;

import org.eclipse.core.runtime.Path;
import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.provider.ContextObjectProvider;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceRegistration;

/**
 *
 */
public class ContextualRuntimeBlackBoxTests {

	public static class DummyObject {
		final IRuntimeContext context;
		int ungetCalled = 0;

		public DummyObject(final IRuntimeContext context) {
			this.context = context;
		}

		@Override
		public String toString() {
			return "DummyObject{ " + context + " }";
		}
	}

	public static class DummyObjectProvider extends ContextObjectProvider {

		@Override
		public Object getObject(final Class type, final IRuntimeContext context) {
			if (type.equals(DummyObject.class)) {
				return new DummyObject(context);
			}
			return null;
		}

		@Override
		public Class[] getObjectTypes() {
			return new Class<?>[] { DummyObject.class };
		}

		@Override
		public void ungetObject(final Object object, final IRuntimeContext context) {
			if (object instanceof DummyObject) {
				((DummyObject) object).ungetCalled++;
			}
		}
	}

	static void safeUnregister(final ServiceRegistration serviceRegistration) {
		if (null != serviceRegistration) {
			try {
				serviceRegistration.unregister();
			} catch (final IllegalStateException e) {
				// ignored
			}
		}
	}

	private IServiceProxy<IRuntimeContextRegistry> contextRegistryProxy;
	private IRuntimeContextRegistry contextRegistry;

	/**
	 * @throws java.lang.Exception
	 */
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
	 * This test tests registration of a contextual object provider and
	 * retrieval without any configuration. In this case, the object should be
	 * available because it's available in the root context automatically.
	 */
	@Test
	public void testContextualObjectScenarioOne() {
		// register our provider
		ServiceRegistration serviceRegistration = Activator.getActivator().getServiceHelper().registerService(ContextObjectProvider.class.getName(), new DummyObjectProvider(), "Eclipse.org Gyrex", "Dummy object provider.", null, null);

		try {
			final IRuntimeContext rootContext = contextRegistry.get(Path.ROOT);

			final DummyObject dummyObject = rootContext.get(DummyObject.class);
			assertNotNull("The dummy object from the root context must not be null!", dummyObject);
			assertEquals("The context of the dummy object does not match!", rootContext, dummyObject.context);

			final IRuntimeContext testContext = contextRegistry.get(new Path("/some/other/context"));
			final DummyObject dummyObject2 = testContext.get(DummyObject.class);
			assertNotNull("The dummy object from the test context must not be null!", dummyObject2);
			assertEquals("The context of the dummy object from the test context does not match!", testContext, dummyObject2.context);

			// now remove the provider
			serviceRegistration.unregister();
			serviceRegistration = null;

			// verify that the objects were properly destroyed
			assertEquals("Dummy object from the root context not properly destoryed after unregister!", 1, dummyObject.ungetCalled);
			assertEquals("Dummy object from the test context not properly destoryed after unregister!", 1, dummyObject2.ungetCalled);
		} finally {
			// cleanup
			safeUnregister(serviceRegistration);
		}
	}

	/**
	 * This test tests registration of a contextual object provider and
	 * retrieval without any configuration. In this case, the object should be
	 * available because it's available in the root context automatically.
	 */
	@Test
	public void testContextualObjectScenarioTwo() {
		// register our provider
		ServiceRegistration serviceRegistration = null;
		ServiceRegistration serviceRegistration2 = null;

		try {
			serviceRegistration = Activator.getActivator().getServiceHelper().registerService(ContextObjectProvider.class.getName(), new DummyObjectProvider(), "Eclipse.org Gyrex", "Dummy object provider.", null, null);

			final IRuntimeContext rootContext = contextRegistry.get(Path.ROOT);

			final DummyObject dummyObject = rootContext.get(DummyObject.class);
			assertNotNull("The dummy object from the root context must not be null!", dummyObject);
			assertEquals("The context of the dummy object does not match!", rootContext, dummyObject.context);

			final IRuntimeContext testContext = contextRegistry.get(new Path("/some/other/context"));
			final DummyObject dummyObject2 = testContext.get(DummyObject.class);
			assertNotNull("The dummy object from the test context must not be null!", dummyObject2);
			assertEquals("The context of the dummy object from the test context does not match!", testContext, dummyObject2.context);

			// now register a second provider instance ... this should destroy the provided objects
			serviceRegistration2 = Activator.getActivator().getServiceHelper().registerService(ContextObjectProvider.class.getName(), new DummyObjectProvider(), "Eclipse.org Gyrex", "Dummy object provider 2.", null, null);

			// verify that the objects were properly destroyed
			assertEquals("Dummy object from the root context not properly destoryed after additional registration!", 1, dummyObject.ungetCalled);
			assertEquals("Dummy object from the test context not properly destoryed after additional registration!", 1, dummyObject2.ungetCalled);

			// cleanup
			serviceRegistration.unregister();
			serviceRegistration = null;
			serviceRegistration2.unregister();
			serviceRegistration2 = null;

			// at this point the should not be destroyed again
			assertEquals("Dummy object from the root context destory multiple times!", 1, dummyObject.ungetCalled);
			assertEquals("Dummy object from the test context destory multiple times!", 1, dummyObject2.ungetCalled);
		} finally {
			safeUnregister(serviceRegistration);
			safeUnregister(serviceRegistration2);
		}
	}

}
