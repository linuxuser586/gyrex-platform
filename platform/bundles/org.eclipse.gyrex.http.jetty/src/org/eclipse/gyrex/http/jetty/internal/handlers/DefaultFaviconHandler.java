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
package org.eclipse.gyrex.http.jetty.internal.handlers;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.gyrex.http.jetty.internal.HttpJettyActivator;

import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.StringUtil;

/**
 * Serves a default favicon.ico.
 */
public class DefaultFaviconHandler extends AbstractHandler {

	private static final String FAVICON_ICO_URI = "/favicon.ico";

	private final byte[] iconBytes;
	private final long iconModified;

	/**
	 * Creates a new instance.
	 */
	public DefaultFaviconHandler() {
		iconBytes = HttpJettyActivator.readBundleResource("/images/eclipse.ico");
		iconModified = (System.currentTimeMillis() / 1000) * 1000;
	}

	@Override
	public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
		// don't do anything if already processed
		if (response.isCommitted() || baseRequest.isHandled()) {
			return;
		}

		// process favicon requests
		if ((null != iconBytes) && HttpMethods.GET.equals(request.getMethod()) && StringUtil.endsWithIgnoreCase(request.getRequestURI(), FAVICON_ICO_URI)) {
			baseRequest.setHandled(true);
			if (request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE) == iconModified) {
				response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			} else {
				response.setStatus(HttpServletResponse.SC_OK);
				response.setContentType("image/x-icon");
				response.setContentLength(iconBytes.length);
				response.setDateHeader(HttpHeaders.LAST_MODIFIED, iconModified);
				response.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=360000,public");
				response.getOutputStream().write(iconBytes);
			}
			return;
		}
	}
}
