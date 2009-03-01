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
package org.eclipse.cloudfree.persistence.internal.storage;

import java.text.MessageFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.cloudfree.persistence.internal.PersistenceActivator;
import org.eclipse.cloudfree.persistence.storage.provider.RepositoryProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The repository type registry.
 * <p>
 * The registry is tight to the bundle life cycle. It is made available as an
 * OSGi service.
 * </p>
 */
public final class RepositoryProviderRegistry {

	/** the map with registered repository types by their id */
	private final ConcurrentMap<String, RepositoryProvider> registeredRepositoryTypesById = new ConcurrentHashMap<String, RepositoryProvider>(5);
	private ServiceTracker serviceTracker;

	public void close() {
		serviceTracker.close();
		serviceTracker = null;
		registeredRepositoryTypesById.clear();
	}

	public RepositoryProvider getRepositoryProvider(final String repositoryProviderId) {
		if (null == repositoryProviderId) {
			throw new IllegalArgumentException("repository type id must not be null");
		}

		final RepositoryProvider repositoryType = registeredRepositoryTypesById.get(repositoryProviderId);
		if (null == repositoryType) {
			throw new IllegalStateException(MessageFormat.format("repository type \"{0}\" not available", repositoryProviderId));
		}

		return repositoryType;
	}

	/**
	 * Registers a repository type under the specified repository type id.
	 * 
	 * @param repositoryProviderId
	 *            the repository type id (may not be <code>null</code>)
	 * @param type
	 *            the repository type (may not be <code>null</code>)
	 * @throws RegistrationException
	 *             if the repository type could not be registered (usually
	 *             because a repository type is already registered for the
	 *             repository type id)
	 */
	// TODO: add support for multiple versions of the same provider
	public void registerRepositoryProvider(final String repositoryProviderId, final RepositoryProvider type) throws CoreException {
		if (null == repositoryProviderId) {
			throw new IllegalArgumentException("repository type identifier must not be null");
		}
		if (null == type) {
			throw new IllegalArgumentException("repository type must not be null");
		}

		final RepositoryProvider existing = registeredRepositoryTypesById.putIfAbsent(repositoryProviderId, type);
		if ((null != existing) && (existing != type)) {
			throw new CoreException(new Status(IStatus.ERROR, PersistenceActivator.PLUGIN_ID, IStatus.ERROR, MessageFormat.format("A repository type with id \"{0}\" is already registered!", repositoryProviderId), null));
		}
	}

	public void start(final BundleContext context) {
		serviceTracker = new ServiceTracker(context, RepositoryProvider.class.getName(), null) {

			@Override
			public Object addingService(final ServiceReference reference) {
				final RepositoryProvider repositoryType = (RepositoryProvider) super.addingService(reference);
				if (null != repositoryType) {
					try {
						registerRepositoryProvider(repositoryType.getProviderId(), repositoryType);
					} catch (final CoreException e) {
						// TODO log error
						e.printStackTrace();
					}
				}
				return repositoryType;
			}

			@Override
			public void removedService(final ServiceReference reference, final Object service) {
				final RepositoryProvider repositoryType = (RepositoryProvider) service;
				if (null != repositoryType) {
					unregisterRepositoryProvider(repositoryType.getProviderId());
				}
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
