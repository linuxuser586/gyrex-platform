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
package org.eclipse.cloudfree.http.internal.application.manager;

import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.cloudfree.http.application.manager.IApplicationManager;

/**
 * Specialized registry for mapping URLs to application mounts.
 */
public class ApplicationMountRegistry {

	public static final String MATCH_ALL_DOMAIN = "";
	public static final Integer MATCH_ALL_PORT = new Integer(-1);

	/**
	 * This is the magic structure. It's a set of nested maps.
	 * 
	 * <pre>
	 * protocol | -domain | -port | -path
	 * </pre>
	 */
	private final ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Integer, ConcurrentMap<String, ApplicationMount>>>> mountsByProtocol = new ConcurrentHashMap<String, ConcurrentMap<String, ConcurrentMap<Integer, ConcurrentMap<String, ApplicationMount>>>>(2);
	private final Lock modificationLock = new ReentrantLock();

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
		// note, we do not intern any lookup string (there are too many)

		// match protocol
		final ConcurrentMap<String, ConcurrentMap<Integer, ConcurrentMap<String, ApplicationMount>>> mountsByDomain = mountsByProtocol.get(url.getProtocol());
		if (null == mountsByDomain) {
			return null;
		}

		// match domain
		final ConcurrentMap<Integer, ConcurrentMap<String, ApplicationMount>> mountsByPort = matchDomain(url, mountsByDomain);
		if (null == mountsByPort) {
			return null;
		}

		// match port
		final ConcurrentMap<String, ApplicationMount> mountsByPath = matchPort(url, mountsByPort);
		if (null == mountsByPath) {
			return null;
		}

		// match path
		final ApplicationMount applicationMount = matchPath(url, mountsByPath);

