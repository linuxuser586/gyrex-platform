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
package org.eclipse.gyrex.http.tests.internal;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.provider.ApplicationProvider;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationProviderRegistration;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationProviderRegistry;

import org.eclipse.core.runtime.CoreException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class ApplicationRegistryTests {

	static class TestApp extends Application {
		TestApp(final String id, final IRuntimeContext context) {
			super(id, context);
		}
	}

	static class TestAppProvider extends ApplicationProvider {
		TestAppProvider() {
			super(TestAppProvider.class.getName());
		}

		@Override
		public Application createApplication(final String applicationId, final IRuntimeContext context) throws CoreException {
			return new TestApp(applicationId, context);
		}
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testProviderRegistration() {
		// create manager
		final BundleContext context = Activator.getBundleContext();
		final ApplicationProviderRegistry registry = new ApplicationProviderRegistry(context);
		final TestAppProvider appProvider = new TestAppProvider();

		try {
			// open manager
			registry.open();

			// ensure that there is no registration when starting the test
			if (null != registry.getProviderRegistration(appProvider.getId())) {
				fail("there is already a provider registered; that should not happen");
			}

			// register provider
			final ServiceRegistration serviceRegistration = context.registerService(ApplicationProvider.class.getName(), appProvider, null);

			// assert registration not null
			ApplicationProviderRegistration providerRegistration = registry.getProviderRegistration(appProvider.getId());
			assertNotNull("provider not registered with manager", providerRegistration);

			// unregister
			serviceRegistration.unregister();

			// assert registration is null
			providerRegistration = registry.getProviderRegistration(appProvider.getId());
			assertNull("provider should be unregistered at this point", providerRegistration);

		} finally {
			// cleanup
			registry.close();
		}
	}
}
