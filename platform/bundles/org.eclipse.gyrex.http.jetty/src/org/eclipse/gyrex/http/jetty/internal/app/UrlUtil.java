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

public class UrlUtil {
	public static String getDomain(final URL url) {
		// domain matching is lower case
		return url.getHost().toLowerCase();
	}

	public static String getPath(final URL url) {
		// remove trailing slashes but preserve case
		return stripTrailingSlahes(url.getPath());
	}

	public static int getPort(final URL url) {
		// note, we always ignore the default port
		return url.getPort() != url.getDefaultPort() ? url.getPort() : -1;
	}

	public static String getProtocol(final URL url) {
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

	private UrlUtil() {
		// empty
	}

}