		// return what we have
		return applicationMount;
	}

	/**
	 * Matches the domain portion against string keys in the specified map.
	 * 
	 * @param url
	 * @param mountsByDomain
	 * @return result map if found, <code>null</code> otherwise
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public ConcurrentMap<Integer, ConcurrentMap<String, ApplicationMount>> matchDomain(final URL url, final ConcurrentMap<String, ConcurrentMap<Integer, ConcurrentMap<String, ApplicationMount>>> mountsByDomain) {
		// don't intern paths ... can be *many*
		// domain matching is case-insensitive
		String domain = url.getHost().toLowerCase();
		ConcurrentMap<Integer, ConcurrentMap<String, ApplicationMount>> mountsByPort;
		do {
			mountsByPort = mountsByDomain.get(domain);
			if (null == mountsByPort) {
				final int firstDot = domain.indexOf('.');
				if (firstDot != -1) {
					// one domain up
					domain = domain.substring(firstDot + 1);
				} else if (domain.length() > 0) {
					// default domain
					domain = MATCH_ALL_DOMAIN;
				} else {
					// abort
					domain = null;
				}
			}
		} while ((null == mountsByPort) && (null != domain));
		return mountsByPort;
	}

	/**
	 * Matches the path against string keys in the specified map.
	 * 
	 * @param url
	 * @param mountsByPath
	 * @return result map if found, <code>null</code> otherwise
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public ApplicationMount matchPath(final URL url, final ConcurrentMap<String, ApplicationMount> mountsByPath) {
		// don't intern paths ... can be *many*
		String path = url.getPath();
		ApplicationMount applicationMount;
		do {
			applicationMount = mountsByPath.get(path);
			if (null == applicationMount) {
				final int lastSlash = path.lastIndexOf('/');
				if (lastSlash != -1) {
					// one dir up
					path = path.substring(0, lastSlash);
				} else {
					// abort
					path = null;
				}
			}
		} while ((null == applicationMount) && (null != path));
		return applicationMount;
	}

	/**
	 * Matches the port against integer keys in the specified map.
	 * 
	 * @param url
	 * @param mountsByPort
	 * @return result map if found, <code>null</code> otherwise
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public ConcurrentMap<String, ApplicationMount> matchPort(final URL url, final ConcurrentMap<Integer, ConcurrentMap<String, ApplicationMount>> mountsByPort) {
		ConcurrentMap<String, ApplicationMount> mountsByPath = null;

		// exact match first
		if (url.getPort() != -1) {
			// use specified port
			mountsByPath = mountsByPort.get(url.getPort());
		} else {
			// use default port if available
			if (url.getDefaultPort() != -1) {
				mountsByPath = mountsByPort.get(url.getDefaultPort());
			}
		}

		// fallback to global port if no exact match
		if (null == mountsByPath) {
			mountsByPath = mountsByPort.get(MATCH_ALL_PORT);
		}

		// return what we have
		return mountsByPath;
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
		modificationLock.lock();
		try {

			// get protocol
			final String protocol = url.getProtocol().toLowerCase().intern();
			ConcurrentMap<String, ConcurrentMap<Integer, ConcurrentMap<String, ApplicationMount>>> mountsByDomain = mountsByProtocol.putIfAbsent(protocol, new ConcurrentHashMap<String, ConcurrentMap<Integer, ConcurrentMap<String, ApplicationMount>>>());
			if (null == mountsByDomain) {
				mountsByDomain = mountsByProtocol.get(protocol);
			}

			// get domain
			final String domain = url.getHost().toLowerCase().intern();
			ConcurrentMap<Integer, ConcurrentMap<String, ApplicationMount>> mountsByPort = mountsByDomain.putIfAbsent(domain, new ConcurrentHashMap<Integer, ConcurrentMap<String, ApplicationMount>>(1));
			if (null == mountsByPort) {
				mountsByPort = mountsByDomain.get(domain);
			}

			// get port
			final Integer port = url.getPort();
			ConcurrentMap<String, ApplicationMount> mountsByPath = mountsByPort.putIfAbsent(port, new ConcurrentHashMap<String, ApplicationMount>());
			if (null == mountsByPath) {
				mountsByPath = mountsByPort.get(port);
			}

			// add mount if absent
			return mountsByPath.putIfAbsent(stripTrailingSlahes(url.getPath()).intern(), applicationMount);
		} finally {
			modificationLock.unlock();
		}
	}

	/**
	 * Removes an application mount mapping for the specified URL.
	 * 
	 * @param url
	 * @return the previous value associated with url, or <code>null</code> if
	 *         there was no mapping for the url
	 */
	public ApplicationMount remove(final URL url) {
		modificationLock.lock();
		try {
			// get protocol
			final ConcurrentMap<String, ConcurrentMap<Integer, ConcurrentMap<String, ApplicationMount>>> mountsByDomain = mountsByProtocol.get(url.getProtocol().toLowerCase().intern());
			if (null == mountsByDomain) {
				return null;
			}

			// get domain
			final ConcurrentMap<Integer, ConcurrentMap<String, ApplicationMount>> mountsByPort = mountsByDomain.get(url.getHost().toLowerCase().intern());
			if (null == mountsByPort) {
				return null;
			}

			// get port
			final ConcurrentMap<String, ApplicationMount> mountsByPath = mountsByPort.get(url.getPort());
			if (null == mountsByPath) {
				return null;
			}

			// remove mount from path
			final ApplicationMount applicationMount = mountsByPath.remove(stripTrailingSlahes(url.getPath()));
			if (null == applicationMount) {
				return null;
			}

			// cleanup port
			if (mountsByPath.isEmpty()) {
				mountsByPort.remove(url.getPort());
			}

			// cleanup domain
			if (mountsByPort.isEmpty()) {
				mountsByDomain.remove(url.getHost().intern());
			}

			// cleanup protocol
			if (mountsByDomain.isEmpty()) {
				mountsByProtocol.remove(url.getProtocol().intern());
			}

			// return removed mount
			return applicationMount;
		} finally {
			modificationLock.unlock();
		}
	}

	private String stripTrailingSlahes(String path) {
		// strip trailing slashes from the path
		while ((path.length() > 0) && (path.charAt(path.length() - 1) == '/')) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}
}
