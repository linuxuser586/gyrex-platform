/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.context.preferences.internal;

import java.util.Set;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.preferences.IRuntimeContextPreferences;
import org.eclipse.gyrex.persistence.context.preferences.ContextPreferencesRepository;
import org.eclipse.gyrex.persistence.storage.Repository;

import org.eclipse.osgi.util.NLS;

import org.osgi.service.prefs.BackingStoreException;

/**
 * {@link ContextPreferencesRepository} implementation.
 */
public class ContextPreferencesRepositoryImpl extends ContextPreferencesRepository {

	private static final String QUALIFIER_PREFIX = ".repository__";

	private final IRuntimeContext context;
	private final String qualifier;

	/**
	 * Creates a new instance.
	 * 
	 * @param repositoryId
	 * @param repositoryType
	 * @param context
	 * @param metrics
	 * @throws IllegalArgumentException
	 */
	public ContextPreferencesRepositoryImpl(final String repositoryId, final ContextPreferencesRepositoryType repositoryType, final IRuntimeContext context) throws IllegalArgumentException {
		super(repositoryId, repositoryType, new ContextPreferencesRepositoryMetrics(createMetricsId(repositoryType, repositoryId)));
		this.context = context;
		qualifier = QUALIFIER_PREFIX.concat(repositoryId);
	}

	@Override
	public byte[] get(final String key) throws IllegalArgumentException {
		return context.getPreferences().getByteArray(qualifier, verifyKey(key), null);
	}

	@Override
	public Set<String> getKeys() {
		return null;
	}

	@Override
	public void remove(final String key) throws IllegalArgumentException, BackingStoreException {
		final IRuntimeContextPreferences preferences = context.getPreferences();
		preferences.remove(qualifier, verifyKey(key));
		preferences.flush(qualifier);
	}

	@Override
	public void store(final String key, final byte[] data) throws IllegalArgumentException, BackingStoreException {
		if (data == null) {
			throw new IllegalArgumentException(NLS.bind("data must not be null (key {0}, repository {1})", key, getRepositoryId()));
		}
		final IRuntimeContextPreferences preferences = context.getPreferences();
		preferences.putByteArray(qualifier, verifyKey(key), data, false);
		preferences.flush(qualifier);
	}

	private String verifyKey(final String key) {
		if (!Repository.isValidId(key)) {
			throw new IllegalArgumentException(NLS.bind("invalid key: {0} (repository {1})", key, getRepositoryId()));
		}
		return key;
	}

}
