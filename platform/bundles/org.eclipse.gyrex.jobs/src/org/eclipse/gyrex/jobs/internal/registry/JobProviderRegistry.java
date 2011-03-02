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
package org.eclipse.gyrex.jobs.internal.registry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.gyrex.jobs.internal.JobsDebug;
import org.eclipse.gyrex.jobs.provider.JobProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The job provider registry
 */
public class JobProviderRegistry {

	private static final Logger LOG = LoggerFactory.getLogger(JobProviderRegistry.class);
	private Object providerExtensionReader;
	private final ConcurrentMap<String, JobProvider> providerById = new ConcurrentHashMap<String, JobProvider>();

	public void activate() {
		if (JobsDebug.providerRegistry) {
			LOG.debug("Activating jobs provider registry. {}", this);
		}
	}

	public void addJobProvider(final JobProvider provider) {
		if (JobsDebug.providerRegistry) {
			LOG.debug("Adding job provider: {}", provider);
		}
		final String[] ids = provider.getProviderIds();
		for (final String id : ids) {
			final JobProvider existing = providerById.putIfAbsent(id, provider);
			if (existing != null) {
				LOG.warn("Job provider with id {} already registered. Registration of job provider {} ignored.", id, provider);
			}
		}
	}

	public void deactivate() {
		if (JobsDebug.providerRegistry) {
			LOG.debug("Deactivating jobs provider registry. {}", this);
		}
	}

	public void removeJobProvider(final JobProvider provider) {
		if (JobsDebug.providerRegistry) {
			LOG.debug("Removing jobs provider: {}", provider);
		}
		final String[] ids = provider.getProviderIds();
		for (final String id : ids) {
			providerById.remove(id, provider);
		}
	}

	public void setExtensionRegistry(final Object extensionRegistry) {
		if (JobsDebug.providerRegistry) {
			LOG.debug("Initializing extension registry. {}", this);
		}
		providerExtensionReader = new JobProviderExtensionReader(this, extensionRegistry);
	}

	public void unsetExtensionRegistry(final Object extensionRegistry) {
		if (JobsDebug.providerRegistry) {
			LOG.debug("Deactivating extension registry. {}", this);
		}
		if (null != providerExtensionReader) {
			((JobProviderExtensionReader) providerExtensionReader).close();
			providerExtensionReader = null;
		}
	}
}
