/*******************************************************************************
 * Copyright (c) 2008, 2011 Gunnar Wagenknecht and others.
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
package org.eclipse.gyrex.persistence.solr.internal;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;

import org.osgi.framework.BundleContext;

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;

public class SolrActivator extends BaseBundleActivator {

	public static final String PLUGIN_ID = "org.eclipse.gyrex.persistence.solr";
	private static final AtomicReference<SolrActivator> instance = new AtomicReference<SolrActivator>();

	public static String getEmbeddedSolrCoreName(final String repositoryId) {
		return repositoryId;
	}

	/**
	 * Returns the instance.
	 * 
	 * @return the instance
	 */
	public static SolrActivator getInstance() {
		return instance.get();
	}

	/**
	 * Creates a new instance.
	 */
	public SolrActivator() {
		super(PLUGIN_ID);
	}

	public void createEmbeddedCore(final String coreName) throws Exception {
		final CoreContainer coreContainer = getEmbeddedCoreContainer();
		if (null == coreContainer) {
			throw new IllegalStateException("no coreContainer");
		}
		final SolrCore core = coreContainer.getCore(coreName);
		try {
			if (null != core) {
				throw new IllegalStateException(String.format("core '%s' already exists", coreName));
			}

			final EmbeddedSolrServer adminServer = new EmbeddedSolrServer(coreContainer, "admin");
			CoreAdminRequest.createCore(coreName, coreName, adminServer);
		} finally {
			if (null != core) {
				core.close();
			}
		}
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instance.set(this);
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instance.set(null);
	}

	@Override
	protected Class getDebugOptions() {
		return SolrDebug.class;
	}

	public CoreContainer getEmbeddedCoreContainer() {
		return EmbeddedSolrServerApplication.coreContainerRef.get();
	}

	public File getEmbeddedSolrBase() {
		return EmbeddedSolrServerApplication.solrBase;
	}

	public File getEmbeddedSolrCoreBase(final String coreName) {
		final File solrBase = EmbeddedSolrServerApplication.solrBase;
		if (null == solrBase) {
			throw new IllegalStateException("no Solr base directory");
		}
		return new File(solrBase, coreName);
	}

	public void unloadEmbeddedCore(final String coreName) throws Exception {
		final CoreContainer coreContainer = getEmbeddedCoreContainer();
		if (null == coreContainer) {
			throw new IllegalStateException("no coreContainer");
		}
		final SolrCore core = coreContainer.getCore(coreName);
		try {
			if (null != core) {
				final EmbeddedSolrServer adminServer = new EmbeddedSolrServer(coreContainer, "admin");
				CoreAdminRequest.unloadCore(coreName, adminServer);
			}
		} finally {
			if (null != core) {
				core.close();
			}
		}
	}
}
