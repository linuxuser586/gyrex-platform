/*******************************************************************************
 * Copyright (c) 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.p2.internal.commands;

import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.p2.internal.P2Activator;
import org.eclipse.gyrex.p2.internal.packages.InstallableUnitReference;
import org.eclipse.gyrex.p2.internal.packages.PackageDefinition;
import org.eclipse.gyrex.p2.internal.repositories.RepoUtil;
import org.eclipse.gyrex.p2.internal.repositories.RepositoryDefinition;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.MetadataFactory;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

import org.apache.commons.lang.StringUtils;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

public final class ListCommand extends BaseSwCmd {

	@Argument(index = 0, usage = "specify what to list (repositories, packages or artifacts)", required = true, metaVar = "WHAT")
	String what;

	@Argument(index = 1, usage = "an optional filter string", required = false, metaVar = "FILTER")
	String filterString;

	@Option(name = "-latest", usage = "a flag to search for latest versions only (default is all versions)", required = false)
	boolean latestVersionOnly = false;

	public ListCommand() {
		super("repos|packages|artifacts [filterString] - list repos, packages or artifacts");
	}

	@Override
	protected void doExecute() throws Exception {
		if (StringUtils.isBlank(what)) {
			printf("ERROR: please specify what to list");
			return;
		}

		if (StringUtils.startsWithIgnoreCase("repos", what)) {
			final Collection<RepositoryDefinition> repos = getRepositoryManager().getRepositories();
			for (final RepositoryDefinition repo : repos) {
				if ((null == filterString) || StringUtils.contains(repo.getId(), filterString) || ((null != repo.getLocation()) && StringUtils.contains(repo.getLocation().toString(), filterString))) {
					ci.println(String.format("%s [%s]", repo.getId(), repo.toString()));
				}
			}
		} else if (StringUtils.startsWithIgnoreCase("packages", what)) {
			listPackages();
		} else if (StringUtils.startsWithIgnoreCase("artifacts", what)) {
			listArtifacts();
		} else {
			printf("ERROR: repos|packages expected");
			return;
		}

	}

	private void listArtifacts() throws Exception {
		IProvisioningAgent agent = null;
		try {
			// get agent
			agent = P2Activator.getInstance().getService(IProvisioningAgentProvider.class).createAgent(null);
			if (agent == null)
				throw new IllegalStateException("The current system has not been provisioned using p2. Unable to acquire provisioning agent.");

			final IMetadataRepositoryManager manager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
			if (manager == null)
				throw new IllegalStateException("The provision system is broken. Unable to acquire metadata repository service.");

			// sync repos
			RepoUtil.configureRepositories(manager, (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME));

			// load repos
			final URI[] knownRepositories = manager.getKnownRepositories(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
			for (final URI uri : knownRepositories) {
				printf("Loading %s", uri.toString());
				manager.loadRepository(uri, new NullProgressMonitor());
			}

			// query for everything that provides an OSGi bundle and features
			IQuery<IInstallableUnit> query = QueryUtil.createMatchQuery("properties[$0] == true || providedCapabilities.exists(p | p.namespace == 'osgi.bundle')", new Object[] { MetadataFactory.InstallableUnitDescription.PROP_TYPE_GROUP }); //$NON-NLS-1$

			// wrap query if necessary
			if (latestVersionOnly) {
				query = QueryUtil.createPipeQuery(query, QueryUtil.createLatestIUQuery());
			}

			// execute
			printf("Done loading. Searching...");
			final SortedSet<String> result = new TreeSet<>();
			for (final Iterator stream = manager.query(query, new NullProgressMonitor()).iterator(); stream.hasNext();) {
				final IInstallableUnit iu = (IInstallableUnit) stream.next();

				// exclude fragments
				if ((iu.getFragments() != null) && (iu.getFragments().size() > 0)) {
					continue;
				}

				final String id = iu.getId();

				// exclude source IUs
				if (StringUtils.endsWith(id, ".source") || StringUtils.endsWith(id, ".source.feature.group")) {
					continue;
				}

				// get name
				String name = iu.getProperty(IInstallableUnit.PROP_NAME, null);
				if ((name == null) || name.startsWith("%")) {
					name = ""; //$NON-NLS-1$
				}

				// check if filter is provided
				if (StringUtils.isBlank(filterString) || StringUtils.containsIgnoreCase(id, filterString) || StringUtils.containsIgnoreCase(name, filterString)) {
					result.add(String.format("%s (%s, %s)", name, id, iu.getVersion()));
				}
			}

			if (result.isEmpty()) {
				printf("No artifacts found!");
			} else {
				printf("Found %d artifacts:", result.size());
				for (final String artifact : result) {
					printf(artifact);
				}
			}
		} finally {
			if (null != agent) {
				agent.stop();
			}
		}
	}

	private void listPackages() {
		// test for direct match
		if (IdHelper.isValidId(filterString)) {
			final PackageDefinition definition = getPackageManager().getPackage(filterString);
			if (definition != null) {
				printPackage(definition);
				return;
			}
		}

		final Collection<PackageDefinition> packages = getPackageManager().getPackages();
		for (final PackageDefinition pkdefinition : packages) {
			if ((null == filterString) || StringUtils.contains(pkdefinition.getId(), filterString)) {
				printf("%s [%s]", pkdefinition.getId(), pkdefinition.getInstallState());
			}
		}
	}

	private void printPackage(final PackageDefinition p) {
		printf("Package %s", p.getId());

		switch (p.getInstallState()) {
			case ROLLOUT:
				printf("  Is rolled out.");
				break;
			case REVOKE:
				printf("  Is revoked.");
				break;

			default:
				printf("  Is neither rolled out nor revoked.");
				break;
		}

		if (StringUtils.isBlank(p.getNodeFilter())) {
			printf("  Will be installed on all nodes.");
		} else {
			printf("  Will only be installed on nodes matching %.!", p.getNodeFilter());
		}

		final Collection<InstallableUnitReference> componentsToInstall = p.getComponentsToInstall();
		if (!componentsToInstall.isEmpty()) {
			printf("  Contains the following artifacts:");
			for (final InstallableUnitReference ref : componentsToInstall) {
				printf("     %s (%s)", ref.getId(), null != ref.getVersion() ? ref.getVersion() : "no version constraint");
			}
		} else {
			printf("  Contains no artifacts!");
		}
	}

}