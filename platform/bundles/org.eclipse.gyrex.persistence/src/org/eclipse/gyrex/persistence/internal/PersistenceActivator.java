/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.cloudfree.persistence.internal;


import org.eclipse.cloudfree.common.runtime.BaseBundleActivator;
import org.eclipse.cloudfree.configuration.PlatformConfiguration;
import org.eclipse.cloudfree.persistence.internal.storage.RepositoriesManager;
import org.eclipse.cloudfree.persistence.internal.storage.type.RepositoryTypeRegistry;
import org.osgi.framework.BundleContext;

/**
 * The persistence plug-in.
 */
public class PersistenceActivator extends BaseBundleActivator {

	/** the plug-in id (value <code>org.eclipse.cloudfree.common.persistence</code>) */
	public static final String PLUGIN_ID = "org.eclipse.cloudfree.persistence";

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
	private RepositoryTypeRegistry repositoryTypeRegistry;

	/** the repositories manager */
	private RepositoriesManager repositoriesManager;

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
	 * @see org.eclipse.cloudfree.common.runtime.BaseBundleActivator#doStart(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStart(final BundleContext context) throws Exception {
		sharedInstance = this;

		// register the type registry
		startRepositoryTypeRegistry(context);

		// start the repositories manager
		startRepositoriesManager();

		// TODO: initialize development defaults
		if (PlatformConfiguration.isOperatingInDevelopmentMode()) {
			//final IEclipsePreferences defaultPreferences = new DefaultScope().getNode(PLUGIN_ID);
			//defaultPreferences.put(IPersistenceConstants.PREF_KEY_SINGLE_REPOSITORY_ID, "local");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cloudfree.common.runtime.BaseBundleActivator#doStop(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStop(final BundleContext context) throws Exception {
		sharedInstance = null;

		// stop the manager
		stopRepositoriesManager();

		// stop the type registry
		stopRepositoryTypeRegistry();
	}

	/**
	 * Returns the repositories manager.
	 * 
	 * @return the repositories manager
	 */
	public RepositoriesManager getRepositoriesManager() {
		return repositoriesManager;
	}

	/**
	 * Returns the repository type registry.
	 * 
	 * @return the repository type registry
	 */
	public RepositoryTypeRegistry getRepositoryTypeRegistry() {
		final RepositoryTypeRegistry registry = repositoryTypeRegistry;
		if (null == registry) {
			throw new IllegalStateException("inactive");
		}

		return registry;
	}

	private synchronized void startRepositoriesManager() {
		if (null != repositoriesManager) {
			return;
		}

		repositoriesManager = new RepositoriesManager();
	}

	private synchronized void startRepositoryTypeRegistry(final BundleContext context) {
		if (null == repositoryTypeRegistry) {
			repositoryTypeRegistry = new RepositoryTypeRegistry();
		}
		repositoryTypeRegistry.start(context);
	}

	private synchronized void stopRepositoriesManager() {
		if (null == repositoriesManager) {
			return;
		}

		repositoriesManager = null;
	}

	private synchronized void stopRepositoryTypeRegistry() {
		if (null != repositoryTypeRegistry) {
			repositoryTypeRegistry.close();
			repositoryTypeRegistry = null;
		}
	}
}
