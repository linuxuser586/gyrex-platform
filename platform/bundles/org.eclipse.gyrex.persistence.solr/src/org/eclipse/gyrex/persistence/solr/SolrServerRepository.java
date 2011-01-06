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
 * A Solr repository based on SolrJ.
 * <p>
 * Typically, a Solr repository provides common functionality for working with
 * Apache Solr. This repository supports working with a single (aka. "default")
 * or multiple collections (aka. "indices").
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
	 * used for communicating with the <strong>default</strong> Solr index.
	 * <p>
	 * This is a convenience method which just calls
	 * {@link #getSolrServer(String)} with a <code>null</code> argument.
	 * </p>
	 * 
	 * @return the {@link SolrServer} object for the default index
	 * @throws IllegalStateException
	 *             if the repository has been closed or no default index is
	 *             available
	 */
	public final SolrServer getSolrServer() throws IllegalStateException {
		return getSolrServer(null);
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
	 * @param collection
	 *            the collection (maybe <code>null</code> for the default
	 *            collection)
	 * @return the {@link SolrServer} object for the specified index
	 * @throws IllegalStateException
	 *             if the repository has been closed
	 * @throws IllegalArgumentException
	 *             if the specified index is unknown
	 */
	public abstract SolrServer getSolrServer(String collection) throws IllegalStateException, IllegalArgumentException;

	/**
	 * Returns a {@link SolrServer SolrJ server object} that may be used for
	 * querying the <strong>default</strong> Solr index.
	 * <p>
	 * This is a convenience method which just calls
	 * {@link #getSolrServerOptimizedForQuery(String)} with a <code>null</code>
	 * argument.
	 * </p>
	 * 
	 * @return the {@link SolrServer} object for the specified index
	 * @throws IllegalStateException
	 *             if the repository has been closed or no default index is
	 *             available
	 */
	public final SolrServer getSolrServerOptimizedForQuery() throws IllegalStateException {
		return getSolrServerOptimizedForQuery(null);
	}

	/**
	 * Returns a {@link SolrServer SolrJ server object} that may be used for
	 * querying the specified Solr index.
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
	 * @param collection
	 *            the collection (maybe <code>null</code> for the default
	 *            collection)
	 * @return the {@link SolrServer} object for the specified index
	 * @throws IllegalStateException
	 *             if the repository has been closed
	 * @throws IllegalArgumentException
	 *             if the specified index is unknown
	 */
	public abstract SolrServer getSolrServerOptimizedForQuery(String collection) throws IllegalStateException, IllegalArgumentException;
}
