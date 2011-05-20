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
package org.eclipse.gyrex.persistence.internal;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.persistence.internal.storage.ContentTypeTracker;
import org.eclipse.gyrex.persistence.internal.storage.RepositoryProviderRegistry;
import org.eclipse.gyrex.persistence.internal.storage.RepositoryRegistry;
import org.eclipse.gyrex.persistence.storage.registry.IRepositoryRegistry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * The persistence plug-in.
 */
public class PersistenceActivator extends BaseBundleActivator {

	/**
	 * the symbolic name (value
	 * <code>org.eclipse.gyrex.common.persistence</code>)
	 */
	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.persistence";

	/** the shared instance */
	private static PersistenceActivator sharedInstance;

	/**
	 * Returns the shared instance.
	 * 
	 * @return the shared instance
	 */
	public static PersistenceActivator getInstance() {
		final PersistenceActivator instance = sharedInstance;
		if (null == instance) {
			throw new IllegalStateException("The Persistence bundle has not been started.");
		}

		return instance;
	}

	/** the repository type registry */
	private volatile RepositoryProviderRegistry repositoryProviderRegistry;

	/** the repositories manager */
	private volatile RepositoryRegistry repositoryRegistry;

	/** tracker for content type */
	private volatile ContentTypeTracker contentTypeTracker;

	private ServiceRegistration repositoryRegistryRegistration;

	/**
	 * Creates a new instance.
	 * 
	 * @param pluginId
	 */
	public PersistenceActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		sharedInstance = this;

		// track content type
		contentTypeTracker = new ContentTypeTracker(context);
		contentTypeTracker.open();

		// register the type registry
		repositoryProviderRegistry = new RepositoryProviderRegistry();
		repositoryProviderRegistry.start(context);

		// start the repository registry
		repositoryRegistry = new RepositoryRegistry();
		repositoryRegistryRegistration = getServiceHelper().registerService(IRepositoryRegistry.class.getName(), repositoryRegistry, "Eclipse.org", "Gyrex Repository Registry", null, null);
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		sharedInstance = null;

		// unregister & stop repository registry
		repositoryRegistryRegistration.unregister();
		repositoryRegistryRegistration = null;
		repositoryRegistry.close();
		repositoryRegistry = null;

		// stop the type registry
		repositoryProviderRegistry.close();
		repositoryProviderRegistry = null;

		// un-track content types
		contentTypeTracker.close();
		contentTypeTracker = null;
	}

	/**
	 * Returns the contentTypeTracker.
	 * 
	 * @return the contentTypeTracker
	 */
	public ContentTypeTracker getContentTypeTracker() {
		final ContentTypeTracker tracker = contentTypeTracker;
		if (null == tracker) {
			throw createBundleInactiveException();
		}
		return tracker;
	}

	/**
	 * Returns the repositories manager.
	 * 
	 * @return the repositories manager
	 */
	public RepositoryRegistry getRepositoriesManager() {
		final RepositoryRegistry registry = repositoryRegistry;
		if (null == registry) {
			throw createBundleInactiveException();
		}

		return registry;
	}

	/**
	 * Returns the repository type registry.
	 * 
	 * @return the repository type registry
	 */
	public RepositoryProviderRegistry getRepositoryProviderRegistry() {
		final RepositoryProviderRegistry registry = repositoryProviderRegistry;
		if (null == registry) {
			throw createBundleInactiveException();
		}

		return registry;
	}

}
