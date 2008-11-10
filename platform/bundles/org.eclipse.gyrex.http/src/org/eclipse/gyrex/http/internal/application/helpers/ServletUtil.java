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
package org.eclipse.cloudfree.http.internal.application.helpers;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.cloudfree.http.internal.application.IApplicationServletConstants;

/**
 * Utility class for working with the servlet API.
 */
public class ServletUtil {

	/**
	 * Returns the path info from the request.
	 * <p>
	 * If the request is an include request the path info set as include
	 * property will be used.
	 * </p>
	 * 
	 * @param req
	 *            the request
	 * @return the path info
	 */
	public static String getPathInfo(final HttpServletRequest req) {
		if (req.getAttribute(IApplicationServletConstants.REQ_ATTR_INCLUDE_REQUEST_URI) != null) {
			return (String) req.getAttribute(IApplicationServletConstants.REQ_ATTR_INCLUDE_PATH_INFO);
		}

		return req.getPathInfo();
	}

	private ServletUtil() {
		// empty
	}

}
