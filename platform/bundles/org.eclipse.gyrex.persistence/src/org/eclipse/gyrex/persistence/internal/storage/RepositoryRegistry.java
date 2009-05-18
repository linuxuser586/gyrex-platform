/*******************************************************************************
 * Copyright (c) 2008,2009 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.internal.storage;

import java.text.MessageFormat;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.gyrex.persistence.internal.PersistenceActivator;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;
import org.eclipse.gyrex.persistence.storage.registry.IRepositoryRegistry;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;
import org.eclipse.gyrex.preferences.PlatformScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * The platform repository manager stores repository information.
 */
public class RepositoryRegistry implements IRepositoryRegistry {

	/**
	 * A repository definition.
	 */
	private static class RepositoryDefinition {
		private static final String KEY_TYPE = "type";
		private static final String NODE_PROPERTIES = "properties";
		private final RepositoryDefinitionsStore store;
		private final String repositoryId;
		private RepositoryPreferences repositoryPreferences;

		/**
		 * Creates a new instance.
		 * 
		 * @param repositoryId
		 * @param repositoryType
		 */
		protected RepositoryDefinition(final String repositoryId, final RepositoryDefinitionsStore repositoryStore) {
			this.repositoryId = repositoryId;
			store = repositoryStore;
		}

		private void checkExist() {
			if (!exists()) {
				throw new IllegalStateException(MessageFormat.format("Repository definition for repository ''{0}'' does not exist!", repositoryId));
			}
		}

		public boolean exists() {
			try {
				return store.storage.nodeExists(repositoryId);
			} catch (final BackingStoreException e) {
				// ignore
				return false;
			}
		}

		private Preferences getNode() {
			return store.storage.node(repositoryId);
		}

		public IRepositoryPreferences getPreferences() {
			if (null != repositoryPreferences) {
				return repositoryPreferences;
			}
			final IEclipsePreferences eclipsePreferences = (IEclipsePreferences) getNode().node("data");
			final ISecurePreferences securePreferences = SecurePreferencesFactory.getDefault().node(PersistenceActivator.PLUGIN_ID + "/repositories/" + repositoryId + "/data");
			return repositoryPreferences = new RepositoryPreferences(securePreferences, eclipsePreferences);
		}

		public Dictionary<String, String> getProperties() {
			try {
				final String propertiesNodePath = repositoryId + "/" + NODE_PROPERTIES;
				if (!store.storage.nodeExists(propertiesNodePath)) {
					return null;
				}

				final Preferences node = store.storage.node(propertiesNodePath);
				final String[] keys = node.keys();
				final Dictionary<String, String> properties = new Hashtable<String, String>(keys.length);
				for (final String key : keys) {
					properties.put(key, node.get(key, ""));
				}
				return properties;
			} catch (final Exception e) {
				// TODO should log
				// ignore
				return null;
			}
		}

		public String getType() {
			checkExist();
			return getNode().get(KEY_TYPE, null);

		}

	}

	/**
	 * The repository definitions store.
	 */
	private static class RepositoryDefinitionsStore {

		private IEclipsePreferences storage;

		public RepositoryDefinition get(final String repositoryId) {
			if (null == storage) {
				throw new IllegalStateException("not loaded");
			}

			final RepositoryDefinition repositoryDefinition = new RepositoryDefinition(repositoryId, this);
			if (!repositoryDefinition.exists()) {
				return null;
			}

			return repositoryDefinition;
		}

		/**
		 * Loads the store
		 */
		public synchronized void load() {
			storage = (IEclipsePreferences) new PlatformScope().getNode(PersistenceActivator.PLUGIN_ID).node("repositories");
		}

	}

	private static void errorCreatingRepository(final RepositoryDefinition repositoryDef, final String detail) {
		throw new IllegalStateException(MessageFormat.format("Invalid repository definition ''{0}'': {1}", repositoryDef.repositoryId, detail));
	}

	private RepositoryDefinitionsStore repositoryDefinitionsStore;

