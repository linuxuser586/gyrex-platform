/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.jetty.internal.app;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.gyrex.http.application.manager.IApplicationManager;
import org.eclipse.gyrex.http.jetty.internal.HttpJettyDebug;

import org.eclipse.jetty.http.PathMap;
import org.eclipse.jetty.http.PathMap.Entry;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.osgi.util.NLS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps request URIs to a context.
 * <p>
 * This implements the lookup as spec'd by
 * {@link IApplicationManager#mount(String, String)}.
 * </p>
 * <p>
 * The basic structure is
 * <code>scheme -&gt; virtual host -&gt; context path -&gt; context</code>
 * </p>
 * <p>
 * Note, this class is intentionally <strong>not</strong> thread safe. It's
 * optimized for concurrent reads but it must neither be modified concurrently
 * nor while concurrent reads happen. Instead, a new map should be built and
 * when completed, replaced with an existing map by the client.
 * </p>
 */
public class UrlMap {

	private static final Logger LOG = LoggerFactory.getLogger(UrlMap.class);

	private static final Integer ANY_PORT = new Integer(-1);
	private static final String EMPTY_STRING = "";

	private static int getEstimatedCapacity(final String domain) {
		if (domain.isEmpty() || "localhost".equals(domain)) {
			return 12;
		}
		return 1;
	}

	private final Map<String, Map<Integer, PathMap>> hostsToPortsToPathsForHttp = new HashMap<String, Map<Integer, PathMap>>(5);
	private final Map<String, Map<Integer, PathMap>> hostsToPortsToPathsForHttps = new HashMap<String, Map<Integer, PathMap>>(5);

	private Map<String, Map<Integer, PathMap>> getHostsToPortsToPathMap(final String protocol) {
		if (URIUtil.HTTP.equals(protocol)) {
			return hostsToPortsToPathsForHttp;
		} else if (URIUtil.HTTPS.equals(protocol)) {
			return hostsToPortsToPathsForHttps;
		} else {
			return null;
		}
	}

	/**
	 * Performs lookup of a handler using the specified input.
	 * <p>
	 * The input will be normalized prior to performing the actual lookup.
	 * </p>
	 * 
	 * @param protocol
	 * @param domain
	 * @param port
	 * @param path
	 * @return the best matching {@link Entry} from the underlying
	 *         {@link PathMap} (maybe <code>null</code>)
	 */
	public Entry getMatch(final String protocol, String domain, final int port, final String path) {
		// check input
		if ((protocol == null) || (domain == null) || (path == null) || !path.startsWith(URIUtil.SLASH)) {
			return null;
		}

		// get domain map based on protocol
		final Map<String, Map<Integer, PathMap>> hostsToPortsToPathMap = getHostsToPortsToPathMap(protocol);
		if (hostsToPortsToPathMap == null) {
			if (HttpJettyDebug.urlMapLookup) {
				LOG.debug("[URLMAP] no map for protocol {}://{}:{}{} --> {}", new Object[] { protocol, domain, port, path, null });
			}
			return null;
		}

		// normalize domain
		domain = UrlUtil.normalizeDomain(domain);

		// begin the lookup procedure
		Entry match = null;
		boolean continueMatching = true;
		while ((match == null) && continueMatching) {
			// debug logging
			if (HttpJettyDebug.urlMapLookup) {
				LOG.debug("[URLMAP] match loop {}://{}:{}{}", new Object[] { protocol, domain, port, path });
			}

			// domain name lookup
			final Map<Integer, PathMap> portsToPathMap = hostsToPortsToPathMap.get(domain);

			// walk upwards if necessary/possible (suffix matching)
			if (!domain.isEmpty()) {
				final int separatorIndex = domain.indexOf('.');
				domain = separatorIndex >= 0 ? domain.substring(separatorIndex + 1) : EMPTY_STRING;
			} else {
				continueMatching = false;
			}

			// check if no domain matched at all
			if (portsToPathMap == null) {
				if (HttpJettyDebug.urlMapLookup) {
					LOG.debug("[URLMAP] no map for domain {}://{}:{}{} --> {}", new Object[] { protocol, domain, port, path, match });
				}
				continue;
			}

			// get direct port match
			PathMap pathMap = portsToPathMap.get(port);
			if ((pathMap != null) && !pathMap.isEmpty()) {
				// get matching handler (don't sanitize path during lookup)
				match = pathMap.getMatch(path);
				// return early (if possible)
				if (match != null) {
					if (HttpJettyDebug.urlMapLookup) {
						LOG.debug("[URLMAP] direct port match {}://{}:{}{} --> {}", new Object[] { protocol, domain, port, path, match });
					}
					return match;
				}
			}

			// fallback to default port if no direct match
			pathMap = portsToPathMap.get(ANY_PORT);

			// get matching handler (don't sanitize path during lookup)
			if ((pathMap != null) && !pathMap.isEmpty()) {
				match = pathMap.getMatch(path);
				// return early (if possible)
				if (match != null) {
					if (HttpJettyDebug.urlMapLookup) {
						LOG.debug("[URLMAP] any port match {}://{}:{}{} --> {}", new Object[] { protocol, domain, port, path, match });
					}
					return match;
				}
			}
		}

		// debug logging
		if (HttpJettyDebug.urlMapLookup) {
			LOG.debug("[URLMAP] done match {}://{}:{}{} --> {}", new Object[] { protocol, domain, port, path, match });
		}

		// return what we have
		return match;
	}

	public boolean put(final String url, final Handler handler) {
		URL parsedUrl;
		try {
			parsedUrl = new URL(url);
		} catch (final MalformedURLException e) {
			throw new IllegalArgumentException("Invalid url: " + url, e);
		}
		// protocol
		final String protocol = UrlUtil.getNormalizedProtocol(parsedUrl);

		// get domain map based on protocol
		final Map<String, Map<Integer, PathMap>> hostsToPortsToPathMap = getHostsToPortsToPathMap(protocol);
		if (hostsToPortsToPathMap == null) {
			throw new IllegalArgumentException(NLS.bind("Protocol {0} not support for url {1}.", protocol, parsedUrl.toExternalForm()));
		}

		// virtual host
		final String domain = UrlUtil.getNormalizedDomain(parsedUrl);

		// get port map based on domain
		if (!hostsToPortsToPathMap.containsKey(domain)) {
			hostsToPortsToPathMap.put(domain, new HashMap<Integer, PathMap>(1));
		}
		final Map<Integer, PathMap> portsToPathMap = hostsToPortsToPathMap.get(domain);

		// port
		final Integer port = new Integer(UrlUtil.getNormalizedPort(parsedUrl));

		// get path map based on port
		if (!portsToPathMap.containsKey(port)) {
			portsToPathMap.put(port, new PathMap(getEstimatedCapacity(domain)));
		}
		final PathMap pathMap = portsToPathMap.get(port);

		// context path
		String path = UrlUtil.getNormalizedPath(parsedUrl);

		// make path matching implicit for PathMap
		// note, '/' must also be made implicit so that internally it matches as '/*'
		path = URIUtil.SLASH.equals(path) ? "/*" : path.concat("/*");

		// put handler
		final Object old = pathMap.put(path.length() == 0 ? "/" : path, handler);

		// ensure there was no conflict
		return old == null;
	}
}
