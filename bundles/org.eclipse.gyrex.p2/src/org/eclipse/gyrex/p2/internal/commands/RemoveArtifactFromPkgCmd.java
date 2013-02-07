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

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.p2.internal.packages.InstallableUnitReference;
import org.eclipse.gyrex.p2.internal.packages.PackageDefinition;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.metadata.Version;

import org.kohsuke.args4j.Argument;

public final class RemoveArtifactFromPkgCmd extends BaseSwCmd {

	@Argument(index = 0, usage = "the package id", required = true, metaVar = "ID")
	String id;

	@Argument(index = 1, usage = "the installable unit id", required = true, metaVar = "IU")
	String iuId;

	@Argument(index = 2, usage = "an optional installable unit version", required = false, metaVar = "VERSION")
	String iuVersion;

	public RemoveArtifactFromPkgCmd() {
		super("<packageId> <installableUnitId> [<installUnitVersion>] - removes an installable unit from a package");
	}

	@Override
	protected void doExecute() throws Exception {
		if (!IdHelper.isValidId(id)) {
			printf("ERROR: invalid package id");
			return;
		}

		final PackageDefinition packageDefinition = getPackageManager().getPackage(id);
		if (null == packageDefinition) {
			printf("ERROR: package not found");
			return;
		}

		// verify the package is not marked for roll-out and has been revoked 48 hours ago
		final IStatus modifiable = getPackageManager().verifyPackageIsModifiable(id);
		if (!modifiable.isOK()) {
			printf("ERROR: %s", modifiable.getMessage());
			return;
		}

		final InstallableUnitReference iu = new InstallableUnitReference();
		if (!IdHelper.isValidId(iuId)) {
			printf("ERROR: invalid installable unit id");
			return;
		}
		iu.setId(iuId);

		if (null != iuVersion) {
			try {
				iu.setVersion(Version.create(iuVersion));
			} catch (final IllegalArgumentException e) {
				printf("ERROR: invalid installable unit version: %s", e.getMessage());
				return;
			}
		}

		if (!packageDefinition.removeComponentToInstall(iu)) {
			printf("ERROR: installable unit not found in package");
			return;
		}

		getPackageManager().savePackage(packageDefinition);
		printf("package updated");
	}
}