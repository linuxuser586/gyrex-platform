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
import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;

import org.osgi.framework.BundleContext;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
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

	private final AtomicReference<CoreContainer> coreContainerRef = new AtomicReference<CoreContainer>();

	private volatile File solrBase;

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

		// start server
		startEmbeddedSolrServer(context);
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		shutdownEmbeddedSolrServer();
		instance.set(null);
	}

	public CoreContainer getEmbeddedCoreContainer() {
		return coreContainerRef.get();
	}

	public File getEmbeddedSolrBase() {
		return solrBase;
	}

	public File getEmbeddedSolrCoreBase(final String coreName) {
		if (null == solrBase) {
			throw new IllegalStateException("no Solr base directory");
		}
		return new File(solrBase, coreName);
	}

	private void shutdownEmbeddedSolrServer() {
		final CoreContainer coreContainer = coreContainerRef.getAndSet(null);
		if (null != coreContainer) {
			coreContainer.persist();
			coreContainer.shutdown();
		}
	}

	private void startEmbeddedSolrServer(final BundleContext context) throws Exception {
		// only in dev mode
		if (!Platform.inDevelopmentMode()) {
			return;
		}

		// the configuration template
		final File configTemplate = new File(FileLocator.toFileURL(context.getBundle().getEntry("conf-embeddedsolr")).getFile());

		// get embedded Solr home directory
		final IPath instanceLocation = Platform.getInstanceLocation();

		solrBase = instanceLocation.append("solr").toFile();
		if (!solrBase.isDirectory()) {
			// initialize dir
			solrBase.mkdirs();
		}

		// get multicore config file
		final File configFile = new File(solrBase, "solr.xml");
		if (!configFile.isFile()) {
			// deploy base configuration
			FileUtils.copyDirectory(configTemplate, solrBase);
			if (!configFile.isFile()) {
				throw new IllegalStateException("config file '" + configFile.getPath() + "' is missing");
			}
		}

		// create core container
		final CoreContainer coreContainer = new CoreContainer();
		if (!coreContainerRef.compareAndSet(null, coreContainer)) {
			// already initialized
			return;
		}

		// load configuration
		coreContainer.load(solrBase.getAbsolutePath(), configFile);

		// ensure that there is an admin core
		if (!coreContainer.getCoreNames().contains("admin")) {
			coreContainer.create(new CoreDescriptor(coreContainer, "admin", "admin"));
		}
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
