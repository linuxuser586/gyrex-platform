/*******************************************************************************
 * Copyright (c) 2008, 2009 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.helper;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpContext;

/**
 * Base <code>{@link HttpContext}</code> implementation that wraps a default
 * context and provides support for additional features like mapping.
 */
public class BaseHttpContext implements HttpContext {

	/** a wrapped default context */
	private final HttpContext defaultContext;

	/** a name mapping */
	private final Map<String, String> nameMapping;

	/**
	 * Creates a new context wrapping an existing default context.
	 * 
	 * @param defaultContext
	 *            the default context to wrap
	 * @param nameMapping
	 *            the name mapping to be used
	 * @see #mapName(String)
	 */
	public BaseHttpContext(final HttpContext defaultContext, final Map<String, String> nameMapping) {
		this.defaultContext = defaultContext;
		this.nameMapping = nameMapping;
	}

	/**
	 * Maps a name to a MIME type by delegating to the wrapped default context.
	 * <p>
	 * Before the default context is called the specified name will be
	 * {@link #mapName(String) mapped}.
	 * </p>
	 * 
	 * @param name
	 *            the name to determine the MIME type for
	 * @see org.osgi.service.http.HttpContext#getMimeType(java.lang.String)
	 */
	public String getMimeType(final String name) {
		return defaultContext.getMimeType(mapName(name));
	}

	/**
	 * Maps a resource name to a URL by delegating to the wrapped default
	 * context.
	 * <p>
	 * Before the default context is called the specified name will be
	 * {@link #mapName(String) mapped}.
	 * </p>
	 * 
	 * @param name
	 *            the name to determine the MIME type for
	 * @see org.osgi.service.http.HttpContext#getResource(java.lang.String)
	 */
	public URL getResource(final String name) {
		return defaultContext.getResource(mapName(name));
	}

	/**
	 * Handles security for the specified request by delegating to the wrapped
	 * default context.
	 * 
	 * @param request
	 *            the HTTP request
	 * @param response
	 *            the HTTP response
	 * @return <code>true</code> if the request should be serviced,
	 *         <code>false</code> if the request should not be serviced and
	 *         Http Service will send the response back to the client.
	 * @see org.osgi.service.http.HttpContext#handleSecurity(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	public boolean handleSecurity(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		return defaultContext.handleSecurity(request, response);
	}

	/**
	 * Maps the specified name according to the configured name mapping.
	 * <p>
	 * This method is called by all HTTP context methods before delegating to
	 * the wrapped default context.
	 * </p>
	 * 
	 * @param name
	 *            the name to map
	 * @return the mapped name if a mapping is available, otherwise the original
	 *         name is returned
	 * @see #getMimeType(String)
	 * @see #getResource(String)
	 */
	protected String mapName(String name) {
		if ((null != name) && nameMapping.containsKey(name)) {
			name = nameMapping.get(name);
		}

		if (null == name) {
			return "";
		}

		return name;
	}

}
