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
package org.eclipse.gyrex.http.internal.application.manager;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.gyrex.common.debug.BundleDebug;
import org.eclipse.gyrex.http.application.manager.IApplicationManager;
import org.eclipse.gyrex.monitoring.metrics.ThroughputMetric;

/**
 * Specialized registry for mapping URLs to application mounts.
 */
public class ApplicationMountRegistry {

	private static final int LOOKUP_LOOP_LIMIT = 1000;

	public static final String MATCH_ALL_DOMAIN = "";

	private static String getDomain(final URL url) {
		// domain matching is lower case
		return url.getHost().toLowerCase();
	}

	private static String getPath(final URL url) {
		// remove trailing slashes but preserve case
		return stripTrailingSlahes(url.getPath());
	}

	private static int getPort(final URL url) {
		// note, we always ignore the default port
		return url.getPort() != url.getDefaultPort() ? url.getPort() : -1;
	}

	private static String getProtocol(final URL url) {
		// protocol matching is lower case
		return url.getProtocol().toLowerCase();
	}

	private static String stripTrailingSlahes(String path) {
		// strip trailing slashes from the path
		while ((path.length() > 0) && (path.charAt(path.length() - 1) == '/')) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

	private final ConcurrentMap<String, ApplicationMount> mountsByUrl = new ConcurrentHashMap<String, ApplicationMount>();
	private final ThroughputMetric lookupMetric = new ThroughputMetric(toString());

	/**
	 * Build the lookup url from the specified URL parts.
	 * 
	 * @param protocol
	 * @param domainName
	 * @param port
	 * @param path
	 * @return the internal lookup url
	 * @noreference This method is not intended to be referenced by clients.
	 */
	private String buildLookupUrl(final String protocol, final String domainName, final int port, final String path) {
		final StringBuilder builder = new StringBuilder(256);
		builder.append(protocol).append(':');
		if ((domainName != null) && (domainName.length() > 0)) {
			builder.append("//").append(domainName);
		}
		if (port != -1) {
			if (builder.charAt(builder.length() - 1) == ':') {
				builder.append("//");
			}
			builder.append(':').append(port);
		}
		if ((path != null) && (path.length() > 0)) {
			builder.append(path);
		}
		return builder.toString();
	}

	/**
	 * Implements application mount lookup as specified in
	 * {@link IApplicationManager#mount(String, String)}.
	 * 
	 * @param url
	 *            the url
	 * @return the {@link ApplicationMount} if found, <code>null</code>
	 *         otherwise
	 */
	public ApplicationMount find(final URL url) {

		// TODO we should try to cache the lookup somehow

		// protocol matching is exact
		final String protocol = getProtocol(url);

		// domain matching is case-insensitive
		String domainName = getDomain(url);

		// port matching
		int port = getPort(url);

		// path matching is case-sensitive
		String path = getPath(url);

		// collect some metrics
		final long start = lookupMetric.requestStarted();
		int loopCounter = 0;

		// start the match loop
		ApplicationMount applicationMount = null;
		do {
			// note, we do not intern any lookup string (there are too many)
			final String lookupUrl = buildLookupUrl(protocol, domainName, port, path);
			applicationMount = mountsByUrl.get(lookupUrl);
			if (applicationMount == null) {
				// first, traverse path up
				if ((path != null) && (path.length() > 0)) {
					path = nextPath(path);
				} else {
					// second, try with default port
					if (port != -1) {
						port = port != url.getDefaultPort() ? url.getDefaultPort() : -1;
						// reset path to start lookup again
						path = getPath(url);
					} else {
						// last, traverse domain up
						if ((domainName != null) && (domainName.length() > 0)) {
							domainName = nextDomainName(domainName);
							// reset path and port to start lookup again
							path = getPath(url);
							port = getPort(url);
						} else {
							// path is null, port -1 and domainName too
							break;
						}
					}
				}
			}
		} while ((++loopCounter < 1000) && (applicationMount == null) && ((path != null) || (domainName != null) || (port != -1)));

		// finish metrics
		if (applicationMount != null) {
			lookupMetric.requestFinished(loopCounter, System.currentTimeMillis() - start);
		} else {
			lookupMetric.requestFailed();
		}

		// log if limit exceeded
		if (loopCounter >= LOOKUP_LOOP_LIMIT) {
			BundleDebug.debug("[ApplicationMountRegistry] loop limit exceeded for url " + url);
		}

		// return what we have
		return applicationMount;
	}

	/**
	 * Returns the lookupMetric.
	 * 
	 * @return the lookupMetric
	 */
	public ThroughputMetric getLookupMetric() {
		return lookupMetric;
	}

	/**
	 * Calculates the next domain name.
	 * 
	 * @param domainName
	 * @return the next domain name to match
	 * @noreference This method is not intended to be referenced by clients.
	 */
	private String nextDomainName(final String domainName) {
		final int firstDot = domainName.indexOf('.');
		if (firstDot != -1) {
			// one domain up
			return domainName.substring(firstDot + 1);
		} else if (domainName.length() > 0) {
			// default domain
			return MATCH_ALL_DOMAIN;
		}

		// abort
		return null;
	}

	/**
	 * Calculates the next path.
	 * 
	 * @param path
	 * @return the next path to match
	 * @noreference This method is not intended to be referenced by clients.
	 */
	private String nextPath(final String path) {
		// one dir up if possible
		final int lastSlash = path.lastIndexOf('/');
		if (lastSlash != -1) {
			return path.substring(0, lastSlash);
		}

		// abort
		return null;
	}

	/**
	 * Puts the specified application mount into the map using the specified URL
	 * if it's not already registered.
	 * 
	 * @param url
	 * @param applicationMount
	 * @return the previous value associated with the specified url, or
	 *         <code>null</code> if there was no mapping for the url
	 */
	public ApplicationMount putIfAbsent(final URL url, final ApplicationMount applicationMount) {
		// get protocol
		final String protocol = getProtocol(url);
		// get domain
		final String domain = getDomain(url);
		// get port
		final int port = getPort(url);
		// get path
		final String path = getPath(url);

		// add mount if absent
		return mountsByUrl.putIfAbsent(buildLookupUrl(protocol, domain, port, path).intern(), applicationMount);
	}

	/**
	 * Removes an application mount mapping for the specified URL.
	 * 
	 * @param url
	 * @return the previous value associated with url, or <code>null</code> if
	 *         there was no mapping for the url
	 */
	public ApplicationMount remove(final URL url) {
		// get protocol
		final String protocol = getProtocol(url);
		// get domain
		final String domain = getDomain(url);
		// get port
		final int port = getPort(url);
		// get path
		final String path = getPath(url);

		// remove mount if present
		return mountsByUrl.remove(buildLookupUrl(protocol, domain, port, path).intern());
	}
}
