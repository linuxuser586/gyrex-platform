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
package org.eclipse.gyrex.persistence.eclipselink;

import javax.persistence.EntityManagerFactory;

import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.content.RepositoryContentType;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;

/**
 * A repository which uses EclipseLink for persisting objects into databases.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class EclipseLinkRepository extends Repository {

	/**
	 * Creates a new instance.
	 * 
	 * @param repositoryId
	 * @param repositoryProvider
	 * @param metrics
	 * @noreference This constructor is not intended to be referenced by
	 *              clients.
	 */
	protected EclipseLinkRepository(final String repositoryId, final RepositoryProvider repositoryProvider, final MetricSet metrics) {
		super(repositoryId, repositoryProvider, metrics);
	}

	/**
	 * Returns an {@link EntityManagerFactory} for the specified content type.
	 * <p>
	 * If the content type contains a parameter
	 * {@link RepositoryContentType#getParameter(String)
	 * <code>persistenceUnitName</code>} its value will be used. If no such
	 * parameter is specified the
	 * {@link RepositoryContentType#getMediaTypeSubType() media type sub type}
	 * will be used.
	 * </p>
	 * <p>
	 * Within a dynamic system, the returned {@link EntityManagerFactory} may
	 * become invalid at any point in time. Thus, clients need to check
	 * regularly if the entity manager factory is still
	 * {@link EntityManagerFactory#isOpen() open}. If not they may abort any
	 * operation or acquire a fresh instance.
	 * </p>
	 * <p>
	 * If the system is not able to create an {@link EntityManagerFactory}
	 * instance due to missing service dependencies and
	 * {@link IllegalStateException} will be thrown.
	 * </p>
	 * 
	 * @param contentType
	 *            the content type for determining the persistent unit
	 * @return the entity manager factory
	 */
	public abstract EntityManagerFactory getEntityManagerFactory(final RepositoryContentType contentType) throws IllegalArgumentException, IllegalStateException;

}
