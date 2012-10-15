/*******************************************************************************
 * Copyright (c) 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.tests.internal;

import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;

public final class DummyRepo extends Repository {

	public DummyRepo(final String repositoryId, final RepositoryProvider repositoryProvider) throws IllegalArgumentException {
		super(repositoryId, repositoryProvider, new MetricSet(createMetricsId(repositoryProvider, repositoryId), "Test") {

		});
	}

}