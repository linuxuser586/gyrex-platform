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
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.equinox.internal.provisional.configurator.Configurator;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.UninstallOperation;
import org.eclipse.equinox.p2.operations.UpdateOperation;

import org.eclipse.gyrex.boot.internal.app.ServerApplication;
import org.eclipse.gyrex.cloud.services.locking.IDurableLock;
import org.eclipse.gyrex.cloud.services.locking.ILockMonitor;
import org.eclipse.gyrex.cloud.services.locking.ILockService;
import org.eclipse.gyrex.p2.internal.P2Activator;
import org.eclipse.gyrex.p2.internal.P2Debug;
import org.eclipse.gyrex.p2.internal.packages.PackageDefinition;
import org.eclipse.gyrex.p2.internal.repositories.RepoUtil;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

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
	public static final String ID_INSTALL_LOCK = P2Activator.SYMBOLIC_NAME.concat(".install.lock");

	public static final Logger LOG = LoggerFactory.getLogger(PackageInstallerJob.class);

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

	private IDurableLock lock;

	private ProvisioningHelper provisioningHelper;

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

	private void checkLock() {
		if (!lock.isValid()) {
			throw new OperationCanceledException();
		}
	}

	private int install(final PackageDefinition installPackage, final IProvisioningAgent agent, final InstallLog installLog) {
		if (P2Debug.nodeInstallation) {
			LOG.debug("Installing package: {}", installPackage);
		}

		// get repositories
		final URI[] repositories = RepoUtil.getFilteredRepositories();
		installLog.logRepositories(repositories);

		// node filter is checked in PackageScanner already
		// TODO should we check here again?

		// find the IUs to install
		final IInstallableUnit packageUnit = PackageInstallState.createUnit(installPackage);

		// check lock
		checkLock();

		// create install operation
		final InstallOperation op = provisioningHelper.getInstallOperation(Collections.singletonList(packageUnit), repositories);

		// check if installable
		if (P2Debug.nodeInstallation) {
			LOG.debug("Resolving p2 install operation for package {}...", installPackage);
		}
		final IStatus result = op.resolveModal(null);
		if (P2Debug.nodeInstallation) {
			LOG.debug("Resolved p2 install operation for package {}: {}", installPackage, op.getResolutionDetails());
		}

		// log status
		installLog.logInstallStatus(op, result);

		if (result.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE) {
			LOG.warn("Nothing to update.");
			return ProvisioningJob.RESTART_NONE;
		}

		if (result.matches(IStatus.ERROR | IStatus.CANCEL)) {
			LOG.warn("Install operation not possible. {}", op.getResolutionDetails());
			throw new IllegalStateException("Install operation not possible. " + op.getResolutionDetails());
		} else if (!result.isOK()) {
			LOG.warn("Install operation resolved with warnings. An installation will be forced. {}", op.getResolutionDetails());
		}

		// check lock
		checkLock();

		// perform install
		if (P2Debug.nodeInstallation) {
			LOG.debug("Performing installation of package {}...", installPackage);
		}
		final ProvisioningJob job = op.getProvisioningJob(null);
		final IStatus installResult = job.runModal(null);
		if (!result.isOK()) {
			LOG.warn("Install operation failed. {}", installResult.getMessage());
			throw new IllegalStateException("Install operation failed. " + installResult.getMessage(), installResult.getException());
		}

		return job.getRestartPolicy();
	}

	private int remove(final PackageDefinition packageDefinition, final IProvisioningAgent agent, final InstallLog installLog) {
		if (P2Debug.nodeInstallation) {
			LOG.debug("Uninstalling package: {}", packageDefinition);
		}

		// get repositories
		final URI[] repositories = RepoUtil.getFilteredRepositories();
		installLog.logRepositories(repositories);

		// node filter is checked in PackageScanner already
		// TODO should we check here again?

		// find the IUs to un-install
		final IInstallableUnit packageUnit = PackageInstallState.createUnit(packageDefinition);

		// check lock
		checkLock();

		// create install operation
		final UninstallOperation op = provisioningHelper.getUninstallOperation(Collections.singletonList(packageUnit), repositories);

		// check if installable
		if (P2Debug.nodeInstallation) {
			LOG.debug("Resolving p2 uninstall operation for package {}...", packageDefinition);
		}
		final IStatus result = op.resolveModal(null);
		if (P2Debug.nodeInstallation) {
			LOG.debug("Resolved p2 uninstall operation for package {}: {}", packageDefinition, op.getResolutionDetails());
		}

		// log status
		installLog.logUninstallStatus(op, result);

		if (result.matches(IStatus.ERROR | IStatus.CANCEL)) {
			LOG.warn("Uninstall operation not possible. {}", op.getResolutionDetails());
			throw new IllegalStateException("Install operation not possible. " + op.getResolutionDetails());
		} else if (!result.isOK()) {
			LOG.warn("Uninstall operation resolved with warnings. An installation will be forced. {}", op.getResolutionDetails());
		}

		// check lock
		checkLock();

		// perform install
		if (P2Debug.nodeInstallation) {
			LOG.debug("Performing installation of package {}...", packageDefinition);
		}
		final ProvisioningJob job = op.getProvisioningJob(null);
		final IStatus installResult = job.runModal(null);
		if (!result.isOK()) {
			LOG.warn("Install operation failed. {}", installResult.getMessage());
			throw new IllegalStateException("Install operation failed. " + installResult.getMessage(), installResult.getException());
		}

		return job.getRestartPolicy();
	}

	@SuppressWarnings("restriction")
	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		// check if there is an install session
		String activeSessionId = PackageInstallState.getActiveInstallSessionId();

		// acquire global installation lock
		LOG.info("Software installation started. Checking for global installation lock.");
		final ILockService lockService = P2Activator.getInstance().getService(ILockService.class);

		// try recovering an existing lock
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
		}

		// start new session if lock could not be recovered
		if (lock == null) {
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
			// if we already have a session id, record it as recovered install session
			final String recoveredInstallSessionId = activeSessionId;

			// set (new) active install session id (in any case to be in sync with the lock id)
			activeSessionId = lock.getRecoveryKey();
			PackageInstallState.setActiveInstallSessionId(activeSessionId);

			// report progress in an installation log in the install area
			installLog = new InstallLog(activeSessionId);

			// write recovery note
			if (null != recoveredInstallSessionId) {
				installLog.recoveredSession(recoveredInstallSessionId);
			}

			// check lock
			checkLock();

			// get agent
			agent = P2Activator.getInstance().getService(IProvisioningAgentProvider.class).createAgent(null);
			if (agent == null) {
				throw new IllegalStateException("The current system has not been provisioned using p2. Unable to acquire provisioning agent.");
			}

			// create helper
			provisioningHelper = new ProvisioningHelper(new ProvisioningSession(agent), IProfileRegistry.SELF);

			// backup configuration
			final Configurator configurator = P2Activator.getInstance().getService(Configurator.class);
			installLog.logConfiguration(configurator.getUrlInUse());

			// check lock
			checkLock();

			// perform removals first
			if (!packagesToRemove.isEmpty()) {
				for (final PackageDefinition packageDefinition : packagesToRemove) {
					final int restartPolicy = remove(packageDefinition, agent, installLog);

					// restart after the first package which requires a restart
					// without releasing the installation lock and continue with
					// installation after successful restart
					if (restartPolicy == ProvisioningJob.RESTART_ONLY) {
						LOG.warn("Restart required after un-installing package {}. Restarting now.", packageDefinition);
						installLog.restart();
						scheduleRestart();
						return Status.OK_STATUS;
					}
				}
			} else {
				installLog.nothingToRemove();
			}

			// perform installation
			if (!packagesToInstall.isEmpty()) {
				for (final PackageDefinition packageDefinition : packagesToInstall) {
					final int restartPolicy = install(packageDefinition, agent, installLog);

					// restart after the first package which requires a restart
					// without releasing the installation lock and continue with
					// installation after successful restart
					if (restartPolicy == ProvisioningJob.RESTART_ONLY) {
						LOG.warn("Restart required after installing package {}. Restarting now.", packageDefinition);
						installLog.restart();
						scheduleRestart();
						return Status.OK_STATUS;
					}
				}
			} else {
				installLog.nothingToAdd();
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

		} catch (final OperationCanceledException e) {
			LOG.warn("Software installation canceled.");

			// cleanup session
			PackageInstallState.removeActiveInstallSessionId();

			// release global installation lock
			if (lock.isValid()) {
				lock.release();
			}

			if (null != installLog) {
				installLog.canceled();
			}

			return Status.CANCEL_STATUS;
		} catch (final Exception e) {
			LOG.error("Error during software installation. Please check installation log ({}). {}", new Object[] { installLog, ExceptionUtils.getRootCauseMessage(e), e });
			return Status.CANCEL_STATUS;
		} finally {
			// TODO should we somehow actively suspend the lock?
			//lock.suspend();

			// close log
			if (null != installLog) {
				installLog.close();
			}

			// clear helper
			provisioningHelper = null;

			// close agent
			if (null != agent) {
				agent.stop();
			}

			// clear references
			lock = null;
		}

		return Status.OK_STATUS;
	}

	private void scheduleRestart() {
		final Job restartJob = new Job("Restarting") {

			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				ServerApplication.signalRelaunch();
				return Status.OK_STATUS;
			}
		};
		restartJob.setSystem(true);
		restartJob.setPriority(Job.SHORT);
		restartJob.schedule(500L);
	}
}
