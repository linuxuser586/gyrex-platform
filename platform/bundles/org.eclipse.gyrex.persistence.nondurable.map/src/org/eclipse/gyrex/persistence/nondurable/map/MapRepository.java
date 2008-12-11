/**
 * Copyright (c) 2008 AGETO Service GmbH and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.cloudfree.persistence.nondurable.map;

import java.util.concurrent.ConcurrentNavigableMap;

import org.eclipse.cloudfree.monitoring.metrics.MetricSet;
import org.eclipse.cloudfree.persistence.storage.Repository;
import org.eclipse.cloudfree.persistence.storage.type.RepositoryType;

/**
 * Base class for non durable repositories backed by an in-memory map.
 * <p>
 * The repository may be used for short living objects. It does not offer any
 * transaction or other rich persistence capabilities. It's backed by a
 * {@link ConcurrentNavigableMap} to simple store objects based on keys.
 * </p>
 * <p>
 * This class may be subclassed by clients that want to contribute a custom map
 * repository to the platform.
 * </p>
 */
public abstract class MapRepository extends Repository {

	/**
	 * Creates a new map repository instance.
	 * 
	 * @param repositoryId
	 *            the repository id
	 * @param repositoryType
	 *            the repository type
	 * @param metrics
	 *            the metrics
	 * @throws IllegalArgumentException
	 */
	protected MapRepository(final String repositoryId, final RepositoryType repositoryType, final MetricSet metrics) throws IllegalArgumentException {
		super(repositoryId, repositoryType, metrics);
	}

}
