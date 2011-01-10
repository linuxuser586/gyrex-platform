/*******************************************************************************
 * Copyright (c) 2011 AGETO and others.
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

import java.util.Arrays;
import java.util.Collection;

import org.eclipse.gyrex.persistence.internal.PersistenceActivator;
import org.eclipse.gyrex.preferences.PlatformScope;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.osgi.util.NLS;

import org.osgi.service.prefs.BackingStoreException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Storage for repository definitions which is based on {@link PlatformScope
 * platform preferences}.
 */
class RepositoryDefinitionsStore {

	private static final String NODE_REPOSITORIES = "repositories";
	private static final Logger LOG = LoggerFactory.getLogger(RepositoryDefinitionsStore.class);

	private final IEclipsePreferences storage;

	/**
	 * Creates a new instance.
	 */
	public RepositoryDefinitionsStore() {
		storage = (IEclipsePreferences) new PlatformScope().getNode(PersistenceActivator.SYMBOLIC_NAME).node(NODE_REPOSITORIES);
	}

	/**
	 * Creates a new repository
	 * 
	 * @param repositoryId
	 * @param repositoryProviderId
	 * @return
	 */
	public RepositoryDefinition create(final String repositoryId, final String repositoryProviderId) {
		final RepositoryDefinition repositoryDefinition = new RepositoryDefinition(repositoryId, (IEclipsePreferences) storage.node(repositoryId));
		repositoryDefinition.setProviderId(repositoryProviderId);
		return repositoryDefinition;
	}

	/**
	 * Lookup of a repository definition
	 * 
	 * @param repositoryId
	 * @return the repo definition (maybe <code>null</code> if not found)
	 */
	public RepositoryDefinition findById(final String repositoryId) {
		if (repositoryId == null) {
			throw new IllegalArgumentException("repository id must not be null");
		}

		try {
			if (!storage.nodeExists(repositoryId)) {
				return null;
			}
		} catch (final BackingStoreException e) {
			LOG.warn("Unable to access preferences store. Repositories not available. {}", new Object[] { e.getMessage(), e });
			return null;
		}

		return new RepositoryDefinition(repositoryId, (IEclipsePreferences) storage.node(repositoryId));
	}

	public Collection<String> findRepositoryIds() {
		if (null == storage) {
			throw new IllegalStateException("not loaded");
		}
		try {
			return Arrays.asList(storage.childrenNames());
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(NLS.bind("Error reading repositories: {1}", e.getMessage()), e);
		}
	}

	/**
	 * Removes a repository definition.
	 * 
	 * @param repositoryId
	 */
	public void remove(final String repositoryId) {
		if (null == storage) {
			throw new IllegalStateException("not loaded");
		}

		try {
			if (storage.nodeExists(repositoryId)) {
				storage.node(repositoryId).removeNode();
				storage.flush();
			}
		} catch (final BackingStoreException e) {
			throw new IllegalStateException(NLS.bind("Error removing repository ''{0}'': {1}", repositoryId, e.getMessage()), e);
		}
	}
}