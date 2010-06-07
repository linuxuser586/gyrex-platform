/*******************************************************************************
 * Copyright (c) 2008, 2009 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.nondurable.map.internal;

import java.util.concurrent.ConcurrentSkipListMap;

import org.eclipse.gyrex.persistence.nondurable.map.MapRepository;

/**
 * {@link MapRepository} implementation using a {@link ConcurrentSkipListMap}.
 */
public class MapRepositoryImpl extends MapRepository {

	/**
	 * Creates a new instance.
	 * 
	 * @param repositoryId
	 * @param repositoryType
	 * @param metrics
	 * @throws IllegalArgumentException
	 */
	public MapRepositoryImpl(final String repositoryId, final MapRepositoryType repositoryType) throws IllegalArgumentException {
		super(repositoryId, repositoryType, new MapRepositoryMetrics(createMetricsId(repositoryType, repositoryId)));
	}

}
