/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.solr;

import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;

import org.apache.solr.client.solrj.SolrServer;

/**
 * Common base class for a Solr repository based on SolrJ.
 * <p>
 * Typically, a Solr repository provides common functionality for working with
 * Apache Solr.
 * </p>
 * <p>
 * Note, this API depends on the SolrJ and Solr API. Thus, it is bound to the
 * evolution of external API which might not follow the Gyrex <a
 * href="http://wiki.eclipse.org/Evolving_Java-based_APIs" target="_blank">API
 * evolution</a> and <a href="http://wiki.eclipse.org/Version_Numbering"
 * target="_blank">versioning</a> guidelines.
 * </p>
 * <p>
 * This class may be subclassed by clients that contribute a Solr repository
 * type to Gyrex.
 * </p>
 */
public abstract class SolrServerRepository extends Repository {

	/** the repository type name */
	public static final String TYPE_NAME = SolrServerRepository.class.getName();

	/**
	 * Creates a new instance.
	 * 
	 * @param repositoryId
	 * @param repositoryProvider
	 * @param metrics
	 * @throws IllegalArgumentException
	 */
	protected SolrServerRepository(final String repositoryId, final RepositoryProvider repositoryProvider, final MetricSet metrics) throws IllegalArgumentException {
		super(repositoryId, repositoryProvider, metrics);
	}

	/**
	 * Returns the underlying {@link SolrServer SolrJ server object} that may be
	 * used for communicating with the Solr index.
	 * <p>
	 * Note, this API depends on the SolrJ and Solr API. Thus, it is bound to
	 * the evolution of external API which might not follow the Gyrex <a
	 * href="http://wiki.eclipse.org/Evolving_Java-based_APIs"
	 * target="_blank">API evolution</a> and <a
	 * href="http://wiki.eclipse.org/Version_Numbering"
	 * target="_blank">versioning</a> guidelines.
	 * </p>
	 * 
	 * @return the {@link SolrServer} object
	 * @throws IllegalStateException
	 *             if the repository has been closed
	 */
	public abstract SolrServer getSolrServer() throws IllegalStateException;

	/**
	 * Returns a {@link SolrServer SolrJ server object} that may be used for
	 * querying only.
	 * <p>
	 * In contrast to {@link #getSolrServer()} this server object returned here
	 * may be optimized for query requests. It <strong>must not</strong> be used
	 * for any kind of update/indexing requests.
	 * </p>
	 * <p>
	 * Note, this API depends on the SolrJ and Solr API. Thus, it is bound to
	 * the evolution of external API which might not follow the Gyrex <a
	 * href="http://wiki.eclipse.org/Evolving_Java-based_APIs"
	 * target="_blank">API evolution</a> and <a
	 * href="http://wiki.eclipse.org/Version_Numbering"
	 * target="_blank">versioning</a> guidelines.
	 * </p>
	 * 
	 * @return the {@link SolrServer} object
	 * @throws IllegalStateException
	 *             if the repository has been closed
	 */
	public abstract SolrServer getSolrServerOptimizedForQuery() throws IllegalStateException;
}
