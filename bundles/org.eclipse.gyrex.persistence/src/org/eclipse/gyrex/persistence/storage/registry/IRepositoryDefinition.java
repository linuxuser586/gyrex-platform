/*******************************************************************************
 * Copyright (c) 2010, 2011 AGETO Service GmbH and others.
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

import java.util.Collection;

import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;

import org.eclipse.core.runtime.IAdaptable;

/**
 * A definition of a a {@link Repository}.
 */
public interface IRepositoryDefinition extends IAdaptable {

	/**
	 * Adds a tag to the repository.
	 * <p>
	 * This method has no effect if the tag is already present.
	 * </p>
	 * 
	 * @param tag
	 *            a tag to add
	 */
	void addTag(String tag);

	/**
	 * Returns {@link RepositoryProvider#getProviderId() the repository provider
	 * identifier}.
	 * 
	 * @return the id of the {@link RepositoryProvider}
	 */
	String getProviderId();

	/**
	 * Returns {@link Repository#getRepositoryId() the repository identifier}.
	 * 
	 * @return the id of the {@link Repository}
	 */
	String getRepositoryId();

	/**
	 * Returns the repository preferences which should be used for storing and
	 * retrieving repository specific configuration.
	 * 
	 * @return the repository preferences
	 * @see IRepositoryPreferences
	 */
	IRepositoryPreferences getRepositoryPreferences();

	/**
	 * Returns a list of tags associated with the repository.
	 * 
	 * @return an unmodifiable collection of string tags associated with the
	 *         repository
	 */
	Collection<String> getTags();

	/**
	 * Removes a tag from the repository.
	 * <p>
	 * This method has no effect if the tag is not present.
	 * </p>
	 * 
	 * @param tag
	 *            a tag to remove
	 */
	void removeTag(String tag);
}
