/*******************************************************************************
 * Copyright (c) 2008, 2009 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.nondurable.map.internal;

import org.eclipse.gyrex.persistence.nondurable.map.MapRepository;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;

/**
 * A repository type which stores objects in a map in memory.
 */
public class MapRepositoryType extends RepositoryProvider {

	/**
	 * Creates a new instance.
	 * 
	 * @param id
	 */
	public MapRepositoryType() {
		super("org.eclipse.gyrex.persistence.nondurable.map", MapRepository.class);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider#newRepositoryInstance(java.lang.String, org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences)
	 */
	@Override
	public Repository createRepositoryInstance(final String repositoryId, final IRepositoryPreferences repositoryPreferences) {
		return new MapRepositoryImpl(repositoryId, this);
	}

}
