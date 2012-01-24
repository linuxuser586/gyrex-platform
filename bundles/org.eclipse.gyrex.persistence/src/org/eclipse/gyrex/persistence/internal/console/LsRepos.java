/*******************************************************************************
 * Copyright (c) 2012 <enter-company-name-here> and others.
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

import org.eclipse.gyrex.persistence.internal.PersistenceActivator;
import org.eclipse.gyrex.persistence.internal.storage.RepositoryDefinition;
import org.eclipse.gyrex.persistence.internal.storage.RepositoryProviderRegistry;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 *
 */
public class LsRepos extends BaseRepoSelectingCommand {

	@Option(name = "-v", aliases = { "--verbose" }, usage = "prints detailed configuration information")
	protected boolean verbose = false;

	@Argument(index = 0, metaVar = "ID-SUB-STRING", usage = "repository sub-string id filter")
	protected String repositoryIdFilterArg;

	/**
	 * Creates a new instance.
	 * 
	 * @param description
	 */
	public LsRepos() {
		super("<repoIdFilter> - lists available repositories");
	}

	@Override
	protected String getRepositoryIdFilter() {
		// allow specifying filter as argument
		if (null != repositoryIdFilterArg) {
			return repositoryIdFilterArg;
		}

		// fallback to option
		return super.getRepositoryIdFilter();
	}

	@Override
	protected void processRepository(final String repositoryId) throws Exception {
		final RepositoryDefinition definition = getRegistry().getRepositoryDefinition(repositoryId);

		final String providerId = definition.getProviderId();

		try {
			final RepositoryProviderRegistry providerRegistry = PersistenceActivator.getInstance().getRepositoryProviderRegistry();
			final String provider = providerRegistry.getRepositoryProviderInfo(providerId);
			printf("%s [%s]", repositoryId, StringUtils.trimToEmpty(provider));
			if (verbose) {
				final IRepositoryPreferences repositoryPreferences = definition.getRepositoryPreferences();
				final String[] keys = repositoryPreferences.getKeys("");
				for (final String key : keys) {
					printf("\t%20s: %s", key, StringUtils.trimToEmpty(repositoryPreferences.get(key, null)));
				}
			}
		} catch (final Exception e) {
			printf("%s [ERROR] %s", repositoryId, ExceptionUtils.getRootCauseMessage(e));
		}
	}

}
