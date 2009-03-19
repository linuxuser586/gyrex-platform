/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.derby.tests;

import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource;
import org.eclipse.gyrex.persistence.jdbc.internal.SimpledPooledJdbcRepositoryImpl;
import org.eclipse.gyrex.persistence.jdbc.storage.JdbcRepository;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;

public class MockRepositoryType extends RepositoryProvider {

	public MockRepositoryType() {
		super(MockRepositoryType.class.getName(), JdbcRepository.class);
	}

	private EmbeddedConnectionPoolDataSource createDataSource(final String databaseName) {
		final EmbeddedConnectionPoolDataSource embeddedConnectionPoolDataSource = new EmbeddedConnectionPoolDataSource();
		embeddedConnectionPoolDataSource.setDatabaseName(databaseName);
		embeddedConnectionPoolDataSource.setCreateDatabase("create");
		return embeddedConnectionPoolDataSource;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider#newRepositoryInstance(java.lang.String, org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences)
	 */
	@Override
	public Repository createRepositoryInstance(final String repositoryId, final IRepositoryPreferences repositoryPreferences) {
		return new SimpledPooledJdbcRepositoryImpl(repositoryId, this, createDataSource(repositoryId), 0);
	}
}
