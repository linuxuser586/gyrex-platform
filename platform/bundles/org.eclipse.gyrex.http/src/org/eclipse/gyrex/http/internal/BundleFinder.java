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
package org.eclipse.gyrex.http.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Finds the calling bundle
 */
class BundleFinder implements PrivilegedAction<List<Bundle>> {

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

	private final PackageAdmin packageAdmin;
	private final Bundle host;

	/**
	 * Creates a new instance.
	 */
	public BundleFinder(final PackageAdmin packageAdmin, final Bundle host) {
		this.packageAdmin = packageAdmin;
		this.host = host;
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
			final Bundle bundle = packageAdmin.getBundle(clazz);
			if ((null != bundle) && !bundle.equals(host)) {
				result.add(bundle);
				return result;
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see java.security.PrivilegedAction#run()
	 */
	@Override
	public List<Bundle> run() {
		return internalFindBundles();
	}

}
