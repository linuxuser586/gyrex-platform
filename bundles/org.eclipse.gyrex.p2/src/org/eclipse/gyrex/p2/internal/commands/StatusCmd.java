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

import java.util.Arrays;

import org.eclipse.gyrex.cloud.environment.INodeEnvironment;
import org.eclipse.gyrex.cloud.services.locking.ILockService;
import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.p2.internal.P2Activator;
import org.eclipse.gyrex.p2.internal.installer.PackageInstallState;
import org.eclipse.gyrex.p2.internal.installer.PackageInstallerJob;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;

import org.osgi.service.prefs.BackingStoreException;

import org.apache.commons.lang.StringUtils;

public final class StatusCmd extends Command {

	public StatusCmd() {
		super("prints status information about the sw process");
	}

	@Override
	protected void doExecute() throws Exception {
		IProvisioningAgent agent = null;
		try {
			// get agent
			agent = P2Activator.getInstance().getService(IProvisioningAgentProvider.class).createAgent(null);
			if (agent == null)
				throw new IllegalStateException("The current system has not been provisioned using p2. Unable to acquire provisioning agent.");

			printInstalledPackages(agent);

		} catch (final Exception e) {
			printf("Unable to determine packages installed on node %s. %s", getNodeId(), e.getMessage());
		} finally {
			// close agent
			if (null != agent) {
				agent.stop();
			}
		}

		final String activeSessionId = PackageInstallState.getActiveInstallSessionId();
		if (StringUtils.isNotBlank(activeSessionId)) {
			printf("Active installation on %s (%s)!", getNodeId(), activeSessionId);
		} else {
			printf("No installation active on %s!", getNodeId());
		}

		final ILockService lockService = P2Activator.getInstance().getService(ILockService.class);
		final IStatus lockStatus = lockService.getDurableLockStatus(PackageInstallerJob.ID_INSTALL_LOCK);
		printf("Installation lock: %s", lockStatus.getMessage());
		if (lockStatus.isMultiStatus()) {
			for (final IStatus status : lockStatus.getChildren()) {
				printf("   %s", status.getMessage());
			}
		}
	}

	private String getNodeId() {
		try {
			return P2Activator.getInstance().getService(INodeEnvironment.class).getNodeId();
		} catch (final IllegalStateException e) {
			return "this node";
		}
	}

	public void printInstalledPackages(final IProvisioningAgent agent) throws BackingStoreException {
		// check if the package IU is available in the current profile
		final IProfileRegistry profileRegistry = (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);
		if (null == profileRegistry)
			throw new IllegalStateException("The current system has not been provisioned using p2. Unable to acquire profile registry.");
		final IProfile profile = profileRegistry.getProfile(IProfileRegistry.SELF);
		if (null == profile)
			throw new IllegalStateException("The current system has not been provisioned using p2. Unable to get profile.");

		final IQueryResult<IInstallableUnit> result = profile.available(QueryUtil.createIUPropertyQuery(PackageInstallState.IU_PROP_PACKAGE, Boolean.TRUE.toString()), null);
		if (result.isEmpty()) {
			printf("No packages installed on %s.", getNodeId());
			return;
		}

		printf("The following packages are available on this node:");
		final IInstallableUnit[] packageIUs = result.toArray(IInstallableUnit.class);
		Arrays.sort(packageIUs);
		for (final IInstallableUnit unit : packageIUs) {
			printf("   %s, %s", PackageInstallState.getPackageId(unit), unit.getVersion());
		}
	}

}