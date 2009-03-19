/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.internal.application;

/**
 * internal constants
 */
public interface IApplicationServletConstants {

	String INTERNAL_HEADER_PREFIX = "X-Gyrex-";//$NON-NLS-1$
	String INTERNAL_ATTR_PREFIX = "org.eclipse.gyrex.http.internal.";//$NON-NLS-1$

	String INTERNAL_HEADER_ORIGINAL_URL = "X-Gyrex-OriginalURL"; //$NON-NLS-1$
	String INTERNAL_HEADER_CLIENT_IP = "X-Gyrex-ClientIP"; //$NON-NLS-1$
	String INTERNAL_HEADER_CLIENT_PORT = "X-Gyrex-ClientPort"; //$NON-NLS-1$

	String INTERNAL_ATTR_APP_REGISTRATION = INTERNAL_ATTR_PREFIX + "application.registration";//$NON-NLS-1$
	String INTERNAL_ATTR_APP_MOUNT = INTERNAL_ATTR_PREFIX + "application.mount";//$NON-NLS-1$
	String INTERNAL_ATTR_REQUEST_URL = INTERNAL_ATTR_PREFIX + "request.url";//$NON-NLS-1$
	String INTERNAL_ATTR_REQUEST_URI = INTERNAL_ATTR_PREFIX + "request.uri";//$NON-NLS-1$
	String INTERNAL_ATTR_CONTEXT_PATH = INTERNAL_ATTR_PREFIX + "context.path";//$NON-NLS-1$
	String INTERNAL_ATTR_PATH_INFO = INTERNAL_ATTR_PREFIX + "path.info";//$NON-NLS-1$
	String INTERNAL_ATTR_QUERY_STRING = INTERNAL_ATTR_PREFIX + "query.string";//$NON-NLS-1$

	String INTERNAL_ATTR_FORWARDED = INTERNAL_ATTR_PREFIX + "forwarded";//$NON-NLS-1$
	String INTERNAL_ATTR_INCLUDED = INTERNAL_ATTR_PREFIX + "included";//$NON-NLS-1$

	String REQ_ATTR_INCLUDE_REQUEST_URI = "javax.servlet.include.request_uri"; //$NON-NLS-1$
	String REQ_ATTR_INCLUDE_CONTEXT_PATH = "javax.servlet.include.context_path"; //$NON-NLS-1$
	String REQ_ATTR_INCLUDE_SERVLET_PATH = "javax.servlet.include.servlet_path"; //$NON-NLS-1$
	String REQ_ATTR_INCLUDE_PATH_INFO = "javax.servlet.include.path_info"; //$NON-NLS-1$
	String REQ_ATTR_INCLUDE_QUERY_STRING = "javax.servlet.include.query_string"; //$NON-NLS-1$

	String REQ_ATTR_FORWARD_REQUEST_URI = "javax.servlet.include.request_uri"; //$NON-NLS-1$
	String REQ_ATTR_FORWARD_CONTEXT_PATH = "javax.servlet.include.context_path"; //$NON-NLS-1$
	String REQ_ATTR_FORWARD_SERVLET_PATH = "javax.servlet.include.servlet_path"; //$NON-NLS-1$
	String REQ_ATTR_FORWARD_PATH_INFO = "javax.servlet.include.path_info"; //$NON-NLS-1$
	String REQ_ATTR_FORWARD_QUERY_STRING = "javax.servlet.include.query_string"; //$NON-NLS-1$
}
