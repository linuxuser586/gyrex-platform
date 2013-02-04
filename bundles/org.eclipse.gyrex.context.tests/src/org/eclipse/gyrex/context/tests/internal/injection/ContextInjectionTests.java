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
package org.eclipse.gyrex.context.tests.internal.injection;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;

import org.eclipse.gyrex.context.internal.di.GyrexContextObjectSupplier;
import org.eclipse.gyrex.context.tests.internal.Activator;
import org.eclipse.gyrex.context.tests.internal.BaseContextTest;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.osgi.framework.ServiceRegistration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class ContextInjectionTests extends BaseContextTest {

	private static final String STRING_1 = "HELLO1_" + System.nanoTime();
	private static final String STRING_2 = "HELLO2_" + System.nanoTime();
	private ServiceRegistration<ISampleService> serviceRegistration;
	private SampleServiceImpl service;

	@Override
	protected IPath getPrimaryTestContextPath() {
		return Path.ROOT;
	}

	@Before
	public void registerService() {
		service = new SampleServiceImpl(STRING_1);
		serviceRegistration = Activator.getActivator().getServiceHelper().registerService(ISampleService.class, service, null, null, null, null);
	}

	@Test
	public void test001MakeStaticObjects() throws Exception {
		final ConstructorInjection object = getContext().getInjector().make(ConstructorInjection.class);
		assertNotNull("no object created", object);
		assertNotSame("object is not a singleton", object, getContext().getInjector().make(ConstructorInjection.class));

		final ConstructorInjectionWithOsgiService object2 = getContext().getInjector().make(ConstructorInjectionWithOsgiService.class);
		assertNotNull("object with osgi service not created", object2);
		assertNotSame("object is not a singleton", object2, getContext().getInjector().make(ConstructorInjectionWithOsgiService.class));
		assertSame("wrong service response", STRING_1, object2.service.getString());

		final ConstructorInjectionSingleton singleton = getContext().getInjector().make(ConstructorInjectionSingleton.class);
		assertNotNull("no singleton object created", singleton);
		assertSame("singleton expected", singleton, getContext().getInjector().make(ConstructorInjectionSingleton.class));
		assertSame("singleton expected", singleton, getContext().getInjector().make(ConstructorInjectionSingleton.class));
		assertSame("wrong service response", STRING_1, singleton.service.getString());
	}

	@Test
	public void test002MakeDynamicObjects() throws Exception {
		final DynamicFieldInjection object = getContext().getInjector().make(DynamicFieldInjection.class);
		assertNotNull("no object created", object);
		object.assertInjected();

		// verify response
		assertSame("wrong service response", STRING_1, object.service.getString());

		// assert that the injected service is a real service but not a proxy
		if (GyrexContextObjectSupplier.dynamicInjectionEnabled) {
			assertSame("real service expected instead of proxy for dynamic injection", service, object.service);
		}

		// register second service instance (with higher ranking)
		final ServiceRegistration<ISampleService> sr2 = Activator.getActivator().getServiceHelper().registerService(ISampleService.class, new SampleServiceImpl(STRING_2), null, null, null, Integer.MAX_VALUE);

		// wait a bit for injector to catch up
		// (events are processed asynchronously and collected for batch processing)
		Thread.sleep(750L);

		// assert that two services are available
		assertTrue("collection should have two services now", object.services.size() == 2);

		// assert that "primary" service changed (because of higher ranking!)
		assertSame("wrong service response; service should have changed", STRING_2, object.service.getString());

		// unregister service again
		sr2.unregister();

		// wait a bit for injector to catch up
		// (events are processed asynchronously and collected for batch processing)
		Thread.sleep(750L);

		// assert that service is gone
		assertTrue("collection should be down to one services now", object.services.size() == 1);

		// assert that "primary" service also changed
		assertSame("wrong service response; service should have changed", STRING_1, object.service.getString());

		// now let's really mess up
		serviceRegistration.unregister();

	}

	@After
	public void unregisterService() {
		try {
			serviceRegistration.unregister();
		} catch (final IllegalStateException e) {
			// ignore
		}
		serviceRegistration = null;
		service = null;
	}
}
