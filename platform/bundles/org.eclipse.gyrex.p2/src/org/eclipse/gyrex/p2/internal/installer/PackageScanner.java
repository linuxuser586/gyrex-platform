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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;

import org.eclipse.gyrex.cloud.environment.INodeEnvironment;
import org.eclipse.gyrex.p2.internal.P2Activator;
import org.eclipse.gyrex.p2.internal.P2Debug;
import org.eclipse.gyrex.p2.internal.packages.IPackageManager;
import org.eclipse.gyrex.p2.internal.packages.PackageDefinition;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import org.osgi.framework.InvalidSyntaxException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The package scanner scans for new or revoked packages regularly. If new (or
 * revoked) packages were found a job will be scheduled which performs the
 * modifications.
 */
public class PackageScanner extends Job {

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

	private static final Logger LOG = LoggerFactory.getLogger(PackageScanner.class);

	static final long INITIAL_SLEEP_TIME = TimeUnit.MINUTES.toMillis(3);
	static final long MAX_SLEEP_TIME = TimeUnit.HOURS.toMillis(2);
	private static final AtomicReference<PackageScanner> instanceRef = new AtomicReference<PackageScanner>();

	/**
	 * Returns the singleton package scanner instance.
	 * 
	 * @return the singleton package scanner instance
	 */
	public static PackageScanner getInstance() {
		final PackageScanner scanner = instanceRef.get();
		if (null != scanner) {
			return scanner;
		}
		instanceRef.compareAndSet(null, new PackageScanner());
		return instanceRef.get();
	}

	private long sleepTime = INITIAL_SLEEP_TIME;

	/**
	 * Creates a new instance.
	 */
	PackageScanner() {
		super("Software Package Scanner");
		setSystem(true);
		setPriority(LONG);
		setRule(new MutexRule(PackageScanner.class));
	}

	private IStatus doRun(final IProgressMonitor monitor) {
		IProvisioningAgent agent = null;
		try {
			final INodeEnvironment nodeEnvironment = P2Activator.getInstance().getService(INodeEnvironment.class);

			// get agent
			agent = P2Activator.getInstance().getService(IProvisioningAgentProvider.class).createAgent(null);
			if (agent == null) {
				throw new IllegalStateException("The current system has not been provisioned using p2. Unable to acquire provisioning agent.");
			}

			// collect packages that should be rolled out
			final Set<PackageDefinition> packagesToInstall = new HashSet<PackageDefinition>();
			final Set<PackageDefinition> packagesToRemove = new HashSet<PackageDefinition>();

			final IPackageManager packageManager = P2Activator.getInstance().getPackageManager();
			final Collection<PackageDefinition> packages = packageManager.getPackages();
			for (final PackageDefinition packageDefinition : packages) {
				// check filter
				if (StringUtils.isNotBlank(packageDefinition.getNodeFilter())) {
					try {
						if (!nodeEnvironment.matches(packageDefinition.getNodeFilter())) {
							if (P2Debug.nodeInstallation) {
								LOG.debug("Ignoring package {}. Not applicable to current node.", packageDefinition.getId());
							}
							continue;
						}
					} catch (final InvalidSyntaxException e) {
						if (P2Debug.nodeInstallation) {
							LOG.debug("Ignoring package {}. Error in node filter syntax: {}", packageDefinition.getId(), e.getMessage());
						}
						continue;
					}
				}

				// check if package has been rolled out on local node
				final boolean installed = PackageInstallState.isInstalled(agent, packageDefinition);

				// add as roll-out or removal
				if (packageManager.isMarkedForInstall(packageDefinition)) {
					if (!installed) {
						if (P2Debug.nodeInstallation) {
							LOG.debug("Found new package to install: {}", packageDefinition.getId());
						}
						packagesToInstall.add(packageDefinition);
					} else {
						if (P2Debug.nodeInstallation) {
							LOG.debug("Package {} already installed. Will be ignored.", packageDefinition.getId());
						}
					}
				} else if (packageManager.isMarkedForUninstall(packageDefinition)) {
					if (installed) {
						if (P2Debug.nodeInstallation) {
							LOG.debug("Found package that should be removed: {}", packageDefinition.getId());
						}
						packagesToRemove.add(packageDefinition);
					} else {
						if (P2Debug.nodeInstallation) {
							LOG.debug("Package {} not installed. Will be ignored.", packageDefinition.getId());
						}
					}
				} else {
					if (P2Debug.nodeInstallation) {
						LOG.debug("Package {} neither marked for installation nor for removal. Will be ignored.", packageDefinition.getId());
					}
				}
			}

			// check if there is something to install
			if (packagesToInstall.isEmpty() && packagesToRemove.isEmpty()) {
				if (P2Debug.nodeInstallation) {
					LOG.debug("Nothing to install or remove.");
				}
				return Status.OK_STATUS;
			}

			// schedule installer job
			LOG.info("Pending software package modifications found. Scheduling software installation for local node.");
			final PackageInstallerJob packageInstallerJob = new PackageInstallerJob(packagesToInstall, packagesToRemove);
			packageInstallerJob.schedule(500l);

			// done
			return Status.OK_STATUS;
		} catch (final IllegalStateException e) {
			LOG.warn("Unable to for new software packages. System does not seem to be ready. {}", ExceptionUtils.getRootCauseMessage(e));
			return Status.CANCEL_STATUS;
		} catch (final Exception e) {
			LOG.error("Error while checking for new or revoked software packages. {}", ExceptionUtils.getRootCauseMessage(e), e);
			return Status.CANCEL_STATUS;
		} finally {
			// close agent
			if (null != agent) {
				agent.stop();
			}
		}
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		try {
			final IStatus status = doRun(monitor);
			if (!status.isOK()) {
				// implement a back-off sleeping time
				sleepTime = Math.min(sleepTime * 2, MAX_SLEEP_TIME);
			} else {
				// reset sleep time
				sleepTime = INITIAL_SLEEP_TIME;
			}
			return status;
		} finally {
			// reschedule
			if (P2Debug.nodeInstallation) {
				LOG.debug("Rescheduling installer to run again in {} minutes", TimeUnit.MILLISECONDS.toMinutes(sleepTime));
			}
			schedule(sleepTime);
		}
	}

}
