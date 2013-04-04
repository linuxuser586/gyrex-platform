/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *     Mike Tschierschke - improvements due working on https://bugs.eclipse.org/bugs/show_bug.cgi?id=344467
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.registry;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.JobsDebug;
import org.eclipse.gyrex.jobs.provider.JobProvider;

import org.eclipse.core.runtime.IExtensionRegistry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The job provider registry
 */
public class JobProviderRegistry extends ServiceTracker<JobProvider, JobProvider> {

	private static final Logger LOG = LoggerFactory.getLogger(JobProviderRegistry.class);
	private final ConcurrentMap<String, JobProvider> providerById = new ConcurrentHashMap<String, JobProvider>();
	private final ConcurrentMap<String, String> nameById = new ConcurrentHashMap<String, String>();
	private JobProviderExtensionReader extensionReader;

	public JobProviderRegistry(final BundleContext context) {
		super(context, JobProvider.class, null);
	}

	@Override
	public JobProvider addingService(final ServiceReference<JobProvider> reference) {
		final JobProvider service = super.addingService(reference);
		final Object defaultName = reference.getProperty(Constants.SERVICE_DESCRIPTION);
		addJobProvider(service, defaultName instanceof String ? (String) defaultName : null);
		return service;
	}

	void addJobProvider(final JobProvider provider, final String defaultName) {
		if (JobsDebug.providerRegistry) {
			LOG.debug("Adding job provider: {}", provider);
		}
		final Collection<String> ids = provider.getProvidedTypeIds();
		for (final String id : ids) {
			final JobProvider existing = providerById.putIfAbsent(id, provider);
			if (existing != null) {
				LOG.warn("Job provider with id {} already registered. Registration of job provider {} ignored.", id, provider);
			} else {
				final String name = provider.getName(id);
				nameById.putIfAbsent(id, name != null ? name : null != defaultName ? defaultName : id);
			}
		}
	}

	@Override
	public void close() {
		extensionReader.close();
		extensionReader = null;

		// super
		super.close();
	}

	public String getName(final String jobTypeId) {
		return nameById.get(jobTypeId);
	}

	public JobProvider getProvider(final String id) {
		return providerById.get(id);
	}

	public Collection<String> getProviders() {
		return providerById.keySet();
	}

	@Override
	public void open() {
		// super
		super.open();

		// hook with extension registry
		final IExtensionRegistry extensionRegistry = JobsActivator.getInstance().getService(IExtensionRegistry.class);
		extensionReader = new JobProviderExtensionReader(this, extensionRegistry);
	}

	@Override
	public void removedService(final ServiceReference<JobProvider> reference, final JobProvider service) {
		removeJobProvider(service);
		super.removedService(reference, service);
	}

	void removeJobProvider(final JobProvider provider) {
		if (JobsDebug.providerRegistry) {
			LOG.debug("Removing jobs provider: {}", provider);
		}
		final Collection<String> ids = provider.getProvidedTypeIds();
		for (final String id : ids) {
			providerById.remove(id, provider);
			nameById.remove(id);
		}
	}
}
