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

import org.eclipse.gyrex.persistence.mongodb.IMondoDbRepositoryConstants;
import org.eclipse.gyrex.persistence.mongodb.MongoDbRepository;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;

/**
 * Repository provider for {@link MongoDbRepository}.
 */
public class MongoDbRepositoryProvider extends RepositoryProvider {

	/**
	 * Creates a new instance.
	 */
	public MongoDbRepositoryProvider() {
		super(IMondoDbRepositoryConstants.PROVIDER_ID, MongoDbRepository.class);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Repository createRepositoryInstance(final String repositoryId, final IRepositoryPreferences repositoryPreferences) {
		return new MongoDbRepositoryImpl(repositoryId, this, repositoryPreferences);
	}

}
