/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.internal.console;

import java.util.Collection;
import java.util.TreeSet;

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.persistence.internal.PersistenceActivator;
import org.eclipse.gyrex.persistence.internal.storage.RepositoryRegistry;
import org.eclipse.gyrex.persistence.storage.registry.IRepositoryRegistry;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.args4j.Option;

/**
 * Base class for commands working with one or multiple repositories.
 */
public abstract class BaseRepoSelectingCommand extends Command {

	@Option(name = "-r", aliases = { "--repositoryIdFilter" }, metaVar = "ID-SUB-STRING", usage = "repository sub-string id filter")
	protected String repositoryIdFilter;

	private RepositoryRegistry repositoryRegistry;

	/**
	 * Creates a new instance.
	 * 
	 * @param description
	 */
	public BaseRepoSelectingCommand(final String description) {
		super(description);
	}

	@Override
	protected void doExecute() throws Exception {
		final IRepositoryRegistry registry = getRegistry();

		final String filter = getRepositoryIdFilter();
		final Collection<String> filteredRepoIds = new TreeSet<String>();
		final Collection<String> repositoryIds = registry.getRepositoryIds();
		for (final String repoId : repositoryIds) {
			if ((null == filter) || StringUtils.contains(repoId, filter)) {
				filteredRepoIds.add(repoId);
			}
		}
		if (filteredRepoIds.isEmpty()) {
			printf("No repositories found!");
			return;
		}

		for (final String repoId : filteredRepoIds) {
			processRepository(repoId);
		}
	}

	protected RepositoryRegistry getRegistry() {
		if (null != repositoryRegistry) {
			return repositoryRegistry;
		}
		return repositoryRegistry = PersistenceActivator.getInstance().getRepositoriesManager();
	}

	protected String getRepositoryIdFilter() {
		return repositoryIdFilter;
	}

	protected abstract void processRepository(final String repositoryId) throws Exception;
}
