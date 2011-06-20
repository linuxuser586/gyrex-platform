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
package org.eclipse.gyrex.persistence.solr.internal;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import org.eclipse.gyrex.boot.internal.app.ServerApplication;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class EmbeddedSolrServerApplication implements IApplication {

	private static final Logger LOG = LoggerFactory.getLogger(EmbeddedSolrServerApplication.class);

	/** Exit object indicating error termination */
	private static final Integer EXIT_ERROR = Integer.valueOf(1);

	static final AtomicReference<CoreContainer> coreContainerRef = new AtomicReference<CoreContainer>();
	static volatile File solrBase;

	private IApplicationContext runningContext;

	@Override
	public Object start(final IApplicationContext context) throws Exception {
		if (SolrDebug.embeddedSolrServer) {
			LOG.debug("Starting embedded Solr server.");
		}

		try {
			// start server
			startEmbeddedSolrServer();

			// signal running
			context.applicationRunning();

			// finish async
			runningContext = context;
			return IApplicationContext.EXIT_ASYNC_RESULT;
		} catch (final Exception e) {
			// shutdown the whole server
			if (Platform.inDevelopmentMode()) {
				ServerApplication.shutdown(new Exception("Could not start the embedded Solr server. " + ExceptionUtils.getRootCauseMessage(e), e));
			} else {
				LOG.error("Unable to start embedded Solr. {}", ExceptionUtils.getRootCauseMessage(e), e);
			}

			return EXIT_ERROR;
		}
	}

	private void startEmbeddedSolrServer() throws Exception {
		// the configuration template
		final File configTemplate = new File(FileLocator.toFileURL(SolrActivator.getInstance().getBundle().getEntry("conf-embeddedsolr")).getFile());

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

	@Override
	public void stop() {
		if (SolrDebug.embeddedSolrServer) {
			LOG.debug("Stopping embedded Solr server.");
		}

		final IApplicationContext context = runningContext;
		if (context == null) {
			throw new IllegalStateException("not started");
		}

		final CoreContainer coreContainer = coreContainerRef.getAndSet(null);
		if (null == coreContainer) {
			return;
		}

		coreContainer.persist();
		coreContainer.shutdown();
	}

}
