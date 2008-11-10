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
package org.eclipse.cloudfree.persistence.internal.storage.type;

import java.text.MessageFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


import org.eclipse.cloudfree.persistence.internal.PersistenceActivator;
import org.eclipse.cloudfree.persistence.storage.type.RegistrationException;
import org.eclipse.cloudfree.persistence.storage.type.RepositoryType;
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
public final class RepositoryTypeRegistry {

	/** the map with registered repository types by their id */
	private final ConcurrentMap<String, RepositoryType> registeredRepositoryTypesById = new ConcurrentHashMap<String, RepositoryType>(5);
	private ServiceTracker serviceTracker;

	public void close() {
		serviceTracker.close();
		serviceTracker = null;
		registeredRepositoryTypesById.clear();
	}

	public RepositoryType getRepositoryType(final String repositoryTypeId) {
		if (null == repositoryTypeId) {
			throw new IllegalArgumentException("repository type id must not be null");
		}

		final RepositoryType repositoryType = registeredRepositoryTypesById.get(repositoryTypeId);
		if (null == repositoryType) {
			throw new IllegalStateException(MessageFormat.format("repository type \"{0}\" not available", repositoryTypeId));
		}

		return repositoryType;
	}

	/**
	 * Registers a repository type under the specified repository type id.
	 * 
	 * @param repositoryTypeId
	 *            the repository type id (may not be <code>null</code>)
	 * @param type
	 *            the repository type (may not be <code>null</code>)
	 * @throws RegistrationException
	 *             if the repository type could not be registered (usually
	 *             because a repository type is already registered for the
	 *             repository type id)
	 */
	public void registerRepositoryType(final String repositoryTypeId, final RepositoryType type) throws RegistrationException {
		if (null == repositoryTypeId) {
			throw new IllegalArgumentException("repository type identifier must not be null");
		}
		if (null == type) {
			throw new IllegalArgumentException("repository type must not be null");
		}

		final RepositoryType existing = registeredRepositoryTypesById.putIfAbsent(repositoryTypeId, type);
		if ((null != existing) && (existing != type)) {
			throw new RegistrationException(new Status(IStatus.ERROR, PersistenceActivator.PLUGIN_ID, RegistrationException.CONFLICTING_ID, MessageFormat.format("A repository type with id \"{0}\" is already registered!", repositoryTypeId), null));
		}
	}

	public void start(final BundleContext context) {
		serviceTracker = new ServiceTracker(context, RepositoryType.class.getName(), null) {
			/* (non-Javadoc)
			 * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
			 */
			@Override
			public Object addingService(final ServiceReference reference) {
				final RepositoryType repositoryType = (RepositoryType) super.addingService(reference);
				if (null != repositoryType) {
					try {
						registerRepositoryType(repositoryType.getId(), repositoryType);
					} catch (final RegistrationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				return repositoryType;
			}

			/* (non-Javadoc)
			 * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
			 */
			@Override
			public void removedService(final ServiceReference reference, final Object service) {
				final RepositoryType repositoryType = (RepositoryType) service;
				if (null != repositoryType) {
					unregisterRepositoryType(repositoryType.getId());
				}
				super.removedService(reference, service);
			}
		};
		serviceTracker.open();
	}

	/**
	 * Unregisters the repository type with the specified id.
	 * 
	 * @param repositoryTypeId
	 *            the repository type id to unregister (may not be
	 *            <code>null</code>)
	 */
	public void unregisterRepositoryType(final String repositoryTypeId) {
		if (null == repositoryTypeId) {
			throw new IllegalArgumentException("repository type identifier must not be null");
		}

		registeredRepositoryTypesById.remove(repositoryTypeId);
	}
}
