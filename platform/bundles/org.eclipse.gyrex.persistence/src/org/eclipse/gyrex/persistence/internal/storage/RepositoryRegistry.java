/*******************************************************************************
 * Copyright (c) 2008, 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *     Mike Tschierschke - customization for rap based admin ui,
 *     						added implementation for method getRepositoryDefinition
 *******************************************************************************/
package org.eclipse.gyrex.persistence.internal.storage;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.gyrex.persistence.internal.PersistenceActivator;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;
import org.eclipse.gyrex.persistence.storage.registry.IRepositoryDefinition;
import org.eclipse.gyrex.persistence.storage.registry.IRepositoryRegistry;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;
import org.eclipse.gyrex.preferences.PlatformScope;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.util.NLS;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;

/**
 * The platform repository manager stores repository information.
 */
public class RepositoryRegistry implements IRepositoryRegistry {

	/**
	 * A repository definition.
	 */
	private static class RepositoryDefinition implements IRepositoryDefinition {

		private static final String KEY_TYPE = "type";

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

		public void create(final String providerId) {
			if (exists()) {
				throw new IllegalStateException(NLS.bind("Repository definition for repository ''{0}'' already exist!", repositoryId));
			}
			final Preferences node = getNode();
			node.put(KEY_TYPE, providerId);
			try {
				node.flush();
			} catch (final BackingStoreException e) {
				throw new IllegalStateException(NLS.bind("Error creating repository definition ''{0}'': {1}", repositoryId, e.getMessage()));
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

		@Override
		public String getProviderId() {
			checkExist();
			return getNode().get(KEY_TYPE, null);
		}

		@Override
		public IRepositoryPreferences getRepositoryPreferences() {
			if (null != repositoryPreferences) {
				return repositoryPreferences;
			}

			return repositoryPreferences = new RepositoryPreferences((IEclipsePreferences) getNode().node("data"));
		}

		public void remove() {
			if (!exists()) {
				throw new IllegalStateException(NLS.bind("Repository definition for repository ''{0}'' does not exist!", repositoryId));
			}
			final Preferences node = getNode();
			final Preferences parent = node.parent();
			try {
				node.removeNode();
				parent.flush();
			} catch (final BackingStoreException e) {
				throw new IllegalStateException(NLS.bind("Error removing repository definition ''{0}'': {1}", repositoryId, e.getMessage()));
			}
		}

	}

	/**
	 * The repository definitions store.
	 */
	private static class RepositoryDefinitionsStore {

		private IEclipsePreferences storage;

		public void create(final String repositoryId, final String repositoryProviderId) {
			final RepositoryDefinition repositoryDefinition = new RepositoryDefinition(repositoryId, this);
			repositoryDefinition.create(repositoryProviderId);
		}

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

		/**
		 * @param repositoryId
		 */
		public void remove(final String repositoryId) {
			if (null == storage) {
				throw new IllegalStateException("not loaded");
			}

			final RepositoryDefinition repositoryDefinition = new RepositoryDefinition(repositoryId, this);
			if (repositoryDefinition.exists()) {
				repositoryDefinition.remove();
			}

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
		final String repositoryProviderId = repositoryDef.getProviderId();
		if (null == repositoryProviderId) {
			errorCreatingRepository(repositoryDef, "invalid type");
		}

		// get repository type
		final RepositoryProvider repositoryType = PersistenceActivator.getInstance().getRepositoryProviderRegistry().getRepositoryProvider(repositoryProviderId);

		// get repository settings
		final IRepositoryPreferences repositoryPreferences = repositoryDef.getRepositoryPreferences();

		// create repository instance
		final Repository repository = repositoryType.createRepositoryInstance(repositoryDef.repositoryId, repositoryPreferences);
		if (null == repository) {
			errorCreatingRepository(repositoryDef, MessageFormat.format("repository type ''{0}'' returned no repository instance", repositoryProviderId));
		}

		return repository;
	}

	@Override
	public IRepositoryPreferences createRepository(final String repositoryId, final String repositoryProviderId) throws IllegalArgumentException {
		if (!Repository.isValidId(repositoryId)) {
			throw new IllegalArgumentException("repository id is not valid");
		}
		if (StringUtils.isBlank(repositoryProviderId)) {
			throw new IllegalArgumentException("repository id must not be null");
		}

		// open store if necessary
		if (null == repositoryDefinitionsStore) {
			open();
		}

		// create
		repositoryDefinitionsStore.create(repositoryId, repositoryProviderId);

		// load prefs
		return getRepositoryDefinition(repositoryId).getRepositoryPreferences();
	}

	@Override
	public List<String> getAllRegisteredRepositories() {
		final List<String> toReturn = new ArrayList<String>();

		// open store if necessary
		if (null == repositoryDefinitionsStore) {
			open();
		}

		try {
			final String[] childrenNames = repositoryDefinitionsStore.storage.childrenNames();
			for (final String id : childrenNames) {
				toReturn.add(id);
			}
			return Collections.unmodifiableList(toReturn);
		} catch (final Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}

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
		Repository repository = repositoryCache.get(repositoryId);
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
			repository = repositoryCache.get(repositoryId);
			if (null != repository) {
				// use cached repository
				return repository;
			}

			// create the repository
			repository = createRepository(repositoryDef);

			// put the repository instance in the cache
			repositoryCache.put(repositoryId, repository);

			// return the repository
			return repository;
		} finally {
			repositoryCreationLock.unlock();
		}
	}

	@Override
	public IRepositoryDefinition getRepositoryDefinition(final String repositoryId) {
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

		return repositoryDef;
	}

	private synchronized void open() {
		if (null != repositoryDefinitionsStore) {
			return;
		}

		repositoryDefinitionsStore = new RepositoryDefinitionsStore();
		repositoryDefinitionsStore.load();
	}

	@Override
	public void removeRepository(final String repositoryId) throws IllegalArgumentException {
		// open store if necessary
		if (null == repositoryDefinitionsStore) {
			open();
		}

		// remove
		repositoryDefinitionsStore.remove(repositoryId);
	}

}
