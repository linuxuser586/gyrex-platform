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
 * Serves static resources referenced by {@link DefaultErrorHandler}.
 */
public class DefaultErrorHandlerResourcesHandler extends AbstractHandler {

	static final String URI_ERROR_IMAGE = "/_error/error.gif";
	static final String URI_WARNING_IMAGE = "/_error/warning.gif";
	static final String URI_INFORMATION_IMAGE = "/_error/information.gif";
	static final String URI_ERROR_CSS = "/_error/error.css";

	static final String MIME_TYPE_ERROR_IMAGE = "image/gif";
	static final String MIME_TYPE_WARNING_IMAGE = "image/gif";
	static final String MIME_TYPE_INFORMATION_IMAGE = "image/gif";
	static final String MIME_TYPE_ERROR_CSS = " text/css;charset=UTF-8";

	private final long lastModified;
	private final byte[] errorCss;
	private final byte[] errorGif;
	private final byte[] informationGif;
	private final byte[] warningGif;

	/**
	 * Creates a new instance.
	 */
	public DefaultErrorHandlerResourcesHandler() {
		errorCss = HttpJettyActivator.readBundleResource("/images/error.css");
		errorGif = HttpJettyActivator.readBundleResource("/images/error.gif");
		warningGif = HttpJettyActivator.readBundleResource("/images/warning.gif");
		informationGif = HttpJettyActivator.readBundleResource("/images/information.gif");
		lastModified = (System.currentTimeMillis() / 1000) * 1000;
	}

	@Override
	public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
		// don't do anything if already processed
		if (response.isCommitted() || baseRequest.isHandled()) {
			return;
		}

		// check request method
		boolean headOnly = false;
		if (!HttpMethods.GET.equals(request.getMethod())) {
			if (!HttpMethods.HEAD.equals(request.getMethod())) {
				// don't handle non-GET or non-HEAD
				return;
			}
			headOnly = true;
		}

		// process matching requests
		String mimeType = null;
		byte[] resourceBytes = null;

		final String requestURI = request.getRequestURI();
		if (StringUtil.endsWithIgnoreCase(requestURI, URI_ERROR_CSS)) {
			mimeType = MIME_TYPE_ERROR_CSS;
			resourceBytes = errorCss;
		} else if (StringUtil.endsWithIgnoreCase(requestURI, URI_ERROR_IMAGE)) {
			mimeType = MIME_TYPE_ERROR_IMAGE;
			resourceBytes = errorGif;
		} else if (StringUtil.endsWithIgnoreCase(requestURI, URI_WARNING_IMAGE)) {
			mimeType = MIME_TYPE_WARNING_IMAGE;
			resourceBytes = warningGif;
		} else if (StringUtil.endsWithIgnoreCase(requestURI, URI_INFORMATION_IMAGE)) {
			mimeType = MIME_TYPE_INFORMATION_IMAGE;
			resourceBytes = informationGif;
		} else {
			// non-error resource
			return;
		}

		// mark as handled
		baseRequest.setHandled(true);

		// check modified
		if (request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE) == lastModified) {
			response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
		} else {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(mimeType);
			response.setContentLength(resourceBytes.length);
			response.setDateHeader(HttpHeaders.LAST_MODIFIED, lastModified);
			response.setHeader(HttpHeaders.CACHE_CONTROL, "max-age=360000,public");
			if (!headOnly) {
				response.getOutputStream().write(resourceBytes);
			}
		}
		return;
	}
}
