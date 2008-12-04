/**
 * Copyright (c) 2008 AGETO Service GmbH and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.cloudfree.http.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.cloudfree.http.internal.application.manager.ApplicationMount;
import org.eclipse.cloudfree.http.internal.application.manager.ApplicationMountRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class ApplicationMountRegistryTests {

	private static String stripTrailingSlahes(String path) {
		// strip trailing slashes from the path
		while ((path.length() > 0) && (path.charAt(path.length() - 1) == '/')) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

	private volatile long counter = 0;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for
	 * {@link org.eclipse.cloudfree.http.internal.application.manager.ApplicationMountRegistry#find(java.net.URL)}
	 * .
	 */
	@Test
	public void testFind() {
		final ApplicationMountRegistry registry = new ApplicationMountRegistry();
		try {
			final ApplicationMount localhost = new ApplicationMount(new URL("http://localhost"), "localhost");
			final ApplicationMount localhostSub = new ApplicationMount(new URL("http://localhost/sub"), "localhostSub");
			final ApplicationMount wwwDomainCom = new ApplicationMount(new URL("http://www.domain.com"), "wwwDomainCom");
			final ApplicationMount shopDomainCom = new ApplicationMount(new URL("http://shop.domain.com"), "shopDomainCom");
			final ApplicationMount domainCom = new ApplicationMount(new URL("http://domain.com"), "domainCom");
			final ApplicationMount domainComAdmin = new ApplicationMount(new URL("https://domain.com:9999/admin"), "domainComAdmin");
			final ApplicationMount defaultHttpResult = new ApplicationMount(new URL("http:///"), "defaultHttpResult");
			final ApplicationMount defaultAdminApp = new ApplicationMount(new URL("http:///admin"), "defaultAdminApp");
			//final ApplicationMount defaultHttpsResult = new ApplicationMount(new URL("https:///"), "defaultHttpsResult");

			registry.putIfAbsent(localhost.getMountPoint(), localhost);
			registry.putIfAbsent(localhostSub.getMountPoint(), localhostSub);
			registry.putIfAbsent(wwwDomainCom.getMountPoint(), wwwDomainCom);
			registry.putIfAbsent(shopDomainCom.getMountPoint(), shopDomainCom);
			registry.putIfAbsent(domainCom.getMountPoint(), domainCom);
			registry.putIfAbsent(domainComAdmin.getMountPoint(), domainComAdmin);
			registry.putIfAbsent(defaultHttpResult.getMountPoint(), defaultHttpResult);
			registry.putIfAbsent(defaultAdminApp.getMountPoint(), defaultAdminApp);

			// test 
			testFindMountPointDirect(registry, localhost);
			testFindMountPointDirect(registry, localhostSub);
			testFindMountPointDirect(registry, wwwDomainCom);
			testFindMountPointDirect(registry, shopDomainCom);
			testFindMountPointDirect(registry, domainCom);
			testFindMountPointDirect(registry, domainComAdmin);
			testFindMountPointDirect(registry, defaultHttpResult);
			testFindMountPointDirect(registry, defaultAdminApp);

			// test some special cases
			testFindSameMountPointByUrl(registry, shopDomainCom, "http://sub.shop.domain.com");
			testFindSameMountPointByUrl(registry, shopDomainCom, "http://sub.shop.domain.com/any/path");
			testFindSameMountPointByUrl(registry, domainCom, "http://sub.domain.com");
			testFindSameMountPointByUrl(registry, domainCom, "http://sub.domain.com/any/path");

			// test domainComAdmin only works on *.domain.com port 9999
			testFindSameMountPointByUrl(registry, domainComAdmin, "https://domain.com:9999/admin/");
			testFindSameMountPointByUrl(registry, domainComAdmin, "https://shop.domain.com:9999/admin/");
			testFindNotSameMountPointByUrl(registry, domainComAdmin, "https://domain.com/admin/");
			testFindNotSameMountPointByUrl(registry, domainComAdmin, "https://shop.domain.com/admin/");
			testFindNotSameMountPointByUrl(registry, domainComAdmin, "http://domain.com/admin/");
			testFindNotSameMountPointByUrl(registry, domainComAdmin, "http://shop.domain.com/admin/");

			// test default admin works on all unregistered domains and ports (for http only)
			testFindSameMountPointByUrl(registry, defaultAdminApp, "http://backoffice.admin.app/admin/");
			testFindSameMountPointByUrl(registry, defaultAdminApp, "http://kqncjdlkcnvaksd/admin/");
			testFindSameMountPointByUrl(registry, defaultAdminApp, "http://kqncjdlkcnvaksd:81/admin/");
			testFindSameMountPointByUrl(registry, defaultAdminApp, "http://kqncjdlkcnvaksd:80/admin/");

			// domain.com is register to domainCom and shut not result in default admin
			testFindNotSameMountPointByUrl(registry, defaultAdminApp, "http://domain.com/admin/");
			testFindNotSameMountPointByUrl(registry, defaultAdminApp, "http://shop.domain.com/admin/");

		} catch (final MalformedURLException e) {
			fail("Error in test case: " + e);
		}

		System.out.println("[testFind] stats: " + registry.getLookupMetric());
	}

	private void testFindMountPointDirect(final ApplicationMountRegistry registry, final ApplicationMount point) throws MalformedURLException {
		final String url = stripTrailingSlahes(point.getMountPoint().toExternalForm());
		testFindSameMountPointByUrl(registry, point, url);
		testFindSameMountPointByUrl(registry, point, url + "/sub" + ++counter);
		testFindSameMountPointByUrl(registry, point, url + "/sub" + ++counter + "/file.txt");
	}

	private void testFindNotSameMountPointByUrl(final ApplicationMountRegistry registry, final ApplicationMount notExpected, final String url) throws MalformedURLException {
		final ApplicationMount result = registry.find(new URL(url));
		if (notExpected == result) {
			fail("did not expected application '" + notExpected.getApplicationId() + "' for url '" + url + "' but got it");
		}
	}

	private void testFindSameMountPointByUrl(final ApplicationMountRegistry registry, final ApplicationMount expected, final String url) throws MalformedURLException {
		final ApplicationMount result = registry.find(new URL(url));
		if (expected != result) {
			fail("expected application '" + expected.getApplicationId() + "' for url '" + url + "' but got: " + result);
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.cloudfree.http.internal.application.manager.ApplicationMountRegistry#putIfAbsent(java.net.URL, org.eclipse.cloudfree.http.internal.application.manager.ApplicationMount)}
	 * .
	 */
	@Test
	public void testPutIfAbsent() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.eclipse.cloudfree.http.internal.application.manager.ApplicationMountRegistry#remove(java.net.URL)}
	 * .
	 */
	@Test
	public void testRemove() {
		fail("Not yet implemented");
	}

	@Test
	public void testResgisterDefaultPortConflict() {
		final ApplicationMountRegistry registry = new ApplicationMountRegistry();
		try {
			final ApplicationMount localhost = new ApplicationMount(new URL("http://localhost"), "localhost");
			final ApplicationMount localhost80 = new ApplicationMount(new URL("http://localhost:80"), "localhost80");

			registry.putIfAbsent(localhost.getMountPoint(), localhost);

			// registration to default port should not happen (i.e. return existing entry)
			assertNotNull("registration should have returned an existing entry", registry.putIfAbsent(localhost80.getMountPoint(), localhost80));

			// lookups should still return localhost
			testFindMountPointDirect(registry, localhost);
			testFindSameMountPointByUrl(registry, localhost, "http://localhost:80/");

		} catch (final MalformedURLException e) {
			fail("Error in test case: " + e);
		}
	}

}
