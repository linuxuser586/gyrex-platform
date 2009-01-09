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
package org.eclipse.cloudfree.persistence.solr.internal;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.eclipse.cloudfree.persistence.storage.Repository;
import org.eclipse.cloudfree.persistence.storage.provider.RepositoryProvider;
import org.eclipse.cloudfree.persistence.storage.settings.IRepositoryPreferences;

/**
 * Useful during development on the local machine.
 */
public class EmbeddedSolrRepositoryType extends RepositoryProvider {

	public static final String TYPE_ID = "org.eclipse.cloudfree.persistence.solr.embedded";

	private final CoreContainer coreContainer;
	private final SolrServer adminServer;

	/**
	 * Creates a new instance.
	 * 
	 * @param coreContainer
	 * @param adminCore
	 * @param repositoryTypeId
	 */
	public EmbeddedSolrRepositoryType(final CoreContainer coreContainer) {
		super(TYPE_ID, SolrRepository.class);
		this.coreContainer = coreContainer;
		final SolrCore adminCore = coreContainer.getAdminCore();
		try {
			adminServer = new EmbeddedSolrServer(coreContainer, adminCore.getName());
		} finally {
			adminCore.close();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.persistence.storage.provider.RepositoryProvider#newRepositoryInstance(java.lang.String, org.eclipse.cloudfree.persistence.storage.settings.IRepositoryPreferences)
	 */
	@Override
	public Repository createRepositoryInstance(final String repositoryId, final IRepositoryPreferences repositoryPreferences) {
		try {
			return new SolrRepository(repositoryId, this, getServer(repositoryId, repositoryPreferences));
		} catch (final Exception e) {
			throw new IllegalStateException("could not create solr core: '" + repositoryId + "': " + e.getMessage(), e);
		}
	}

	public SolrServer getAdminServer() {
		return adminServer;
	}

	private SolrServer getServer(final String repositoryId, final IRepositoryPreferences repositoryPreferences) throws Exception {
		// we simple use the repository id as the core name
		final SolrCore core = coreContainer.getCore(repositoryId);
		if (null == core) {
			throw new IllegalStateException("no solr core '" + repositoryId + "' not found");
		}
		core.close();
		return new EmbeddedSolrServer(coreContainer, repositoryId);
	}

}
