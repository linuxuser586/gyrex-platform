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

import org.eclipse.gyrex.p2.internal.P2Activator;
import org.eclipse.gyrex.p2.packages.PackageDefinition;

import org.eclipse.core.runtime.IPath;

import org.osgi.service.prefs.BackingStoreException;

/**
 * Helper for working with package install state.
 */
public class PackageInstallState {

	private static IPath getPackageFile(final PackageDefinition packageDefinition) {
		return P2Activator.getInstance().getConfigLocation().append(P2Activator.SYMBOLIC_NAME).append("packages").append(packageDefinition.getId());
	}

	public static boolean isInstalled(final PackageDefinition packageDefinition) throws BackingStoreException {
		return getPackageFile(packageDefinition).toFile().isFile();
	}

	/**
	 * Creates a new instance.
	 */
	private PackageInstallState() {
		// empty
	}

}
