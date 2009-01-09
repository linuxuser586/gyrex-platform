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
package org.eclipse.cloudfree.persistence.derby.internal;

import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource;
import org.eclipse.cloudfree.persistence.jdbc.internal.JdbcRepositoryImpl;
import org.eclipse.cloudfree.persistence.jdbc.storage.JdbcRepository;
import org.eclipse.cloudfree.persistence.storage.Repository;
import org.eclipse.cloudfree.persistence.storage.provider.RepositoryProvider;
import org.eclipse.cloudfree.persistence.storage.settings.IRepositoryPreferences;

/**
 * Embedded Derby Database.
 */
public class DerbyRepositoryType extends RepositoryProvider {

	/** the plug-in id */
	public static final String TYPE_ID = "org.eclipse.cloudfree.persistence.derby.type";

	/**
	 * Creates a new instance.
	 * 
	 * @param repositoryTypeId
	 */
	DerbyRepositoryType() {
		super(TYPE_ID, JdbcRepository.class);
	}

	private EmbeddedConnectionPoolDataSource createDataSource(final String databaseName) {
		final EmbeddedConnectionPoolDataSource embeddedConnectionPoolDataSource = new EmbeddedConnectionPoolDataSource();
		embeddedConnectionPoolDataSource.setDatabaseName(databaseName);
		embeddedConnectionPoolDataSource.setCreateDatabase("create");
		return embeddedConnectionPoolDataSource;
	}

	private Repository createJdbcRepository(final String repositoryId, final EmbeddedConnectionPoolDataSource embeddedConnectionPoolDataSource) {
		return new JdbcRepositoryImpl(repositoryId, this, embeddedConnectionPoolDataSource, 0);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.persistence.storage.provider.RepositoryProvider#newRepositoryInstance(java.lang.String, org.eclipse.cloudfree.persistence.storage.settings.IRepositoryPreferences)
	 */
	@Override
	public Repository createRepositoryInstance(final String repositoryId, final IRepositoryPreferences repositoryPreferences) {
		final EmbeddedConnectionPoolDataSource embeddedConnectionPoolDataSource = createDataSource(repositoryId);
		return createJdbcRepository(repositoryId, embeddedConnectionPoolDataSource);
	}
}
