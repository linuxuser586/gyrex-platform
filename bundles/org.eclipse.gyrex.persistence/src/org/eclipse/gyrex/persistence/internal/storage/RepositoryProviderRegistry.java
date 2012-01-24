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
 *******************************************************************************/
package org.eclipse.gyrex.persistence.internal.storage;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.persistence.storage.provider.RepositoryProvider;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The repository type registry.
 * <p>
 * The registry is tight to the bundle life cycle. It is made available as an
 * OSGi service.
 * </p>
 */
public final class RepositoryProviderRegistry {

	public static class RepositoryProviderRegistration {

		private final RepositoryProvider provider;
		private final String providerInfo;

		/**
		 * Creates a new instance.
		 */
		public RepositoryProviderRegistration(final RepositoryProvider provider, final String providerInfo) {
			this.provider = provider;
			this.providerInfo = providerInfo;
		}

		/**
		 * Returns the provider.
		 * 
		 * @return the provider
		 */
		public RepositoryProvider getProvider() {
			return provider;
		}

		public String getProviderId() {
			return provider.getProviderId();
		}

		/**
		 * Returns the providerInfo.
		 * 
		 * @return the providerInfo
		 */
		public String getProviderInfo() {
			return providerInfo;
		}

		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder();
			builder.append("RepositoryProviderRegistration [provider=").append(provider).append(", providerInfo=").append(providerInfo).append("]");
			return builder.toString();
		}

	}

	/** the map with registered repository types by their id */
	private final ConcurrentMap<String, RepositoryProviderRegistration> registeredRepositoryTypesById = new ConcurrentHashMap<String, RepositoryProviderRegistration>(5);
	private ServiceTracker<RepositoryProvider, RepositoryProvider> serviceTracker;

	private static final Logger LOG = LoggerFactory.getLogger(RepositoryProviderRegistry.class);

	public void close() {
		serviceTracker.close();
		serviceTracker = null;
		registeredRepositoryTypesById.clear();
	}

	public List<RepositoryProviderRegistration> getAllProviderRegistrations() {
		final Collection<RepositoryProviderRegistration> values = registeredRepositoryTypesById.values();
		return Arrays.asList(values.toArray(new RepositoryProviderRegistration[values.size()]));

	}

	public RepositoryProvider getRepositoryProvider(final String repositoryProviderId) {
		if (null == repositoryProviderId) {
			throw new IllegalArgumentException("repository type id must not be null");
		}

		final RepositoryProviderRegistration registration = registeredRepositoryTypesById.get(repositoryProviderId);
		if (null == registration) {
			throw new IllegalStateException(MessageFormat.format("repository provider \"{0}\" not available", repositoryProviderId));
		}

		return registration.getProvider();
	}

	public String getRepositoryProviderInfo(final String repositoryProviderId) {
		if (null == repositoryProviderId) {
			throw new IllegalArgumentException("repository type id must not be null");
		}

		final RepositoryProviderRegistration registration = registeredRepositoryTypesById.get(repositoryProviderId);
		if (null == registration) {
			throw new IllegalStateException(MessageFormat.format("repository provider \"{0}\" not available", repositoryProviderId));
		}

		return registration.getProviderInfo();
	}

	public void registerRepositoryProvider(final String providerId, final RepositoryProvider repositoryProvider, final String providerInfo) throws IllegalArgumentException, IllegalStateException {
		if (!IdHelper.isValidId(providerId)) {
			throw new IllegalArgumentException(String.format("Invalid repository provider id '%s'.", providerId));
		}
		final RepositoryProviderRegistration existing = registeredRepositoryTypesById.putIfAbsent(providerId, new RepositoryProviderRegistration(repositoryProvider, providerInfo));
		if ((null != existing) && (existing.getProvider() != repositoryProvider)) {
			throw new IllegalStateException(String.format("A repository provider with id \"%s\" is already registered!", providerId));
		}
	}

	public void start(final BundleContext context) {
		serviceTracker = new ServiceTracker<RepositoryProvider, RepositoryProvider>(context, RepositoryProvider.class, null) {

			@Override
			public RepositoryProvider addingService(final ServiceReference<RepositoryProvider> reference) {
				final RepositoryProvider repositoryProvider = super.addingService(reference);
				if (null != repositoryProvider) {
					try {
						registerRepositoryProvider(repositoryProvider.getProviderId(), repositoryProvider, (String) reference.getProperty(Constants.SERVICE_DESCRIPTION));
					} catch (final Exception e) {
						LOG.error("Unable to register repository provider ({}). {}", new Object[] { reference, ExceptionUtils.getRootCauseMessage(e), e });
					}
				}
				return repositoryProvider;
			}

			@Override
			public void removedService(final ServiceReference<RepositoryProvider> reference, final RepositoryProvider service) {
				unregisterRepositoryProvider(service.getProviderId());
				super.removedService(reference, service);
			}
		};
		serviceTracker.open();
	}

	/**
	 * Unregisters the repository type with the specified id.
	 * 
	 * @param repositoryProviderId
	 *            the repository type id to unregister (may not be
	 *            <code>null</code>)
	 */
	public void unregisterRepositoryProvider(final String repositoryProviderId) {
		if (null == repositoryProviderId) {
			throw new IllegalArgumentException("repository type identifier must not be null");
		}

		registeredRepositoryTypesById.remove(repositoryProviderId);
	}

}
