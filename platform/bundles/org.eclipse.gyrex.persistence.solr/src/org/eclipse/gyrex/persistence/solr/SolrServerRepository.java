/*******************************************************************************
 * Copyright (c) 2010, 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *     Mike Tschierschke - rework of the SolrRepository concept (https://bugs.eclipse.org/bugs/show_bug.cgi?id=337404)
 *******************************************************************************/
package org.eclipse.gyrex.persistence.solr;

import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;

import org.apache.solr.client.solrj.SolrServer;

/**
 * A Solr repository based on SolrJ.
 * <p>
 * Typically, a Solr repository provides common functionality for working with
 * Apache Solr. The central point of entrance in SolrJ is a {@link SolrServer}.
 * The repository provides access to {@link #getSolrServer() a main server} as
 * well as {@link #getSolrServerOptimizedForQuery() a server optimized for
 * queries}.
 * </p>
 * <p>
 * Note, this API depends on the SolrJ and Solr API. Thus, it is bound to the
 * evolution of external API which might not follow the Gyrex <a
 * href="http://wiki.eclipse.org/Evolving_Java-based_APIs" target="_blank">API
 * evolution</a> and <a href="http://wiki.eclipse.org/Version_Numbering"
 * target="_blank">versioning</a> guidelines.
 * </p>
 * <p>
 * This class must not be subclassed or instantiated by clients. The platform
 * provides an implementation which will be injected.
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
	 * used for communicating with the specified Solr index.
	 * <p>
	 * Note, this API depends on the SolrJ and Solr API. Thus, it is bound to
	 * the evolution of external API which might not follow the Gyrex <a
	 * href="http://wiki.eclipse.org/Evolving_Java-based_APIs"
	 * target="_blank">API evolution</a> and <a
	 * href="http://wiki.eclipse.org/Version_Numbering"
	 * target="_blank">versioning</a> guidelines.
	 * </p>
	 * 
	 * @return the {@link SolrServer} object for the specified index
	 * @throws IllegalStateException
	 *             if the repository has been closed
	 */
	public abstract SolrServer getSolrServer() throws IllegalStateException, IllegalArgumentException;

	/**
	 * Returns a {@link SolrServer SolrJ server object} that may be used for
	 * querying the specified Solr index.
	 * <p>
	 * In contrast to {@link #getSolrServer(String)} this server object returned
	 * here may be optimized for query requests. It <strong>must not</strong> be
	 * used for any kind of update/indexing requests.
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
	 * @return the {@link SolrServer} object for the specified index
	 * @throws IllegalStateException
	 *             if the repository has been closed
	 */
	public abstract SolrServer getSolrServerOptimizedForQuery() throws IllegalStateException, IllegalArgumentException;
}
