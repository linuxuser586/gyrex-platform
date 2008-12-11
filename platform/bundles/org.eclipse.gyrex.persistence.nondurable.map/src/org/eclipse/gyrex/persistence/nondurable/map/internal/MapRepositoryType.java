/**
 * Copyright (c) 2008 AGETO Service GmbH and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.cloudfree.persistence.nondurable.map.internal;

import org.eclipse.cloudfree.persistence.storage.Repository;
import org.eclipse.cloudfree.persistence.storage.settings.IRepositoryPreferences;
import org.eclipse.cloudfree.persistence.storage.type.RepositoryType;

/**
 * A repository type which stores objects in a map in memory.
 */
public class MapRepositoryType extends RepositoryType {

	/**
	 * Creates a new instance.
	 * 
	 * @param id
	 */
	public MapRepositoryType() {
		super("org.eclipse.cloudfree.persistence.nondurable.map");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.persistence.storage.type.RepositoryType#newRepositoryInstance(java.lang.String, org.eclipse.cloudfree.persistence.storage.settings.IRepositoryPreferences)
	 */
	@Override
	public Repository newRepositoryInstance(final String repositoryId, final IRepositoryPreferences repositoryPreferences) {
		return new MapRepositoryImpl(repositoryId, this);
	}

}
