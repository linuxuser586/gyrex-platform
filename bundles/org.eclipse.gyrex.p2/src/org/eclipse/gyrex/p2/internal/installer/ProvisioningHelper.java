/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sonatype, Inc. - ongoing development
 ******************************************************************************/
package org.eclipse.gyrex.p2.internal.installer;

import java.net.URI;
import java.util.Collection;

import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.engine.ProvisioningContext;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.operations.UninstallOperation;
import org.eclipse.equinox.p2.operations.UpdateOperation;

/**
 * A litter helper for provisioning operations
 */
public class ProvisioningHelper {

	private final ProvisioningSession session;
	private String profileId;

	/**
	 * Creates a new instance of the provisioning user interface.
	 * 
	 * @param session
	 *            The current provisioning session
	 * @param profileId
	 *            The profile that this user interface is operating on
	 * @param policy
	 *            The user interface policy settings to use
	 */
	public ProvisioningHelper(final ProvisioningSession session, final String profileId) {
		this.profileId = profileId;
		if (profileId == null) {
			this.profileId = IProfileRegistry.SELF;
		}
		this.session = session;
	}

	/**
	 * Return an install operation that describes installing the specified
	 * IInstallableUnits from the provided list of repositories.
	 * 
	 * @param iusToInstall
	 *            the IInstallableUnits to be installed
	 * @param repositories
	 *            the repositories to use for the operation
	 * @return the install operation
	 */
	public InstallOperation getInstallOperation(final Collection<IInstallableUnit> iusToInstall, final URI[] repositories) {
		final InstallOperation op = new InstallOperation(getSession(), iusToInstall);
		op.setProfileId(getProfileId());
		op.setProvisioningContext(makeProvisioningContext(repositories));
		return op;
	}

	/**
	 * Return the profile id that should be assumed for this ProvisioningUI if
	 * no other id is otherwise specified. Some UI classes are assigned a
	 * profile id, while others are not. For those classes that are not assigned
	 * a current profile id, this id can be used to obtain one.
	 * 
	 * @return a profile id
	 */
	public String getProfileId() {
		return profileId;
	}

	/**
	 * Return the repository tracker that should be used to add, remove, and
	 * track the statuses of known repositories.
	 * 
	 * @return the repository tracker, must not be <code>null</code>
	 */
	public RepositoryTracker getRepositoryTracker() {
		return (RepositoryTracker) session.getProvisioningAgent().getService(RepositoryTracker.class.getName());
	}

	/**
	 * Return the provisioning session that should be used to obtain
	 * provisioning services.
	 * 
	 * @return the provisioning session, must not be <code>null</code>
	 */
	public ProvisioningSession getSession() {
		return session;
	}

	/**
	 * Return an uninstall operation that describes uninstalling the specified
	 * IInstallableUnits, using the supplied repositories to replace any
	 * metadata that must be retrieved for the uninstall.
	 * 
	 * @param iusToUninstall
	 *            the IInstallableUnits to be installed
	 * @param repositories
	 *            the repositories to use for the operation
	 * @return the uninstall operation
	 */
	public UninstallOperation getUninstallOperation(final Collection<IInstallableUnit> iusToUninstall, final URI[] repositories) {
		final UninstallOperation op = new UninstallOperation(getSession(), iusToUninstall);
		op.setProfileId(getProfileId());
		op.setProvisioningContext(makeProvisioningContext(repositories));
		return op;
	}

	/**
	 * Return an update operation that describes updating the specified
	 * IInstallableUnits from the provided list of repositories.
	 * 
	 * @param iusToUpdate
	 *            the IInstallableUnits to be updated
	 * @param repositories
	 *            the repositories to use for the operation
	 * @return the update operation
	 */
	public UpdateOperation getUpdateOperation(final Collection<IInstallableUnit> iusToUpdate, final URI[] repositories) {
		final UpdateOperation op = new UpdateOperation(getSession(), iusToUpdate);
		op.setProfileId(getProfileId());
		op.setProvisioningContext(makeProvisioningContext(repositories));
		return op;
	}

	/**
	 * Return a boolean indicating whether the receiver has scheduled any
	 * operations for the profile under management.
	 * 
	 * @return <code>true</code> if other provisioning operations have been
	 *         scheduled, <code>false</code> if there are no operations
	 *         scheduled.
	 */
	public boolean hasScheduledOperations() {
		return getSession().hasScheduledOperationsFor(profileId);
	}

	private ProvisioningContext makeProvisioningContext(final URI[] repos) {
		if (repos != null) {
			final ProvisioningContext context = new ProvisioningContext(getSession().getProvisioningAgent());
			context.setMetadataRepositories(repos);
			context.setArtifactRepositories(repos);
			return context;
		}
		// look everywhere
		return new ProvisioningContext(getSession().getProvisioningAgent());
	}

}
