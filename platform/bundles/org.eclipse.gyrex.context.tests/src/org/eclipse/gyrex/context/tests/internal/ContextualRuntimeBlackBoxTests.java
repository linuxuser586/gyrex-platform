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
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import org.eclipse.gyrex.common.services.IServiceProxy;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.internal.ContextActivator;
import org.eclipse.gyrex.context.internal.GyrexContextHandle;
import org.eclipse.gyrex.context.internal.registry.ContextDefinition;
import org.eclipse.gyrex.context.internal.registry.ContextRegistryImpl;
import org.eclipse.gyrex.context.manager.IRuntimeContextManager;
import org.eclipse.gyrex.context.provider.RuntimeContextObjectProvider;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceRegistration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

	public static class DummyObject2 extends DummyObject {

		public DummyObject2(final IRuntimeContext context) {
			super(context);
		}

		@Override
		public String toString() {
			return "DummyObject2{ " + context + " }";
		}
	}

	public static class DummyObjectProvider extends RuntimeContextObjectProvider {

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

	public static class DummyObjectProvider2 extends DummyObjectProvider {

		@Override
		public Object getObject(final Class type, final IRuntimeContext context) {
			if (type.equals(DummyObject.class)) {
				return new DummyObject2(context);
			}
			return null;
		}
	}

	/** ORG_ECLIPSE_GYREX_CONTEXT_TESTS_DUMMY2 */
	private static final String SERVICE_PID_DUMMY2 = "org.eclipse.gyrex.context.tests.dummy2";

	/** CONTEXT_PATH */
	private static final Path SOME_CONTEXT_PATH = new Path("/some/other/context");

	static void safeUnregister(final ServiceRegistration serviceRegistration) {
		if (null != serviceRegistration) {
			try {
				serviceRegistration.unregister();
			} catch (final IllegalStateException e) {
				// ignored
			}
		}
	}

	private ContextRegistryImpl contextRegistry;
	private IServiceProxy<IRuntimeContextManager> contextManagerProxy;
	private IRuntimeContextManager contextManager;

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

		contextManagerProxy = Activator.getActivator().getServiceHelper().trackService(IRuntimeContextManager.class);
		contextManager = contextManagerProxy.getService();
	}

	@After
	public void tearDown() throws Exception {
		contextRegistry = null;

		contextManager = null;
		contextManagerProxy.dispose();
		contextManagerProxy = null;
	}

	@Test
	public void test0001_ContextualDefinition() {
		final IRuntimeContext rootContext = contextRegistry.get(Path.ROOT);
		assertNotNull("root context may never be null", rootContext);

		final GyrexContextHandle testContext = contextRegistry.get(SOME_CONTEXT_PATH);
		if (null != testContext) {
			testRemove(testContext);
		}

		assertContextDefined(SOME_CONTEXT_PATH);
	}

	/**
	 * This test tests registration of a contextual object provider and
	 * retrieval without any configuration. In this case, the object should be
	 * available because it's available in the root context automatically.
	 */
	@Test
	public void testContextualObjectScenario001() {
		// register our provider
		ServiceRegistration serviceRegistration = Activator.getActivator().getServiceHelper().registerService(RuntimeContextObjectProvider.class.getName(), new DummyObjectProvider(), "Eclipse.org Gyrex", "Dummy object provider.", null, null);

		try {
			final IRuntimeContext rootContext = contextRegistry.get(Path.ROOT);
			assertNotNull("root context may never be null", rootContext);

			final DummyObject dummyObject = rootContext.get(DummyObject.class);
			assertNotNull("The dummy object from the root context must not be null!", dummyObject);
			assertEquals("The context of the dummy object does not match!", rootContext, dummyObject.context);

			final IRuntimeContext testContext = assertContextDefined(SOME_CONTEXT_PATH);
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
	public void testContextualObjectScenario002() {
		// register our provider
		ServiceRegistration serviceRegistration = null;
		ServiceRegistration serviceRegistration2 = null;

		try {
			serviceRegistration = Activator.getActivator().getServiceHelper().registerService(RuntimeContextObjectProvider.class.getName(), new DummyObjectProvider(), "Eclipse.org Gyrex", "Dummy object provider.", null, null);

			final IRuntimeContext rootContext = contextRegistry.get(Path.ROOT);

			final DummyObject dummyObject = rootContext.get(DummyObject.class);
			assertNotNull("The dummy object from the root context must not be null!", dummyObject);
			assertEquals("The context of the dummy object does not match!", rootContext, dummyObject.context);

			final IRuntimeContext testContext = assertContextDefined(SOME_CONTEXT_PATH);
			final DummyObject dummyObject2 = testContext.get(DummyObject.class);
			assertNotNull("The dummy object from the test context must not be null!", dummyObject2);
			assertEquals("The context of the dummy object from the test context does not match!", testContext, dummyObject2.context);

			// now register a second provider instance ... this should destroy the provided objects
			serviceRegistration2 = Activator.getActivator().getServiceHelper().registerService(RuntimeContextObjectProvider.class.getName(), new DummyObjectProvider(), "Eclipse.org Gyrex", "Dummy object provider 2.", null, null);

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

	/**
	 * This test tests registration of a contextual object provider and
	 * retrieval with a particular configuration. It also tests update of the
	 * configuration and relies on service ranking to work properly.
	 */
	@Test
	public void testContextualObjectScenario003() {
		ServiceRegistration serviceRegistration = null;
		ServiceRegistration serviceRegistration2 = null;

		try {
			// register our providers
			// give provider one a higher ranking so that it is returned in favour of the second one
			serviceRegistration = Activator.getActivator().getServiceHelper().registerService(RuntimeContextObjectProvider.class.getName(), new DummyObjectProvider(), "Eclipse.org Gyrex", "Dummy object provider.", "org.eclipse.gyrex.context.tests.dummy1", Integer.MAX_VALUE);
			serviceRegistration2 = Activator.getActivator().getServiceHelper().registerService(RuntimeContextObjectProvider.class.getName(), new DummyObjectProvider2(), "Eclipse.org Gyrex", "Dummy object provider 2.", SERVICE_PID_DUMMY2, Integer.MIN_VALUE);

			final IRuntimeContext testContext = assertContextDefined(SOME_CONTEXT_PATH);

			// get object
			// this must return the object from provider 1 because of the higher service ranking
			final DummyObject dummyObject1 = testContext.get(DummyObject.class);
			assertFalse("The returned object was not provided by the expected provider (DummyObjectProvider 1)! Please verify that service ranking is taken into account when having multiple providers!", dummyObject1 instanceof DummyObject2);

			// configure context
			try {
				contextManager.set(testContext, DummyObject.class, FrameworkUtil.createFilter("(service.pid=" + SERVICE_PID_DUMMY2 + ")"));
			} catch (final InvalidSyntaxException e) {
				fail("Please check test case implementation, invalid filter string: " + e.getFilter() + "; " + e.getMessage());
			}

			// get object
			// this must return the object from provider 2 because of the defined filter
			final DummyObject dummyObject2 = testContext.get(DummyObject.class);
			assertTrue("The returned object was not provided by the configured provider (DummyObjectProvider 2)! Please verify that configured filters are verified correctly when having multiple providers!", dummyObject2 instanceof DummyObject2);

			// reconfigure
			contextManager.set(testContext, DummyObject.class, null);

			// get object
			// this time the object from provider 1 must be returned because of a higher service ranking
			final DummyObject dummyObject3 = testContext.get(DummyObject.class);
			assertFalse("The returned object was not provided by the expected provider (DummyObjectProvider 1)! Please verify that service ranking is taken into account when having multiple providers!", dummyObject3 instanceof DummyObject2);

		} finally {
			safeUnregister(serviceRegistration);
			safeUnregister(serviceRegistration2);
		}
	}

	private void testRemove(final GyrexContextHandle context) {
		final IPath path = context.getContextPath();
		final ContextDefinition definition = contextRegistry.getDefinition(path);
		assertNotNull("definiton must not be null if a context exists", definition);
		contextRegistry.removeDefinition(definition);
		assertNotNull("definitiono must be gone after remove", contextRegistry.getDefinition(path));

		try {
			context.get();
			fail("definition has been removed!");
		} catch (final IllegalStateException e) {
			// good
		}
	}
}
