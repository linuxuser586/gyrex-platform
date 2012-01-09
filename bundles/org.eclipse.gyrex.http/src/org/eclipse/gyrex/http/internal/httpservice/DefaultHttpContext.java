/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.internal.httpservice;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

public class DefaultHttpContext implements HttpContext {

	private final Bundle bundle;

	public DefaultHttpContext(final Bundle bundle) {
		this.bundle = bundle;
	}

	@Override
	public String getMimeType(final String name) {
		return null;
	}

	@Override
	public URL getResource(final String name) {
		return bundle.getResource(name);
	}

	@Override
	public boolean handleSecurity(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		// default behaviour assumes the container has already performed authentication
		return true;
	}
}
