/*******************************************************************************
 * Copyright (c) 2011, 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.p2.internal;

import java.net.URL;
import java.util.Collection;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.service.datalocation.Location;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class P2Activator extends BaseBundleActivator {

	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.p2";
	private static volatile P2Activator instance;

	/**
	 * Returns the instance.
	 * 
	 * @return the instance
	 */
	public static P2Activator getInstance() {
		final P2Activator activator = instance;
		if (activator == null)
			throw new IllegalArgumentException("inactive");
		return activator;
	}

	private volatile PackageManager packageManager;
	private volatile RepoManager repoManager;
	private volatile IPath configLocationPath;

	/**
	 * Creates a new instance.
	 */
	public P2Activator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instance = this;
		packageManager = new PackageManager();
		repoManager = new RepoManager();
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instance = null;
		packageManager = null;
		repoManager = null;
	}

	public IPath getConfigLocation() {
		if (configLocationPath != null)
			return configLocationPath;
		final BundleContext context = getBundle().getBundleContext();
		Collection<ServiceReference<Location>> serviceReferences;
		try {
			serviceReferences = context.getServiceReferences(Location.class, Location.CONFIGURATION_FILTER);
		} catch (final InvalidSyntaxException e) {
			throw new IllegalStateException("error determining configuration location: " + e.getMessage(), e);
		}
		final Location userConfigLocation = context.getService(serviceReferences.iterator().next());
		if (userConfigLocation.isReadOnly())
			throw new IllegalStateException("config location is read-only");
		final URL url = userConfigLocation.getURL();
		if (url == null)
			throw new IllegalStateException("config location not available");
		if (!url.getProtocol().equals("file"))
			throw new IllegalStateException("config location must be on local file system");
		return configLocationPath = new Path(url.getPath());
	}

	@Override
	protected Class getDebugOptions() {
		return P2Debug.class;
	}

	public PackageManager getPackageManager() {
		final PackageManager manager = packageManager;
		if (manager == null)
			throw createBundleInactiveException();
		return manager;
	}

	public RepoManager getRepositoryManager() {
		final RepoManager manager = repoManager;
		if (manager == null)
			throw createBundleInactiveException();
		return manager;
	}

}
