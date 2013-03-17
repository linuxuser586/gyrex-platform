/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.gyrex.http.application.context.IResourceProvider;

import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

public class DefaultResourceProvider implements IResourceProvider {

	private final Bundle bundle;
	private final HttpContext context;

	public DefaultResourceProvider(final Bundle bundle, final HttpContext context) {
		this.bundle = bundle;
		this.context = context;
	}

	@Override
	public URL getResource(final String path) throws MalformedURLException {
		if (null != context) {
			return context.getResource(path);
		}
		return bundle.getEntry(path);
	}

	@Override
	public Set<String> getResourcePaths(final String path) {
		final Enumeration entryPaths = bundle.getEntryPaths(path);
		if (entryPaths == null) {
			return null;
		}

		final Set<String> result = new HashSet<String>();
		while (entryPaths.hasMoreElements()) {
			result.add((String) entryPaths.nextElement());
		}
		return result;
	}

}
