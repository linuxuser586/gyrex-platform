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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.cloudfree.http.internal.application.manager.ApplicationMount;
import org.eclipse.cloudfree.http.internal.application.manager.ApplicationMountRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class ApplicationMountRegistryTests {

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
			//final ApplicationMount defaultHttpsResult = new ApplicationMount(new URL("https:///"), "defaultHttpsResult");

			registry.putIfAbsent(localhost.getMountPoint(), localhost);
			registry.putIfAbsent(localhostSub.getMountPoint(), localhostSub);
			registry.putIfAbsent(wwwDomainCom.getMountPoint(), wwwDomainCom);
			registry.putIfAbsent(shopDomainCom.getMountPoint(), shopDomainCom);
			registry.putIfAbsent(domainCom.getMountPoint(), domainCom);
			registry.putIfAbsent(domainComAdmin.getMountPoint(), domainComAdmin);
			registry.putIfAbsent(defaultHttpResult.getMountPoint(), defaultHttpResult);

			// test 
			testFindMountPointDirect(registry, localhost);
			testFindMountPointDirect(registry, localhostSub);
			testFindMountPointDirect(registry, wwwDomainCom);
			testFindMountPointDirect(registry, shopDomainCom);
			testFindMountPointDirect(registry, domainCom);
			testFindMountPointDirect(registry, domainComAdmin);
			testFindMountPointDirect(registry, defaultHttpResult);

			// test some special cases
			testFindMountPointByUrl(registry, shopDomainCom, "http://sub.shop.domain.com");
			testFindMountPointByUrl(registry, shopDomainCom, "http://sub.shop.domain.com/any/path");
			testFindMountPointByUrl(registry, domainCom, "http://sub.domain.com");
			testFindMountPointByUrl(registry, domainCom, "http://sub.domain.com/any/path");
			testFindMountPointByUrl(registry, domainComAdmin, "https://domain.com:9999/admin");
			testFindMountPointByUrl(registry, null, "https://domain.com/admin/");
		} catch (final MalformedURLException e) {
			fail("Error in test case: " + e);
		}
	}

	private void testFindMountPointByUrl(final ApplicationMountRegistry registry, final ApplicationMount point, final String url) throws MalformedURLException {
		assertSame(url + " should match " + (null != point ? point.getApplicationId() : null), point, registry.find(new URL(url)));
	}

	private void testFindMountPointDirect(final ApplicationMountRegistry registry, final ApplicationMount point) throws MalformedURLException {
		final URL url = point.getMountPoint();
		testFindMountPointByUrl(registry, point, url.toExternalForm());
		testFindMountPointByUrl(registry, point, url.toExternalForm() + "/sub" + ++counter);
		testFindMountPointByUrl(registry, point, url.toExternalForm() + "/sub" + ++counter + "/file.txt");
	}

	/**
	 * Test method for
	 * {@link org.eclipse.cloudfree.http.internal.application.manager.ApplicationMountRegistry#matchDomain(java.net.URL, java.util.concurrent.ConcurrentMap)}
	 * .
	 */
	@Test
	public void testMatchDomain() {
		final ApplicationMountRegistry registry = new ApplicationMountRegistry();
		final ConcurrentMap<String, ConcurrentMap<Integer, ConcurrentMap<String, ApplicationMount>>> test = new ConcurrentHashMap<String, ConcurrentMap<Integer, ConcurrentMap<String, ApplicationMount>>>();
		final ConcurrentHashMap<Integer, ConcurrentMap<String, ApplicationMount>> localhost = new ConcurrentHashMap<Integer, ConcurrentMap<String, ApplicationMount>>();
		final ConcurrentHashMap<Integer, ConcurrentMap<String, ApplicationMount>> wwwDomainCom = new ConcurrentHashMap<Integer, ConcurrentMap<String, ApplicationMount>>();
		final ConcurrentHashMap<Integer, ConcurrentMap<String, ApplicationMount>> shopDomainCom = new ConcurrentHashMap<Integer, ConcurrentMap<String, ApplicationMount>>();
		final ConcurrentHashMap<Integer, ConcurrentMap<String, ApplicationMount>> domainCom = new ConcurrentHashMap<Integer, ConcurrentMap<String, ApplicationMount>>();
		final ConcurrentHashMap<Integer, ConcurrentMap<String, ApplicationMount>> defaultResult = new ConcurrentHashMap<Integer, ConcurrentMap<String, ApplicationMount>>();
		test.put("localhost", localhost);
		test.put("www.domain.com", wwwDomainCom);
		test.put("shop.domain.com", shopDomainCom);
		test.put("domain.com", domainCom);
		test.put(ApplicationMountRegistry.MATCH_ALL_DOMAIN, defaultResult);
		try {
			// test different domain names
			assertSame("localhost should match localhost", localhost, registry.matchDomain(new URL("http://localhost/path/does/notMatter"), test));
			assertSame("www.domain.com should match wwwDomainCom", wwwDomainCom, registry.matchDomain(new URL("http://www.domain.com/path/does/notMatter"), test));
			assertSame("shop.domain.com should match shopDomainCom", shopDomainCom, registry.matchDomain(new URL("http://shop.domain.com/path/does/notMatter"), test));
			assertSame("domain.com should match domainCom", domainCom, registry.matchDomain(new URL("http://domain.com/path/does/notMatter"), test));
			assertSame("some.domain.com should match domainCom", domainCom, registry.matchDomain(new URL("http://some.domain.com/path/does/notMatter"), test));
			assertSame("any.other.domain should match defaultResult", defaultResult, registry.matchDomain(new URL("http://any.other.domain/path/does/notMatter"), test));
			assertSame("<empty domain> should match defaultResult", defaultResult, registry.matchDomain(new URL("http:///path/does/notMatter"), test));

			// test different case
			assertSame("SHOP.domain.com should match shopDomainCom", shopDomainCom, registry.matchDomain(new URL("http://SHOP.domain.com/path/does/notMatter"), test));
			assertSame("shop.DOMAIN.com should match shopDomainCom", shopDomainCom, registry.matchDomain(new URL("http://shop.DOMAIN.com/path/does/notMatter"), test));
			assertSame("shop.domain.COM should match shopDomainCom", shopDomainCom, registry.matchDomain(new URL("http://shop.domain.COM/path/does/notMatter"), test));
			assertSame("ANY.other.domain should match defaultResult", defaultResult, registry.matchDomain(new URL("http://ANY.other.domain/path/does/notMatter"), test));
			assertSame("any.OTHER.domain should match defaultResult", defaultResult, registry.matchDomain(new URL("http://any.OTHER.domain/path/does/notMatter"), test));
			assertSame("any.other.DOMAIN should match defaultResult", defaultResult, registry.matchDomain(new URL("http://any.other.DOMAIN/path/does/notMatter"), test));

			// test different ports
			assertSame("http://shop.domain.com:80 should match shopDomainCom", shopDomainCom, registry.matchDomain(new URL("http://shop.domain.com:81"), test));
			assertSame("http://shop.domain.com:81 should match shopDomainCom", shopDomainCom, registry.matchDomain(new URL("http://shop.domain.com:80"), test));
			assertSame("http://shop.domain.com:8080 should match shopDomainCom", shopDomainCom, registry.matchDomain(new URL("http://shop.domain.com:8080"), test));
			assertSame("http://domain.com:80 should match domainCom", domainCom, registry.matchDomain(new URL("http://domain.com:80"), test));
			assertSame("http://domain.com:81 should match domainCom", domainCom, registry.matchDomain(new URL("http://domain.com:81"), test));
			assertSame("http://domain.com:888 should match domainCom", domainCom, registry.matchDomain(new URL("http://domain.com:888"), test));

			// test different paths
			assertSame("http://shop.domain.com should match shopDomainCom", shopDomainCom, registry.matchDomain(new URL("http://shop.domain.com"), test));
			assertSame("http://shop.domain.com/ should match shopDomainCom", shopDomainCom, registry.matchDomain(new URL("http://shop.domain.com/"), test));
			assertSame("http://shop.domain.com/path should match shopDomainCom", shopDomainCom, registry.matchDomain(new URL("http://shop.domain.com/path"), test));

			// mixture
			assertSame("http://shop.domain.com:81/ should match shopDomainCom", shopDomainCom, registry.matchDomain(new URL("http://shop.domain.com:81/"), test));
			assertSame("http://shop.domain.com:8080/path should match shopDomainCom", shopDomainCom, registry.matchDomain(new URL("http://shop.domain.com:8080/path"), test));
		} catch (final MalformedURLException e) {
			fail("Error in test case: " + e);
		}
	}

	/**
	 * Test method for
	 * {@link org.eclipse.cloudfree.http.internal.application.manager.ApplicationMountRegistry#matchPath(java.net.URL, java.util.concurrent.ConcurrentMap)}
	 * .
	 */
	@Test
	public void testMatchPath() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.eclipse.cloudfree.http.internal.application.manager.ApplicationMountRegistry#matchPort(java.net.URL, java.util.concurrent.ConcurrentMap)}
	 * .
	 */
	@Test
	public void testMatchPort() {
		final ApplicationMountRegistry registry = new ApplicationMountRegistry();
		final ConcurrentMap<Integer, ConcurrentMap<String, ApplicationMount>> test = new ConcurrentHashMap<Integer, ConcurrentMap<String, ApplicationMount>>();
		final ConcurrentMap<String, ApplicationMount> defaultResult = new ConcurrentHashMap<String, ApplicationMount>(1);
		final ConcurrentMap<String, ApplicationMount> port80 = new ConcurrentHashMap<String, ApplicationMount>(1);
		final ConcurrentMap<String, ApplicationMount> port443 = new ConcurrentHashMap<String, ApplicationMount>(1);
		final ConcurrentMap<String, ApplicationMount> port8080 = new ConcurrentHashMap<String, ApplicationMount>(1);
		test.put(80, port80);
		test.put(443, port443);
		test.put(8080, port8080);
		test.put(ApplicationMountRegistry.MATCH_ALL_PORT, defaultResult);
		try {
			// test different ports
			assertSame("http://shop.domain.com:80 should match port80", port80, registry.matchPort(new URL("http://shop.domain.com:80"), test));
			assertSame("http://shop.domain.com:443 should match port443", port443, registry.matchPort(new URL("http://shop.domain.com:443"), test));
			assertSame("http://shop.domain.com:8080 should match port8080", port8080, registry.matchPort(new URL("http://shop.domain.com:8080"), test));
			assertSame("http://shop.domain.com:999 should match defaultResult", defaultResult, registry.matchPort(new URL("http://shop.domain.com:999"), test));

			// different protocol should still give the same result
			assertSame("https://shop.domain.com:80 should match port80", port80, registry.matchPort(new URL("https://shop.domain.com:80"), test));
			assertSame("https://shop.domain.com:443 should match port443", port443, registry.matchPort(new URL("https://shop.domain.com:443"), test));
			assertSame("https://shop.domain.com:8080 should match port8080", port8080, registry.matchPort(new URL("https://shop.domain.com:8080"), test));
			assertSame("https://shop.domain.com:999 should match defaultResult", defaultResult, registry.matchPort(new URL("https://shop.domain.com:999"), test));

			// different domains should still give the same result
			assertSame("http://bla.de:80 should match port80", port80, registry.matchPort(new URL("http://bla.de:80"), test));
			assertSame("http://shop.xyz:443 should match port443", port443, registry.matchPort(new URL("http://shop.xyz:443"), test));
			assertSame("https://shop.abc.com:8080 should match port8080", port8080, registry.matchPort(new URL("https://shop.abc.com:8080"), test));
			assertSame("http://def:999 should match defaultResult", defaultResult, registry.matchPort(new URL("http://def:999"), test));

			// different paths should still give the same result
			assertSame("https://shop.domain.com:80/ should match port80", port80, registry.matchPort(new URL("https://shop.domain.com:80/"), test));
			assertSame("https://shop.domain.com:443/some/path should match port443", port443, registry.matchPort(new URL("https://shop.domain.com:443/some/path"), test));
			assertSame("https://shop.domain.com:8080/here/too should match port8080", port8080, registry.matchPort(new URL("https://shop.domain.com:8080/here/too"), test));
			assertSame("https://shop.domain.com:999/oh/sorry should match defaultResult", defaultResult, registry.matchPort(new URL("https://shop.domain.com:999/oh/sorry"), test));

		} catch (final MalformedURLException e) {
			fail("Error in test case: " + e);
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

}
