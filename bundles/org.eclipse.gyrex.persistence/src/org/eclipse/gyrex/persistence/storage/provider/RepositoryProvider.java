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
package org.eclipse.gyrex.persistence.storage.provider;

import java.text.MessageFormat;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.persistence.storage.Repository;
import org.eclipse.gyrex.persistence.storage.settings.IRepositoryPreferences;

import org.eclipse.core.runtime.PlatformObject;

/**
 * A repository provider base class which provides {@link Repository repository}
 * instances to Gyrex.
 * <p>
 * Repository providers can be dynamically registered to Gyrex by registering
 * {@link RepositoryProvider} instances as OSGi services. Repository providers
 * are considered core elements of Gyrex. Security restrictions may be used to
 * only allow a set of well known (i.e. trusted) providers.
 * </p>
 * <p>
 * Repository providers do not represent a concrete repository. They will be
 * used, however, to create concrete repository instances.
 * </p>
 * <p>
 * This class must be subclassed by clients that contribute a repository
 * implementation to Gyrex. As such it is considered part of a service provider
 * API which may evolve faster than the general API. Please get in touch with
 * the development team through the prefered channels listed on <a
 * href="http://www.eclipse.org/gyrex">the Gyrex website</a> to stay up-to-date
 * of possible changes.
 * </p>
 */
public abstract class RepositoryProvider extends PlatformObject {

	/** the OSGi service name */
	public static final String SERVICE_NAME = RepositoryProvider.class.getName();

	/** repository provider providerId */
	private final String providerId;

	/** repository type */
	private final Class<? extends Repository> repositoryType;

	/**
	 * Creates a new instance using the specified provider id.
	 * 
	 * @param id
	 *            the repository provider id (may not be <code>null</code>, will
	 *            be {@link IdHelper#isValidId(String) validated})
	 * @param repositoryType
	 *            the public repository type contract provided by this provider
	 *            (may not be <code>null</code>, the type name will be
	 *            {@link IdHelper#isValidId(String) validated})
	 * @see IdHelper#isValidId(String)
	 */
	protected RepositoryProvider(final String id, final Class<? extends Repository> repositoryType) {
		if (null == id) {
			throw new IllegalArgumentException("repository provider id must not be null");
		}
		if (!IdHelper.isValidId(id)) {
			throw new IllegalArgumentException(MessageFormat.format("repository provider id \"{0}\" is invalid; valid chars are US-ASCII a-z / A-Z / 0-9 / '.' / '-' / '_'", id));
		}
		providerId = id;

		if (null == repositoryType) {
			throw new IllegalArgumentException("repository type must not be null");
		}
		if (!IdHelper.isValidId(repositoryType.getName())) {
			throw new IllegalArgumentException(MessageFormat.format("repository type name \"{0}\" is invalid; valid chars are US-ASCII a-z / A-Z / 0-9 / '.' / '-' / '_'", repositoryType.getName()));
		}
		this.repositoryType = repositoryType;
	}

	/**
	 * Called by the persistence API to create a new repository instance.
	 * 
	 * @param repositoryId
	 *            the repository providerId
	 * @param repositoryPreferences
	 *            the repository preferences
	 * @return a new {@link Repository} instance
	 */
	public abstract Repository createRepositoryInstance(String repositoryId, IRepositoryPreferences repositoryPreferences);

	/**
	 * Returns the repository provider id.
	 * 
	 * @return the repository provider id
	 */
	public final String getProviderId() {
		return providerId;
	}

	/**
	 * Returns the public repository type contract provided by this provider.
	 * 
	 * @return the public repository type contract
	 */
	public final Class<? extends Repository> getRepositoryType() {
		return repositoryType;
	}

	/**
	 * Convenience method which returns the name of the public repository type
	 * contract provided by this provider.
	 * <p>
	 * The type name was {@link IdHelper#isValidId(String) validated} during
	 * provider initialization.
	 * </p>
	 * 
	 * @return the public repository type contract name
	 * @see IdHelper#isValidId(String)
	 * @see RepositoryProvider#getRepositoryType()
	 */
	public final String getRepositoryTypeName() {
		return getRepositoryType().getName();
	}

	/**
	 * Returns a string containing a concise, human-readable description of the
	 * provider.
	 * 
	 * @return a string representation of the provider
	 */
	@Override
	public final String toString() {
		return getClass().getSimpleName() + " {" + getProviderId() + "}";
	}
}
