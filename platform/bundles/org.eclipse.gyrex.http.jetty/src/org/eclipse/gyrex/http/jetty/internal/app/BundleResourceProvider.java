/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.jetty.internal.app;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.gyrex.http.application.context.IResourceProvider;

import org.osgi.framework.Bundle;

/**
 * A simple resource provider relying on a bundle.
 */
public class BundleResourceProvider implements IResourceProvider {

	private final Bundle bundle;

	/**
	 * Creates a new instance.
	 */
	public BundleResourceProvider(final Bundle bundle) {
		this.bundle = bundle;
	}

	@Override
	public URL getResource(final String path) throws MalformedURLException {
		return bundle.getEntry(path);
	}

	@Override
	public Set<String> getResourcePaths(final String path) {
		final Enumeration<String> entryPaths = bundle.getEntryPaths(path);
		if (entryPaths == null) {
			return null;
		}
		final HashSet<String> result = new HashSet<String>();
		while (entryPaths.hasMoreElements()) {
			result.add(entryPaths.nextElement());
		}
		return result;
	}

}
