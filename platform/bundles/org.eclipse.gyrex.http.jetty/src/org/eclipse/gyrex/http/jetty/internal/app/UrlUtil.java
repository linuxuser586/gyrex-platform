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

import java.net.URL;

import org.eclipse.jetty.util.URIUtil;

public class UrlUtil {
	public static String getNormalizedDomain(final URL url) {
		return normalizeDomain(url.getHost());
	}

	public static String getNormalizedPath(final URL url) {
		// remove trailing slashes but preserve case
		return stripTrailingSlahes(url.getPath());
	}

	public static int getNormalizedPort(final URL url) {
		// we never return the protocols default port
		// and we rely on "-1" as the default port
		return url.getPort() != url.getDefaultPort() ? url.getPort() : -1;
	}

	public static String getNormalizedProtocol(final URL url) {
		// protocol matching is lower case
		return url.getProtocol().toLowerCase();
	}

	public static boolean isDefaultPort(final int port, final String protocol) {
		switch (port) {
			case 80:
				return URIUtil.HTTP.equals(protocol);
			case 443:
				return URIUtil.HTTPS.equals(protocol);

			case 0:
			case -1:
				return true;

			default:
				return false;
		}
	}

	public static String normalizeDomain(final String domain) {
		if (domain == null) {
			return null;
		}

		if (domain.endsWith(".")) {
			return domain.substring(0, domain.length() - 1);
		}

		// domain matching is lower case
		return domain.toLowerCase();
	}

	private static String stripTrailingSlahes(String path) {
		// strip trailing slashes from the path
		while ((path.length() > 0) && (path.charAt(path.length() - 1) == '/')) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

	private UrlUtil() {
		// empty
	}

}
