/*******************************************************************************
 * Copyright (c) 2008 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Cognos Incorporated, IBM Corporation - concept/implementation from
 *                                            org.eclipse.equinox.http.registry
 *     Gunnar Wagenknecht - adaption to Gyrex
 *******************************************************************************/
package org.eclipse.gyrex.http.registry.internal;

import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.eclipse.gyrex.http.application.context.IResourceProvider;

import org.osgi.framework.Bundle;

public class BundleResourceProvider implements IResourceProvider {
	private final Bundle bundle;
	private Properties mimeMappings;

	/**
	 * Creates a new instance.
	 */
	public BundleResourceProvider(final Bundle bundle) {
		this.bundle = bundle;
	}

	public String getMimeType(final String name) {
		if (mimeMappings != null) {
			final int dotIndex = name.lastIndexOf('.');
			if (dotIndex != -1) {
				final String mimeExtension = name.substring(dotIndex + 1);
				final String mimeType = mimeMappings.getProperty(mimeExtension);
				if (mimeType != null) {
					return mimeType;
				}
			}
		}
		return null;
	}

	public URL getResource(final String name) {
		return bundle.getEntry(name);
	}

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