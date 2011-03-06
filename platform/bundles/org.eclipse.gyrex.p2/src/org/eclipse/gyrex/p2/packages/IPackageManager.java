/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.p2.packages;

import java.util.Collection;

/**
 * Manages the available packages that may be installed on nodes in the cloud.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IPackageManager {

	/**
	 * @param id
	 * @return
	 */
	PackageDefinition getPackage(String id);

	Collection<PackageDefinition> getPackages();

	/**
	 * @param packageDefinition
	 * @return
	 */
	boolean isRolledOut(PackageDefinition packageDefinition);

	/**
	 * @param id
	 */
	void removePackage(String id);

	/**
	 * @param packageDefinition
	 */
	void savePackage(PackageDefinition packageDefinition);

}
