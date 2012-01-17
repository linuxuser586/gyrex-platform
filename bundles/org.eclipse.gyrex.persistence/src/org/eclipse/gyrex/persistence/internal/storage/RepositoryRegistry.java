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
 *     Mike Tschierschke - customization for rap based admin ui,
 *     						added implementation for method getRepositoryDefinition
 *******************************************************************************/
package org.eclipse.gyrex.persistence.internal.storage;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.persistence.internal.PersistenceActivator;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;
import org.eclipse.gyrex.persistence.storage.registry.IRepositoryDefinition;
import org.eclipse.gyrex.persistence.storage.registry.IRepositoryRegistry;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;

import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The platform repository manager stores repository information.
 */
public class RepositoryRegistry implements IRepositoryRegistry {

	private static final Logger LOG = LoggerFactory.getLogger(RepositoryRegistry.class);

	private static void errorCreatingRepository(final RepositoryDefinition repositoryDef, final String detail) {
		throw new IllegalStateException(MessageFormat.format("Invalid repository definition ''{0}'': {1}", repositoryDef.getRepositoryId(), detail));
	}

	private final INodeChangeListener repositoryModifcationListener = new INodeChangeListener() {

		@Override
		public void added(final NodeChangeEvent event) {
			// not handled
		}

		@Override
		public void removed(final NodeChangeEvent event) {
			// repository has been removed
			final String repositoryId = event.getChild().name();
			// close removed repo
			close(repositoryId);
		}
	};

	private final AtomicReference<RepositoryDefinitionsStore> repositoryDefinitionsStoreRef = new AtomicReference<RepositoryDefinitionsStore>();
	private final ConcurrentMap<String, Lock> locksByRepositoryId = new ConcurrentHashMap<String, Lock>(4);
	private final ConcurrentMap<String, Repository> repositoryCache = new ConcurrentHashMap<String, Repository>(4);
	private final AtomicBoolean closed = new AtomicBoolean(false);

	public void close(final String repositoryId) {
		if (!repositoryCache.containsKey(repositoryId)) {
			return;
		}

		// lock
		final Lock lock = locksByRepositoryId.get(repositoryId);
		if (lock != null) {
			lock.lock();
		}
		final Repository repository;
		try {

			// remove cached instance
			repository = repositoryCache.remove(repositoryId);

			// remove lock
			locksByRepositoryId.remove(repositoryId);

		} finally {
			if (lock != null) {
				lock.unlock();
			}
		}

		// close repository outside lock
		if (null != repository) {
			try {
				repository.close();
			} catch (final Exception e) {
				LOG.error("Error closing repository {}. {}", new Object[] { repositoryId, ExceptionUtils.getRootCauseMessage(e), e });
			}
		}

	}

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
		final Repository repository = repositoryType.createRepositoryInstance(repositoryDef.getRepositoryId(), repositoryPreferences);
		if (null == repository) {
			errorCreatingRepository(repositoryDef, MessageFormat.format("repository type ''{0}'' returned no repository instance", repositoryProviderId));
		}

		return repository;
	}

	@Override
	public IRepositoryDefinition createRepository(final String repositoryId, final String repositoryProviderId) throws IllegalArgumentException {
		if (!IdHelper.isValidId(repositoryId)) {
			throw new IllegalArgumentException("repository id is not valid");
		}
		if (StringUtils.isBlank(repositoryProviderId)) {
			throw new IllegalArgumentException("repository id must not be null");
		}

		// create
		return getStore().create(repositoryId, repositoryProviderId);
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

		if (closed.get()) {
			throw new IllegalStateException("closed");
		}

		// lookup a cached instance
		Repository repository = repositoryCache.get(repositoryId);
		if (null != repository) {
			return repository;
		}

		// get repository definition
		final RepositoryDefinition repositoryDef = getStore().findById(repositoryId);
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
	public RepositoryDefinition getRepositoryDefinition(final String repositoryId) {
		if (null == repositoryId) {
			throw new IllegalArgumentException("repository id must not be null");
		}

		// get repository definition
		return getStore().findById(repositoryId);
	}

	@Override
	public Collection<String> getRepositoryIds() {
		return Collections.unmodifiableCollection(getStore().findRepositoryIds());
	}

	RepositoryDefinitionsStore getStore() {
		final RepositoryDefinitionsStore store = repositoryDefinitionsStoreRef.get();
		if (store != null) {
			return store;
		}
		if (repositoryDefinitionsStoreRef.compareAndSet(null, new RepositoryDefinitionsStore())) {
			RepositoryDefinitionsStore.getRepositoriesNode().addNodeChangeListener(repositoryModifcationListener);
		}
		return repositoryDefinitionsStoreRef.get();
	}

	@Override
	public void removeRepository(final String repositoryId) throws IllegalArgumentException {
		getStore().remove(repositoryId);
	}

	/**
	 * Stops the registry
	 */
	public void stop() {
		if (!closed.compareAndSet(false, true)) {
			return;
		}

		// remove listener
		RepositoryDefinitionsStore.getRepositoriesNode().removeNodeChangeListener(repositoryModifcationListener);

		// close any open repository
		while (!repositoryCache.isEmpty()) {
			final Set<String> repoIds = repositoryCache.keySet();
			for (final String repoId : repoIds) {
				close(repoId);
			}
		}
	}

}
