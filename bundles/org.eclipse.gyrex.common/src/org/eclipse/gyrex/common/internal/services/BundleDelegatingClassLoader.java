/*******************************************************************************
 * Copyright (c) 2008, 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.common.internal.services;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.osgi.framework.Bundle;

/**
 * A class loader which delegates class loading to a bundle.
 */
class BundleDelegatingClassLoader extends ClassLoader {

	private final Bundle bundle;

	/**
	 * Creates a new instance.
	 * 
	 * @param bundle
	 */
	public BundleDelegatingClassLoader(final Bundle bundle) {
		this.bundle = bundle;
	}

	@Override
	public URL getResource(final String name) {
		return bundle.getResource(name);
	}

	@Override
	public Enumeration<URL> getResources(final String name) throws IOException {
		return bundle.getResources(name);
	}

	@Override
	public Class<?> loadClass(final String name) throws ClassNotFoundException {
		return bundle.loadClass(name);
	}
}