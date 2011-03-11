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
package org.eclipse.gyrex.p2.internal.installer;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

import org.eclipse.gyrex.cloud.environment.INodeEnvironment;
import org.eclipse.gyrex.cloud.services.locking.IDurableLock;
import org.eclipse.gyrex.cloud.services.locking.ILockMonitor;
import org.eclipse.gyrex.cloud.services.locking.ILockService;
import org.eclipse.gyrex.p2.internal.P2Activator;
import org.eclipse.gyrex.p2.internal.P2Debug;
import org.eclipse.gyrex.p2.packages.IComponent;
import org.eclipse.gyrex.p2.packages.PackageDefinition;
import org.eclipse.gyrex.p2.packages.components.InstallableUnit;
import org.eclipse.gyrex.p2.repositories.IRepositoryDefinitionManager;
import org.eclipse.gyrex.p2.repositories.RepositoryDefinition;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import org.osgi.framework.InvalidSyntaxException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class PackageInstallerJob extends Job {

	static class MutexRule implements ISchedulingRule {

		private final Object object;

		public MutexRule(final Object object) {
			this.object = object;
		}

		public boolean contains(final ISchedulingRule rule) {
			return rule == this;
		}

		public boolean isConflicting(final ISchedulingRule rule) {
			if (rule instanceof MutexRule) {
				return object.equals(((MutexRule) rule).object);
			}
			return false;
		}
	}

	/** CONCAT */
	private static final String ID_INSTALL_LOCK = P2Activator.SYMBOLIC_NAME.concat(".install.lock");

	private static final Logger LOG = LoggerFactory.getLogger(PackageInstallerJob.class);

	private final ILockMonitor<IDurableLock> lockMonitor = new ILockMonitor<IDurableLock>() {

		@Override
		public void lockAcquired(final IDurableLock lock) {
			// empty
		}

		@Override
		public void lockLost(final IDurableLock lock) {
			// cancel the job
			LOG.warn("Lost global installation lock. Aborting installation.");
			cancel();
		}

		@Override
		public void lockReleased(final IDurableLock lock) {
			// empty
		}
	};

	private final Set<PackageDefinition> packagesToInstall;

	private final Set<PackageDefinition> packagesToRemove;

	/**
	 * Creates a new instance.
	 * 
	 * @param packagesToRemove
	 * @param packagesToInstall
	 */
	PackageInstallerJob(final Set<PackageDefinition> packagesToInstall, final Set<PackageDefinition> packagesToRemove) {
		super("Software Package Installer");
		this.packagesToInstall = packagesToInstall;
		this.packagesToRemove = packagesToRemove;
		setSystem(true);
		setPriority(LONG);
		setRule(new MutexRule(PackageInstallerJob.class));
	}

	/**
	 * Synchronizes the p2 repository manager with all cloud repo definitions.
	 * 
	 * @param repositoryManager
	 */
	private void configureRepositories(final IMetadataRepositoryManager metadataRepositoryManager, final IArtifactRepositoryManager artifactRepositoryManager) {
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
			metadataRepositoryManager.addRepository(uri);
			metadataRepositoryManager.setRepositoryProperty(uri, IRepository.PROP_NICKNAME, definition.getId());
			artifactRepositoryManager.addRepository(uri);
			artifactRepositoryManager.setRepositoryProperty(uri, IRepository.PROP_NICKNAME, definition.getId());
		}
	}

	private void install(final IProvisioningAgent agent, final InstallLog installLog) {
		// configure metadata & artifact repository
		final IMetadataRepositoryManager metadataRepositoryManager = (IMetadataRepositoryManager) agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
		final IArtifactRepositoryManager artifactRepositoryManager = (IArtifactRepositoryManager) agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
		configureRepositories(metadataRepositoryManager, artifactRepositoryManager);
		installLog.logRepositories(metadataRepositoryManager, artifactRepositoryManager);

		// find the IUs to install
		final Set<IInstallableUnit> unitsToInstall = new HashSet<IInstallableUnit>();
		for (final PackageDefinition installPackage : packagesToInstall) {
			// node filter is checked in PackageScanner already
			final Collection<IComponent> componentsToInstall = installPackage.getComponentsToInstall();
			for (final IComponent component : componentsToInstall) {
				if (component instanceof InstallableUnit) {
					final String id = component.getId();
					final Version version = ((InstallableUnit) component).getVersion();
					IQuery<IInstallableUnit> query;
					if (null != version) {
						query = QueryUtil.createIUQuery(id, version);
					} else {
						query = QueryUtil.createLatestQuery(QueryUtil.createIUQuery(id));
					}
					if (P2Debug.nodeInstallation) {
						LOG.debug("Performing query for IU {} (version {}): {}", new Object[] { id, version, query });
					}
					final IQueryResult<IInstallableUnit> queryResult = metadataRepositoryManager.query(query, new NullProgressMonitor());
					if (queryResult.isEmpty()) {
						LOG.warn("Component {} specified in package {} could not be found. Please check repository configuration.", component, installPackage.getId());
						// TODO should abort installation?
						continue;
					}
					final Iterator<IInstallableUnit> iterator = queryResult.iterator();
					while (iterator.hasNext()) {
						final IInstallableUnit unit = iterator.next();
						if (P2Debug.nodeInstallation) {
							LOG.debug("Found unit {} (version {}) for component {}", new Object[] { unit.getId(), unit.getVersion(), component });
						}
						unitsToInstall.add(unit);
					}
				}
			}
		}

		// create install operation
		final InstallOperation op = new InstallOperation(new ProvisioningSession(agent), unitsToInstall);

		// check if installable
		final IStatus result = op.resolveModal(new NullProgressMonitor());

		// log status
		installLog.logInstallStatus(op, result);

		if (result.matches(IStatus.ERROR | IStatus.CANCEL)) {
			LOG.warn("Install operation not possible. {}", op.getResolutionDetails());
			throw new IllegalStateException("Install operation not possible. " + op.getResolutionDetails());
		} else if (!result.isOK()) {
			LOG.warn("Install operation resolved with warnings. An installation will be forced. {}", op.getResolutionDetails());
		}

		// perform install
		final ProvisioningJob job = op.getProvisioningJob(new NullProgressMonitor());
		final IStatus installResult = job.runModal(new NullProgressMonitor());
		if (!result.isOK()) {
			LOG.warn("Install operation failed. {}", installResult.getMessage());
			throw new IllegalStateException("Install operation faile. " + installResult.getMessage(), installResult.getException());
		}

		// mark packages as installed

		// TODO check job restart policy
		// TODO rework to install packages one by one
		// TODO restart after the first package which requires a restart
	}

	private void remove(final IProvisioningAgent agent, final InstallLog installLog) {
		// TODO Auto-generated method stub

	}

	@SuppressWarnings("restriction")
	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		// check if there is an install session
		String activeSessionId = PackageInstallState.getActiveInstallSessionId();

		// acquire global installation lock
		LOG.info("Software installation started. Checking for global installation lock.");
		final ILockService lockService = P2Activator.getInstance().getService(ILockService.class);
		IDurableLock lock;
		if (null != activeSessionId) {
			if (P2Debug.nodeInstallation) {
				LOG.debug("Recovering active installation session.");
			}
			try {
				lock = lockService.recoverDurableLock(ID_INSTALL_LOCK, lockMonitor, activeSessionId);
			} catch (final IllegalStateException e) {
				LOG.warn("Unable to recover installation session. Please intervent manually! {}", ExceptionUtils.getRootCauseMessage(e));
				// TODO we should be able to recover from this ourselves
				// it's not that bad; if we can reliably say the lock was done/deleted we can kill the active session and restart
				// however, if this happens after a suspended install session the system might be in an broken state?
				// on the other hand, how broken can the system be if we reached this point?
				return Status.CANCEL_STATUS;
			}
		} else {
			if (P2Debug.nodeInstallation) {
				LOG.debug("Starting new installation session.");
			}
			try {
				lock = lockService.acquireDurableLock(ID_INSTALL_LOCK, lockMonitor, TimeUnit.SECONDS.toMillis(5));
			} catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
				LOG.info("Installation canceled while waiting for install lock. Aborting current installation.");
				return Status.CANCEL_STATUS;
			} catch (final TimeoutException e) {
				LOG.info("Concurrent installation in progress on a differnt node. Aborting current installation.");
				return Status.CANCEL_STATUS;
			}
		}

		InstallLog installLog = null;
		IProvisioningAgent agent = null;
		try {
			// set (new) active install session id
			activeSessionId = lock.getRecoveryKey();
			PackageInstallState.setActiveInstallSessionId(activeSessionId);

			// report progress in an installation log in the install area
			installLog = new InstallLog(activeSessionId);

			// get agent
			agent = P2Activator.getInstance().getService(IProvisioningAgentProvider.class).createAgent(null);
			if (agent == null) {
				throw new IllegalStateException("The current system has not been provisioned using p2. Unable to acquire provisioning agent.");
			}

			// backup configuration
			final Configurator configurator = P2Activator.getInstance().getService(Configurator.class);
			installLog.logConfiguration(configurator.getUrlInUse());

			// perform installation
			if (!packagesToInstall.isEmpty()) {
				install(agent, installLog);
			} else {
				installLog.nothingToAdd();
			}
			if (!packagesToRemove.isEmpty()) {
				remove(agent, installLog);
			} else {
				installLog.nothingToRemove();
			}

			// apply configuration
			if (P2Debug.nodeInstallation) {
				LOG.info("Applying new software configuration. {}", configurator.getUrlInUse());
			}
			installLog.logConfiguration(configurator.getUrlInUse());
			configurator.applyConfiguration();

			// close session
			PackageInstallState.removeActiveInstallSessionId();

			// release global installation lock
			lock.release();

		} catch (final Exception e) {
			LOG.error("Error during software installation. Please check installation log ({}). {}", new Object[] { installLog, ExceptionUtils.getRootCauseMessage(e), e });
			return Status.CANCEL_STATUS;
		} finally {
			// TODO we should somehow actively suspend the lock
			//lock.suspend();

			// close log
			if (null != installLog) {
				installLog.close();
			}

			// close agent
			if (null != agent) {
				agent.stop();
			}
		}

		return Status.OK_STATUS;
	}
}
