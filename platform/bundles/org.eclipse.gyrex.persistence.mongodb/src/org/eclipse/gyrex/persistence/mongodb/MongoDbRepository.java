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
package org.eclipse.gyrex.persistence.mongodb;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;

import com.mongodb.DB;

/**
 * A repository which uses the MongoDb Java Driver to connect to a MongoDb
 * repository.
 * <p>
 * The repository essentially provides access to a ready-configured Mongo
 * {@link DB} object (see {@link #getDb()}). Thus, a repository maps one-on-one
 * to a MondoDB database.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class MongoDbRepository extends Repository {

	/**
	 * Creates a new instance.
	 * 
	 * @param repositoryId
	 *            the repository id (may not be <code>null</code>, must conform
	 *            to {@link IdHelper#isValidId(String)})
	 * @param repositoryProvider
	 *            the repository provider (may not be <code>null</code>)
	 * @param metrics
	 *            the metrics used by this repository (may not be
	 *            <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if an invalid argument was specified
	 * @noreference This constructor is not intended to be referenced by
	 *              clients.
	 */
	protected MongoDbRepository(final String repositoryId, final RepositoryProvider repositoryProvider, final MetricSet metrics) throws IllegalArgumentException {
		super(repositoryId, repositoryProvider, metrics);
	}

	/**
	 * Returns the Mongo {@link DB} object represented by the repository.
	 * <p>
	 * Note, clients may not be able to call all methods on the returned
	 * {@link DB} object. Security restrictions may not allow all code to
	 * perform all operations on the MongoDB.
	 * </p>
	 * 
	 * @return
	 */
	public abstract DB getDb();
}
