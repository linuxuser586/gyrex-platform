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

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.jobs.provider.JobProvider;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.dynamichelpers.ExtensionTracker;
import org.eclipse.core.runtime.dynamichelpers.IExtensionChangeHandler;
import org.eclipse.core.runtime.dynamichelpers.IExtensionTracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A reader for Job provider extensions
 */
public class JobProviderExtensionReader implements IExtensionChangeHandler {

	private static final Logger LOG = LoggerFactory.getLogger(JobProviderExtensionReader.class);

	private final ExtensionTracker extensionTracker;
	private final JobProviderRegistry providerRegistry;

	/**
	 * Creates a new instance.
	 * 
	 * @param providerRegistry
	 * @param extensionRegistry
	 */
	public JobProviderExtensionReader(final JobProviderRegistry providerRegistry, final IExtensionRegistry extensionRegistry) {
		this.providerRegistry = providerRegistry;
		final IExtensionPoint extensionPoint = extensionRegistry.getExtensionPoint("org.eclipse.gyrex.jobs.providers");
		if (extensionPoint == null) {
			throw new IllegalStateException("providers extension point not found; please check bundle deployment");
		}
		extensionTracker = new ExtensionTracker(extensionRegistry);
		extensionTracker.registerHandler(this, ExtensionTracker.createExtensionPointFilter(extensionPoint));
		for (final IExtension extension : extensionPoint.getExtensions()) {
			addExtension(extensionTracker, extension);
		}
	}

	@Override
	public void addExtension(final IExtensionTracker tracker, final IExtension extension) {
		for (final IConfigurationElement element : extension.getConfigurationElements()) {
			if ("job".equals(element.getName())) {
				final String id = element.getAttribute("id");
				if (!IdHelper.isValidId(id)) {
					LOG.warn("Invalid id {} found in job provider extension contributed by {}", new Object[] { id, extension.getContributor().getName() });
					continue;
				}
				final RegistryJobProvider provider = new RegistryJobProvider(id, element);
				tracker.registerObject(extension, provider, IExtensionTracker.REF_STRONG);
				providerRegistry.addJobProvider(provider);
			}
		}
	}

	public void close() {
		extensionTracker.close();
	}

	@Override
	public void removeExtension(final IExtension extension, final Object[] objects) {
		// remove all registered providers
		for (final Object object : objects) {
			if (object instanceof RegistryJobProvider) {
				providerRegistry.removeJobProvider((JobProvider) object);
			}
		}
	}

}
