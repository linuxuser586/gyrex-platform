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

import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.persistence.jdbc.storage.JdbcRepository;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;

/**
 * A repository which uses EclipseLink for persisting objects into databases.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public abstract class EclipseLinkRepository extends JdbcRepository {

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

}
