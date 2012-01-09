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
package org.eclipse.gyrex.http.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Finds the calling bundle
 */
public class BundleFinder implements PrivilegedAction<List<Bundle>> {

	static final class Finder extends SecurityManager {
		@Override
		public Class[] getClassContext() {
			return super.getClassContext();
		}
	}

	static final Finder contextFinder = AccessController.doPrivileged(new PrivilegedAction<Finder>() {
		public Finder run() {
			return new Finder();
		}
	});

	private final Bundle host;

	private final Bundle bundleFinderHost;

	/**
	 * Creates a new instance.
	 */
	public BundleFinder(final Bundle host) {
		this.host = host;
		bundleFinderHost = FrameworkUtil.getBundle(getClass());
	}

	private List<Bundle> findBundles() {
		if (System.getSecurityManager() == null) {
			return internalFindBundles();
		}
		return AccessController.doPrivileged(this);
	}

	public Bundle getCallingBundle() {
		final List<Bundle> bundles = findBundles();
		if (bundles.isEmpty()) {
			return null;
		}
		return bundles.iterator().next();
	}

	private List<Bundle> internalFindBundles() {
		final Class[] stack = contextFinder.getClassContext();
		final List<Bundle> result = new ArrayList<Bundle>(1);
		for (final Class clazz : stack) {
			final Bundle bundle = FrameworkUtil.getBundle(clazz);
			if ((null != bundle) && !bundle.equals(host) && !bundle.equals(bundleFinderHost)) {
				result.add(bundle);
				return result;
			}
		}
		return result;
	}

	@Override
	public List<Bundle> run() {
		return internalFindBundles();
	}

}
