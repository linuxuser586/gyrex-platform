/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.p2.internal.repositories;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

import org.eclipse.gyrex.cloud.environment.INodeEnvironment;
import org.eclipse.gyrex.p2.internal.P2Activator;
import org.eclipse.gyrex.p2.internal.P2Debug;

import org.osgi.framework.InvalidSyntaxException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class RepoUtil {

	private static final Logger LOG = LoggerFactory.getLogger(RepoUtil.class);

	/**
	 * Synchronizes the p2 repository manager with all cloud repo definitions.
	 * 
	 * @param repositoryManager
	 */
	public static void configureRepositories(final IMetadataRepositoryManager metadataRepositoryManager, final IArtifactRepositoryManager artifactRepositoryManager) {
		if (metadataRepositoryManager == null) {
			throw new IllegalArgumentException("Metadata repository service not available. Please check installation.");
		}
		if (artifactRepositoryManager == null) {
			throw new IllegalArgumentException("Metadata repository service not available. Please check installation.");
		}
		if (P2Debug.debug) {
			LOG.debug("Synchronizing repository information...");
		}
		final Map<URI, RepositoryDefinition> repositoriesToInstall = getFilteredReposMap();

		// disable all non-local
		for (final URI repo : metadataRepositoryManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_NON_LOCAL)) {
			metadataRepositoryManager.setEnabled(repo, false);
		}
		for (final URI repo : artifactRepositoryManager.getKnownRepositories(IRepositoryManager.REPOSITORIES_NON_LOCAL)) {
			artifactRepositoryManager.setEnabled(repo, false);
		}

		// now add or install all allowed
		for (final URI uri : repositoriesToInstall.keySet()) {
			final RepositoryDefinition definition = repositoriesToInstall.get(uri);
			if (P2Debug.debug) {
				LOG.debug("Adding repository: {}", definition);
			}
			metadataRepositoryManager.addRepository(uri);
			metadataRepositoryManager.setRepositoryProperty(uri, IRepository.PROP_NICKNAME, definition.getId());
			artifactRepositoryManager.addRepository(uri);
			artifactRepositoryManager.setRepositoryProperty(uri, IRepository.PROP_NICKNAME, definition.getId());
		}
	}

	public static URI[] getFilteredRepositories() {
		final IRepositoryDefinitionManager repositoryDefinitionManager = P2Activator.getInstance().getRepositoryManager();
		final Collection<RepositoryDefinition> repositories = repositoryDefinitionManager.getRepositories();
		final List<URI> repositoriesToInstall = new ArrayList<URI>(repositories.size());
		for (final RepositoryDefinition repositoryDefinition : repositories) {
			final String nodeFilter = repositoryDefinition.getNodeFilter();
			if (StringUtils.isNotBlank(nodeFilter)) {
				try {
					if (!P2Activator.getInstance().getService(INodeEnvironment.class).matches(nodeFilter)) {
						continue;
					}
				} catch (final InvalidSyntaxException e) {
					LOG.warn("Invalid node filter for repository {}. Repository will be ignored. {}", repositoryDefinition.getId(), ExceptionUtils.getRootCauseMessage(e));
					continue;
				}
			}
			final URI location = repositoryDefinition.getLocation();
			if (null != location) {
				repositoriesToInstall.add(location);
			}
		}
		return repositoriesToInstall.toArray(new URI[repositoriesToInstall.size()]);
	}

	private static Map<URI, RepositoryDefinition> getFilteredReposMap() {
		final IRepositoryDefinitionManager repositoryDefinitionManager = P2Activator.getInstance().getRepositoryManager();
		final Collection<RepositoryDefinition> repositories = repositoryDefinitionManager.getRepositories();
		final Map<URI, RepositoryDefinition> repositoriesToInstall = new HashMap<URI, RepositoryDefinition>(repositories.size());
		for (final RepositoryDefinition repositoryDefinition : repositories) {
			final String nodeFilter = repositoryDefinition.getNodeFilter();
			if (StringUtils.isNotBlank(nodeFilter)) {
				try {
					if (!P2Activator.getInstance().getService(INodeEnvironment.class).matches(nodeFilter)) {
						continue;
					}
				} catch (final InvalidSyntaxException e) {
					LOG.warn("Invalid node filter for repository {}. Repository will be ignored. {}", repositoryDefinition.getId(), ExceptionUtils.getRootCauseMessage(e));
					continue;
				}
			}
			final URI location = repositoryDefinition.getLocation();
			if (null != location) {
				repositoriesToInstall.put(location, repositoryDefinition);
			}
		}
		return repositoriesToInstall;
	}

	private RepoUtil() {
		// empty
	}

}
