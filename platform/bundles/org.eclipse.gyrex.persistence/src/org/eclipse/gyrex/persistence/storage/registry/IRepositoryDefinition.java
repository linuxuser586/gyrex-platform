/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Mike Tschierschke - initial API and implementation
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.storage.registry;

import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;

/**
 * A definition of a a {@link Repository}.
 */
public interface IRepositoryDefinition {

	/**
	 * Returns {@link RepositoryProvider#getProviderId() the repository provider
	 * identifier}.
	 * 
	 * @return the id of the {@link RepositoryProvider}
	 */
	public String getProviderId();

	/**
	 * Returns the repository preferences which should be used for storing and
	 * retrieving repository specific configuration.
	 * 
	 * @return the repository preferences
	 * @see IRepositoryPreferences
	 */
	public IRepositoryPreferences getRepositoryPreferences();
}
