/*******************************************************************************
 * Copyright (c) 2010, 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.context.preferences;

import java.util.Collection;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.context.preferences.IRuntimeContextPreferences;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;

import org.osgi.service.prefs.BackingStoreException;

/**
 * A Repository which stores data in {@link IRuntimeContextPreferences}.
 * <p>
 * The repository may be used for small objects. It does not offer any
 * transaction or other rich persistence capabilities.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class ContextPreferencesRepository extends Repository implements IContextPreferencesRepositoryConstants {

	/**
	 * Creates a new map repository instance.
	 * 
	 * @param repositoryId
	 *            the repository id
	 * @param repositoryType
	 *            the repository type
	 * @param metrics
	 *            the metrics
	 * @throws IllegalArgumentException
	 */
	protected ContextPreferencesRepository(final String repositoryId, final RepositoryProvider repositoryType, final MetricSet metrics) throws IllegalArgumentException {
		super(repositoryId, repositoryType, metrics);
	}

	/**
	 * Returns the stored data for the specified {@code key}.
	 * 
	 * @param key
	 *            the lookup key of the data (must validate using
	 *            {@link IdHelper#isValidId(String)})
	 * @return the stored data (maybe <code>null</code> if no data is stored for
	 *         the specified {@code key})
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 */
	public abstract byte[] get(String key) throws IllegalArgumentException;

	/**
	 * Returns a collection of all known keys.
	 * 
	 * @return an unmodifiable collection of all keys that have data in the
	 *         repository
	 * @throws BackingStoreException
	 *             if an exception occurred accessing the preferences store
	 */
	public abstract Collection<String> getKeys() throws BackingStoreException;

	/**
	 * Removes any stored data for the specified {@code key}.
	 * 
	 * @param key
	 *            the lookup key of the data (must validate using
	 *            {@link IdHelper#isValidId(String)})
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 * @throws BackingStoreException
	 *             if an exception occurred saving the preferences store
	 */
	public abstract void remove(String key) throws IllegalArgumentException, BackingStoreException;

	/**
	 * Stores the specified {@code data} using the specified {@code key}.
	 * <p>
	 * If the key already exists its data will be overwritten.
	 * </p>
	 * 
	 * @param key
	 *            the lookup key of the data (must validate using
	 *            {@link IdHelper#isValidId(String)})
	 * @param data
	 *            the data to store (may not be <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 * @throws BackingStoreException
	 *             if an exception occurred saving the preferences store
	 */
	public abstract void store(String key, byte[] data) throws IllegalArgumentException, BackingStoreException;
}