	private final ConcurrentMap<String, Lock> locksByRepositoryId = new ConcurrentHashMap<String, Lock>(4);
	private final ConcurrentMap<String, Repository> repositoryCache = new ConcurrentHashMap<String, Repository>(4);

	/**
	 * Creates a repository from a definition
	 * 
	 * @param repositoryDef
	 * @return
	 * @throws IllegalStateException
	 *             if the repository could not be created
	 */
	private Repository createRepository(final RepositoryDefinition repositoryDef) throws IllegalStateException {
		final String type = repositoryDef.getType();
		if (null == type) {
			errorCreatingRepository(repositoryDef, "invalid type");
		}

		// get repository type
		final RepositoryProvider repositoryType = PersistenceActivator.getInstance().getRepositoryProviderRegistry().getRepositoryProvider(type);

		// get repository settings
		final IRepositoryPreferences repositoryPreferences = repositoryDef.getPreferences();

		// create repository instance
		final Repository repository = repositoryType.createRepositoryInstance(repositoryDef.repositoryId, repositoryPreferences);
		if (null == repository) {
			errorCreatingRepository(repositoryDef, MessageFormat.format("repository type ''{0}'' returned no repository instance", type));
		}

		return repository;
	}

	private Lock getOrCreateRepositoryLock(final String repositoryId) {
		Lock lock = locksByRepositoryId.get(repositoryId);
		if (lock == null) {
			final Lock newLock = new ReentrantLock();
			lock = locksByRepositoryId.putIfAbsent(repositoryId, newLock);
			if (lock == null) {
				// put succeeded, use new value
				lock = newLock;
			}
		}
		return lock;
	}

	/**
	 * Returns the repository with the specified id.
	 * 
	 * @param repositoryId
	 *            the repository id.
	 * @return the repository
	 * @throws IllegalStateException
	 *             if a repository with the specified id is not available
	 */
	public Repository getRepository(final String repositoryId) throws IllegalStateException {
		if (null == repositoryId) {
			throw new IllegalArgumentException("repository id must not be null");
		}

		// lookup a cached instance
		Repository repository = repositoryCache.get(repositoryId.intern());
		if (null != repository) {
			return repository;
		}

		// open store if necessary
		if (null == repositoryDefinitionsStore) {
			open();
		}

		// get repository definition
		final RepositoryDefinition repositoryDef = repositoryDefinitionsStore.get(repositoryId);
		if (null == repositoryDef) {
			throw new IllegalStateException(MessageFormat.format("The repository with id \"{0}\" could not be found!", repositoryId));
		}

		// create a new instance
		final Lock repositoryCreationLock = getOrCreateRepositoryLock(repositoryId);
		repositoryCreationLock.lock();
		try {
			// make sure the cache is empty
			repository = repositoryCache.get(repositoryId.intern());
			if (null != repository) {
				// use cached repository
				return repository;
			}

			// create the repository
			repository = createRepository(repositoryDef);

			// put the repository instance in the cache
			repositoryCache.put(repositoryId.intern(), repository);

			// return the repository
			return repository;
		} finally {
			repositoryCreationLock.unlock();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.persistence.storage.registry.IRepositoryRegistry#getRepositoryPreferences(java.lang.String)
	 */
	@Override
	public IRepositoryPreferences getRepositoryPreferences(final String repositoryId) {
		if (null == repositoryId) {
			throw new IllegalArgumentException("repository id must not be null");
		}

		// open store if necessary
		if (null == repositoryDefinitionsStore) {
			open();
		}

		// get repository definition
		final RepositoryDefinition repositoryDef = repositoryDefinitionsStore.get(repositoryId);
		if (null == repositoryDef) {
			return null;
		}

		return repositoryDef.getPreferences();
	}

	private synchronized void open() {
		if (null != repositoryDefinitionsStore) {
			return;
		}

		repositoryDefinitionsStore = new RepositoryDefinitionsStore();
		repositoryDefinitionsStore.load();
	}

}
