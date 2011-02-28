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
package org.eclipse.gyrex.persistence.internal.storage;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.persistence.storage.RepositoryMetadata;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.osgi.service.prefs.BackingStoreException;

/**
 * {@link IRepositoryPreferences} based implementation of
 * {@link RepositoryMetadata} which may be used by repository providers.
 * <p>
 * This class may be instantiated by repository implementation contributors.
 * However, it is typically not referenced directly outside Gyrex.
 * </p>
 */
public final class RepositoryPreferencesBasedMetadata extends RepositoryMetadata {

	private static final String METADATA = "metadata";

	private final IRepositoryPreferences preferences;
	private final IPath nodePath;
	private final String metadataId;
	private final String repositoryId;

	/**
	 * Creates a new instance.
	 * 
	 * @param preferences
	 *            the preferences to store the metadata
	 * @param metadataId
	 *            the metadata id
	 * @param repositoryId
	 *            the repository id
	 */
	public RepositoryPreferencesBasedMetadata(final IRepositoryPreferences preferences, final String metadataId, final String repositoryId) {
		if (!IdHelper.isValidId(metadataId)) {
			throw new IllegalArgumentException(String.format("invalid metadata id: %s (repository %s)", metadataId, repositoryId));
		}
		if (!IdHelper.isValidId(repositoryId)) {
			throw new IllegalArgumentException(String.format("invalid repository id: %s (metadata %s)", repositoryId, metadataId));
		}
		this.preferences = preferences;
		this.metadataId = metadataId;
		this.repositoryId = repositoryId;
		nodePath = new Path(METADATA).append(metadataId);
	}

	@Override
	public boolean exists() {
		try {
			final String[] childrenNames = preferences.getChildrenNames(METADATA);
			for (final String childName : childrenNames) {
				if (metadataId.equals(childName)) {
					return true;
				}

			}
			return false; // not found
		} catch (final Exception e) {
			// ignore, will return false below
			return false;
		}
	}

	@Override
	public void flush() throws BackingStoreException {
		preferences.flush();
	}

	@Override
	public byte[] get(final String key) throws IllegalArgumentException {
		return preferences.getByteArray(getPathToKey(key), null);
	}

	@Override
	public String getId() {
		return metadataId;
	}

	@Override
	public Collection<String> getKeys() throws BackingStoreException {
		return Collections.unmodifiableCollection(Arrays.asList(preferences.getChildrenNames(nodePath.toString())));
	}

	private String getPathToKey(final String key) {
		if (!IdHelper.isValidId(key)) {
			throw new IllegalArgumentException(String.format("invalid key: %s (metadata %s for repository %s)", key, metadataId, repositoryId));
		}
		return nodePath.append(key).toString();
	}

	@Override
	public void put(final String key, final byte[] data) throws IllegalArgumentException {
		if (data == null) {
			throw new IllegalArgumentException(String.format("data must not be null (key %s, metadata %s for repository %s)", key, metadataId, repositoryId));
		}
		preferences.putByteArray(getPathToKey(key), data, false);
	}

	@Override
	public void remove() throws BackingStoreException {
		preferences.remove(nodePath.toString());
	}

	@Override
	public void remove(final String key) throws IllegalArgumentException {
		preferences.remove(getPathToKey(key));
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("RepositoryPreferencesBasedMetadata [metadataId=").append(metadataId).append(", repositoryId=").append(repositoryId).append("]");
		return builder.toString();
	}

}
