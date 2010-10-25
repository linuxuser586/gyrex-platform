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
package org.eclipse.gyrex.persistence.internal;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.configuration.PlatformConfiguration;
import org.eclipse.gyrex.persistence.internal.storage.RepositoryProviderRegistry;
import org.eclipse.gyrex.persistence.internal.storage.RepositoryRegistry;
import org.eclipse.gyrex.persistence.storage.registry.IRepositoryRegistry;
import org.osgi.framework.BundleContext;

/**
 * The persistence plug-in.
 */
public class PersistenceActivator extends BaseBundleActivator {

	/**
	 * the plug-in id (value
	 * <code>org.eclipse.gyrex.common.persistence</code>)
	 */
	public static final String PLUGIN_ID = "org.eclipse.gyrex.persistence";

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
	private RepositoryProviderRegistry repositoryProviderRegistry;

	/** the repositories manager */
	private RepositoryRegistry repositoryRegistry;

	/**
	 * Creates a new instance.
	 * 
	 * @param pluginId
	 */
	public PersistenceActivator() {
		super(PLUGIN_ID);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStart(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStart(final BundleContext context) throws Exception {
		sharedInstance = this;

		// register the type registry
		startRepositoryTypeRegistry(context);

		// start the repository registry
		startRepositoryRegistry();

		// TODO: initialize development defaults
		if (PlatformConfiguration.isOperatingInDevelopmentMode()) {
			//final IEclipsePreferences defaultPreferences = new DefaultScope().getNode(PLUGIN_ID);
			//defaultPreferences.put(IPersistenceConstants.PREF_KEY_SINGLE_REPOSITORY_ID, "local");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStop(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStop(final BundleContext context) throws Exception {
		sharedInstance = null;

		// stop the manager
		stopRepositoryRegistry();

		// stop the type registry
		stopRepositoryTypeRegistry();
	}

	/**
	 * Returns the repositories manager.
	 * 
	 * @return the repositories manager
	 */
	public RepositoryRegistry getRepositoriesManager() {
		return repositoryRegistry;
	}

	/**
	 * Returns the repository type registry.
	 * 
	 * @return the repository type registry
	 */
	public RepositoryProviderRegistry getRepositoryProviderRegistry() {
		final RepositoryProviderRegistry registry = repositoryProviderRegistry;
		if (null == registry) {
			throw new IllegalStateException("inactive");
		}

		return registry;
	}

	private synchronized void startRepositoryRegistry() {
		if (null != repositoryRegistry) {
			return;
		}

		repositoryRegistry = new RepositoryRegistry();

		// register service
		getServiceHelper().registerService(IRepositoryRegistry.class.getName(), repositoryRegistry, "Eclipse.org", "Gyrex Repository Registry", null, null);
	}

	private synchronized void startRepositoryTypeRegistry(final BundleContext context) {
		if (null == repositoryProviderRegistry) {
			repositoryProviderRegistry = new RepositoryProviderRegistry();
		}
		repositoryProviderRegistry.start(context);
	}

	private synchronized void stopRepositoryRegistry() {
		if (null == repositoryRegistry) {
			return;
		}

		repositoryRegistry = null;
	}

	private synchronized void stopRepositoryTypeRegistry() {
		if (null != repositoryProviderRegistry) {
			repositoryProviderRegistry.close();
			repositoryProviderRegistry = null;
		}
	}
}
