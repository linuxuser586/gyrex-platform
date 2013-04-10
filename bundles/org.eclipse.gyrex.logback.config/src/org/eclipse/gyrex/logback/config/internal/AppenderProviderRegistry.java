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
package org.eclipse.gyrex.logback.config.internal;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.gyrex.logback.config.spi.AppenderProvider;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The appender provider registry
 */
public class AppenderProviderRegistry extends ServiceTracker<AppenderProvider, AppenderProvider> {

	private static final Logger LOG = LoggerFactory.getLogger(AppenderProviderRegistry.class);
	private final ConcurrentMap<String, AppenderProvider> providerById = new ConcurrentHashMap<String, AppenderProvider>();
	private final ConcurrentMap<String, String> nameById = new ConcurrentHashMap<String, String>();

	public AppenderProviderRegistry(final BundleContext context) {
		super(context, AppenderProvider.class, null);
	}

	void addAppenderProvider(final AppenderProvider provider, final String defaultName) {
		if (LogbackConfigDebug.providerRegistry) {
			LOG.debug("Adding appender provider: {}", provider);
		}
		final Collection<String> ids = provider.getProvidedTypeIds();
		for (final String id : ids) {
			final AppenderProvider existing = providerById.putIfAbsent(id, provider);
			if (existing != null) {
				LOG.warn("Appender provider with id {} already registered. Registration of appender provider {} ignored.", id, provider);
			} else {
				final String name = provider.getName(id);
				nameById.putIfAbsent(id, name != null ? name : null != defaultName ? defaultName : id);
			}
		}
	}

	@Override
	public AppenderProvider addingService(final ServiceReference<AppenderProvider> reference) {
		final AppenderProvider service = super.addingService(reference);
		final Object defaultName = reference.getProperty(Constants.SERVICE_DESCRIPTION);
		addAppenderProvider(service, defaultName instanceof String ? (String) defaultName : null);
		return service;
	}

	public String getName(final String appenderTypeId) {
		return nameById.get(appenderTypeId);
	}

	public AppenderProvider getProvider(final String id) {
		return providerById.get(id);
	}

	public Collection<String> getAvailableTypeIds() {
		return providerById.keySet();
	}

	void removeAppenderProvider(final AppenderProvider provider) {
		if (LogbackConfigDebug.providerRegistry) {
			LOG.debug("Removing appenders provider: {}", provider);
		}
		final Collection<String> ids = provider.getProvidedTypeIds();
		for (final String id : ids) {
			providerById.remove(id, provider);
			nameById.remove(id);
		}
	}

	@Override
	public void removedService(final ServiceReference<AppenderProvider> reference, final AppenderProvider service) {
		removeAppenderProvider(service);
		super.removedService(reference, service);
	}
}
