/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.eclipselink.internal;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;

import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.persistence.eclipselink.EclipseLinkRepository;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentType;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;

/**
 * {@link EclipseLinkRepository} implementation
 */
public class EclipseLinkRepositoryImpl extends EclipseLinkRepository {

	/**
	 * Creates a new instance.
	 * 
	 * @param repositoryId
	 * @param repositoryProvider
	 * @param metrics
	 */
	public EclipseLinkRepositoryImpl(final String repositoryId, final RepositoryProvider repositoryProvider, final MetricSet metrics) {
		super(repositoryId, repositoryProvider, new EclipseLinkRepositoryMetrics(createMetricsId(repositoryProvider, repositoryId), repositoryId, repositoryProvider, "new", "created"));
	}

	@Override
	protected void doClose() {
		super.doClose();
	}

	@Override
	public Connection getConnection() throws SQLException {
//		final EntityManagerFactoryBuilder builder = EclipseLinkActivator.getInstance().getService(EntityManagerFactoryBuilder.class);
//		builder.createEntityManagerFactory(null).getCriteriaBuilder().
//		JpaHelper.getServerSession(null)
//		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Connection getConnection(final long timeout, final TimeUnit timeUnit) throws SQLException, InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	public EntityManager getEntityManager(final RepositoryContentType contentType) {
//		final String persistenceUnitName
		return null;
	}
}
