/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.p2.repositories.IRepositoryManager;
import org.eclipse.gyrex.p2.repositories.RepositoryDefinition;
import org.eclipse.gyrex.preferences.CloudScope;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class RepoManager implements IRepositoryManager {

	private static final String PREF_NODE_REPOSITORIES = "repositories";

	private static final String PREF_KEY_NODE_FILTER = "nodeFilter";
	private static final String PREF_KEY_LOCATION = "location";

	private static final Logger LOG = LoggerFactory.getLogger(RepoManager.class);

	private Preferences getRepoNode(final String repoId) {
		return CloudScope.INSTANCE.getNode(P2Activator.SYMBOLIC_NAME).node(PREF_NODE_REPOSITORIES).node(repoId);
	}

	@Override
	public Collection<RepositoryDefinition> getRepositories() {
		try {
			final IEclipsePreferences rootNode = CloudScope.INSTANCE.getNode(P2Activator.SYMBOLIC_NAME);
			if (!rootNode.nodeExists(PREF_NODE_REPOSITORIES)) {
				return Collections.emptyList();
			}
			final Preferences channelsNode = rootNode.node(PREF_NODE_REPOSITORIES);
			final String[] childrenNames = channelsNode.childrenNames();
			final List<RepositoryDefinition> repos = new ArrayList<RepositoryDefinition>();
			for (final String repoId : childrenNames) {
				final RepositoryDefinition repo = readRepo(repoId);
				if (repo != null) {
					repos.add(repo);
				}
			}
			return repos;
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error reading repositories from backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	private RepositoryDefinition readRepo(final String repoId) {
		try {
			final RepositoryDefinition descriptor = new RepositoryDefinition();
			descriptor.setId(repoId);

			final Preferences node = getRepoNode(repoId);
			descriptor.setLocation(new URI(node.get(PREF_KEY_LOCATION, null)));
			descriptor.setNodeFilter(node.get(PREF_KEY_NODE_FILTER, null));
			return descriptor;
		} catch (final Exception e) {
			LOG.warn("Unable to read repository definition {}. {}", repoId, ExceptionUtils.getRootCauseMessage(e));
			return null;
		}
	}

	@Override
	public void removeRepository(final String id) {
		try {
			if (!IdHelper.isValidId(id)) {
				throw new IllegalArgumentException("invalid id");
			}

			final IEclipsePreferences rootNode = CloudScope.INSTANCE.getNode(P2Activator.SYMBOLIC_NAME);
			if (!rootNode.nodeExists(PREF_NODE_REPOSITORIES)) {
				return;
			}
			final Preferences channelsNode = rootNode.node(PREF_NODE_REPOSITORIES);
			if (!channelsNode.nodeExists(id)) {
				return;
			}

			channelsNode.node(id).removeNode();
			channelsNode.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error removing repository from backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

	@Override
	public void saveRepository(final RepositoryDefinition repository) {
		try {
			final String id = repository.getId();
			if (!IdHelper.isValidId(id)) {
				throw new IllegalArgumentException("invalid repository id");
			}
			final URI location = repository.getLocation();
			if (location == null) {
				throw new IllegalArgumentException("repository must have a location");
			}
			final Preferences node = getRepoNode(id);
			node.put(PREF_KEY_LOCATION, location.toString());
			final String nodeFilter = repository.getNodeFilter();
			if (StringUtils.isNotBlank(nodeFilter)) {
				node.put(PREF_KEY_NODE_FILTER, nodeFilter);
			} else {
				node.remove(PREF_KEY_NODE_FILTER);
			}
			node.flush();
		} catch (final BackingStoreException e) {
			throw new IllegalStateException("Error saving repository to backend store. " + ExceptionUtils.getRootCauseMessage(e), e);
		}
	}

}
