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

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationProviderRegistry;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

public class HttpActivator extends BaseBundleActivator {

	/** PLUGIN_ID */
	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.http";

	/** server type for the default web server */
	public static final String TYPE_WEB = SYMBOLIC_NAME + ".default";

	/** the shared instance */
	private static HttpActivator sharedInstance;

	/**
	 * Returns the instance.
	 * 
	 * @return the instance
	 */
	public static HttpActivator getInstance() throws IllegalStateException {
		final HttpActivator instance = sharedInstance;
		if (null == instance) {
			throw new IllegalStateException("Gyrex HTTP Core has not been started.");
		}
		return instance;
	}

	/**
	 * Returns the bundle version.
	 * <p>
	 * This method doesn't fail if the plug-in is inactive.
	 * </p>
	 * 
	 * @return the bundle version
	 */
	public static String getVersion() {
		try {
			return getInstance().getBundleVersion().toString();
		} catch (final RuntimeException e) {
			// don't fail
			return Version.emptyVersion.toString();
		}
	}

	private volatile ApplicationProviderRegistry providerRegistry;

	/**
	 * Creates a new instance.
	 * <p>
	 * Note, this constructor is called by the OSGi framework. It is not
	 * intended to be called by clients.
	 * </p>
	 */
	public HttpActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected synchronized void doStart(final BundleContext context) throws Exception {
		sharedInstance = this;

		// track the available providers
		providerRegistry = new ApplicationProviderRegistry(context);
		providerRegistry.open();

	}

	@Override
	protected synchronized void doStop(final BundleContext context) throws Exception {
		// unset instance
		sharedInstance = null;

		// stop provider tracker
		providerRegistry.close();
		providerRegistry = null;
	}

	/**
	 * Returns the bundle that is calling us.
	 * 
	 * @return the bundle or <code>null</code>
	 */
	// TODO implement security
	public Bundle getCallingBundle() {
		final Bundle bundle = getBundle();
		if (null == bundle) {
			return null;
		}

		return new BundleFinder(bundle).getCallingBundle();
	}

	@Override
	protected Class getDebugOptions() {
		return HttpDebug.class;
	}

	/**
	 * Returns the providerRegistry.
	 * 
	 * @return the providerRegistry
	 */
	public ApplicationProviderRegistry getProviderRegistry() {
		final ApplicationProviderRegistry registry = providerRegistry;
		if (null == registry) {
			throw createBundleInactiveException();
		}
		return registry;
	}

}
