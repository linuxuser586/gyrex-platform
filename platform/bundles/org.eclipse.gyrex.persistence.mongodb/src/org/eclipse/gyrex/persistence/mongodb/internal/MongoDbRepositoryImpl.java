/**
 * Copyright (c) 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.persistence.mongodb.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.persistence.mongodb.MongoDbRepository;
import org.eclipse.gyrex.persistence.storage.exceptions.ResourceFailureException;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;

import com.mongodb.DB;
import com.mongodb.Mongo;

/**
 * {@link MongoDbRepository} implementation.
 */
public class MongoDbRepositoryImpl extends MongoDbRepository {

	private final String databaseName;
	private final String poolId;
	private final AtomicReference<Mongo> mongoRef = new AtomicReference<Mongo>();

	/**
	 * Creates a new instance.
	 * 
	 * @param repositoryId
	 * @param repositoryProvider
	 * @param repositoryPreferences
	 * @throws IllegalArgumentException
	 */
	public MongoDbRepositoryImpl(final String repositoryId, final MongoDbRepositoryProvider repositoryProvider, final IRepositoryPreferences repositoryPreferences) throws IllegalArgumentException {
		super(repositoryId, repositoryProvider, null);

		databaseName = repositoryPreferences.get("db", null);
		poolId = repositoryPreferences.get("poolId", null);
	}

	@Override
	protected void doClose() {
		final Mongo mongo = mongoRef.getAndSet(null);
		if (null != mongo) {
			MongoDbActivator.getInstance().getRegistry().unget(poolId);
		}
		super.doClose();
	}

	private Mongo ensureInitialized() {
		if (null == poolId) {
			throw new IllegalStateException("pool not configured");
		}

		Mongo mongo = mongoRef.get();
		while (mongo == null) {
			mongo = MongoDbActivator.getInstance().getRegistry().get(poolId);
			if (null == mongo) {
				throw new ResourceFailureException(String.format("Pool '%s' not available!", String.valueOf(poolId)));
			}

			if (!mongoRef.compareAndSet(null, mongo)) {
				mongo = mongoRef.get();
			}
		}

		return mongo;
	}

	@Override
	public DB getDb() {
		if (null == databaseName) {
			throw new IllegalStateException("database not configured");
		}
		return ensureInitialized().getDB(databaseName);
	}
}
