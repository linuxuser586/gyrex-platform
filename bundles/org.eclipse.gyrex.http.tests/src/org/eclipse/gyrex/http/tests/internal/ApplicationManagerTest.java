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

import static junit.framework.Assert.fail;

import java.net.MalformedURLException;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.manager.MountConflictException;
import org.eclipse.gyrex.http.application.provider.ApplicationProvider;
import org.eclipse.gyrex.http.internal.application.gateway.HttpGatewayBinding;
import org.eclipse.gyrex.http.internal.application.gateway.IHttpGateway;
import org.eclipse.gyrex.http.internal.application.gateway.IUrlRegistry;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationManager;

import org.eclipse.core.runtime.CoreException;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class ApplicationManagerTest {

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

	static class TestGateway implements IHttpGateway {

		@Override
		public String getName() {
			return "Test";
		}

		@Override
		public IUrlRegistry getUrlRegistry(final HttpGatewayBinding applicationManager) {
			return new TestUrlRegistry();
		}

	}

	static class TestUrlRegistry implements IUrlRegistry {

		ConcurrentHashMap<String, String> map = new ConcurrentHashMap<String, String>();

		/* (non-Javadoc)
		 * @see org.eclipse.gyrex.http.internal.application.gateway.IUrlRegistry#applicationUnregistered(java.lang.String)
		 */
		@Override
		public void applicationUnregistered(final String applicationId) {
			// TODO Auto-generated method stub

		}

		@Override
		public String registerIfAbsent(final String url, final String applicationId) {
			return map.putIfAbsent(url, applicationId);
		}

		@Override
		public String unregister(final String url) {
			return map.remove(url);
		}

	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testMount() {
		// create manager
		final BundleContext context = Activator.getBundleContext();
		final ApplicationManager applicationManager = new ApplicationManager();

		ServiceRegistration serviceRegistration = null;
		try {
			// register provider
			final TestAppProvider appProvider = new TestAppProvider();
			serviceRegistration = context.registerService(ApplicationProvider.class.getName(), appProvider, null);

			// define application
			final String applicationId = "testMountApp" + String.valueOf(System.currentTimeMillis());
			try {
				applicationManager.register(applicationId, appProvider.getId(), null, null);
			} catch (final Exception e) {
				fail("error while registering dummy application: " + e);
			}

			// test urls
			final String urlWithServerAndPath = "http://someserver/some/path";
			final String urlWithServerAndPathAndTrailingSlash = "http://someserver/some/path/";
			final String urlJustServer = "http://just.a.server.name";
			final String urlJustServerWithTrailingSlash = "http://just.a.server.name/";
			final String urlWithoutServerButWithPath1 = "http:///some/path";
			final String urlWithoutServerButWithPath2 = "http:/some/path";
			final String urlWithoutServerButWithPathAndTrailingSlash1 = "http:///some/path/";
			final String urlWithoutServerButWithPathAndTrailingSlash2 = "http:/some/path/";
			final String urlWithoutServerWithoutPath1 = "http:///";
			final String urlWithoutServerWithoutPath2 = "http:/";

			// test using exact urls first
			testMountFollowedByUnmount(applicationManager, applicationId, urlWithServerAndPath);
			testMountFollowedByUnmount(applicationManager, applicationId, urlWithServerAndPathAndTrailingSlash);
			testMountFollowedByUnmount(applicationManager, applicationId, urlJustServer);
			testMountFollowedByUnmount(applicationManager, applicationId, urlJustServerWithTrailingSlash);
			testMountFollowedByUnmount(applicationManager, applicationId, urlWithoutServerButWithPath1);
			testMountFollowedByUnmount(applicationManager, applicationId, urlWithoutServerButWithPath2);
			testMountFollowedByUnmount(applicationManager, applicationId, urlWithoutServerButWithPathAndTrailingSlash1);
			testMountFollowedByUnmount(applicationManager, applicationId, urlWithoutServerButWithPathAndTrailingSlash2);
			testMountFollowedByUnmount(applicationManager, applicationId, urlWithoutServerWithoutPath1);
			testMountFollowedByUnmount(applicationManager, applicationId, urlWithoutServerWithoutPath2);

			testMountAndUnmountConflicts(applicationManager, applicationId, urlWithServerAndPath, urlWithServerAndPathAndTrailingSlash);
			testMountAndUnmountConflicts(applicationManager, applicationId, urlJustServer, urlJustServerWithTrailingSlash);

			testMountAndUnmountConflicts(applicationManager, applicationId, urlWithoutServerButWithPath1, urlWithoutServerButWithPath2);
			testMountAndUnmountConflicts(applicationManager, applicationId, urlWithoutServerButWithPath1, urlWithoutServerButWithPathAndTrailingSlash1);
			testMountAndUnmountConflicts(applicationManager, applicationId, urlWithoutServerButWithPath1, urlWithoutServerButWithPathAndTrailingSlash2);
			testMountAndUnmountConflicts(applicationManager, applicationId, urlWithoutServerButWithPathAndTrailingSlash1, urlWithoutServerButWithPathAndTrailingSlash2);

			testMountAndUnmountConflicts(applicationManager, applicationId, urlWithoutServerWithoutPath1, urlWithoutServerWithoutPath2);
		} finally {
			if (null != serviceRegistration) {
				serviceRegistration.unregister();
			}

		}

	}

	private void testMountAndUnmountConflicts(final ApplicationManager applicationManager, final String applicationId, final String url1, final String url2) {
		// registration w/o trailing slash
		testMountUrl(applicationManager, applicationId, url1, false /* no conflict*/);
		testMountUrl(applicationManager, applicationId, url2, true /* conflict expected */);

		// unmount
		testUnmountUrl(applicationManager, url1, false /* must not fail */);
		testUnmountUrl(applicationManager, url2, true /* must fail */);

		// registration w/o trailing slash
		testMountUrl(applicationManager, applicationId, url1, false /* no conflict*/);
		testMountUrl(applicationManager, applicationId, url2, true /* conflict expected */);

		// unmount in reverse order
		testUnmountUrl(applicationManager, url2, false /* must not fail */);
		testUnmountUrl(applicationManager, url1, true /* must fail */);

		// registration w/o trailing slash in reverse order
		testMountUrl(applicationManager, applicationId, url2, false /* no conflict*/);
		testMountUrl(applicationManager, applicationId, url1, true /* conflict expected */);

		// unmount
		testUnmountUrl(applicationManager, url1, false /* must not fail */);
		testUnmountUrl(applicationManager, url2, true /* must fail */);

		// registration w/o trailing slash in reverse order
		testMountUrl(applicationManager, applicationId, url2, false /* no conflict*/);
		testMountUrl(applicationManager, applicationId, url1, true /* conflict expected */);

		// unmount in reverse order
		testUnmountUrl(applicationManager, url2, false /* must not fail */);
		testUnmountUrl(applicationManager, url1, true /* must fail */);
	}

	private void testMountFollowedByUnmount(final ApplicationManager applicationManager, final String applicationId, final String url) {
		testMountUrl(applicationManager, applicationId, url, false /* no conflict*/);
		testUnmountUrl(applicationManager, url, false /* must not fail */);
	}

	private void testMountUrl(final ApplicationManager applicationManager, final String applicationId, final String url, final boolean expectMountConflict) {
		try {
			applicationManager.mount(url, applicationId);
			if (expectMountConflict) {
				fail("mount conflict expected but not occured for url '" + url + "'");
			}
		} catch (final MountConflictException e) {
			if (!expectMountConflict) {
				fail("mount conflict occured but not expected for url '" + url + "': " + e.getMessage());
			}
		} catch (final MalformedURLException e) {
			fail("invalid test url '" + url + "': " + e.getMessage());
		} catch (final IllegalArgumentException e) {
			fail("invalid test url '" + url + "': " + e.getMessage());
		}
	}

	private void testUnmountUrl(final ApplicationManager applicationManager, final String url, final boolean expectedUnmountToFail) {
		try {
			applicationManager.unmount(url);
			if (expectedUnmountToFail) {
				fail("unmount did not fail but was expected for url '" + url + "'");
			}
		} catch (final MalformedURLException e) {
			fail("invalid test url '" + url + "': " + e.getMessage());
		} catch (final IllegalArgumentException e) {
			fail("invalid test url '" + url + "': " + e.getMessage());
		} catch (final IllegalStateException e) {
			if (!expectedUnmountToFail) {
				fail("unmount failed but not expected for url '" + url + "': " + e.getMessage());
			}
		}
	}

}
