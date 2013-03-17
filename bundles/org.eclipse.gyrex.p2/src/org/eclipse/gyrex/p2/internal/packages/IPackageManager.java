/*******************************************************************************
 * Copyright (c) 2011, 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.p2.internal.packages;

import java.util.Collection;

import org.eclipse.core.runtime.IStatus;

/**
 * Manages the available packages that may be installed on nodes in the cloud.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IPackageManager {

	PackageDefinition getPackage(String id);

	Collection<PackageDefinition> getPackages();

	boolean isMarkedForInstall(PackageDefinition packageDefinition);

	boolean isMarkedForUninstall(PackageDefinition packageDefinition);

	void markedForInstall(PackageDefinition packageDefinition);

	void markedForUninstall(PackageDefinition packageDefinition);

	void removePackage(String id);

	void savePackage(PackageDefinition packageDefinition);

	IStatus verifyPackageIsModifiable(String id) throws IllegalStateException, IllegalArgumentException;

}
