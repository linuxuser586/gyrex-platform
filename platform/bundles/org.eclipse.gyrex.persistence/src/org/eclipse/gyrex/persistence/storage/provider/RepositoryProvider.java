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
package org.eclipse.cloudfree.persistence.storage.provider;

import java.text.MessageFormat;

import org.eclipse.cloudfree.persistence.storage.Repository;
import org.eclipse.cloudfree.persistence.storage.settings.IRepositoryPreferences;
import org.eclipse.core.runtime.PlatformObject;

/**
 * A repository provider base class which provides {@link Repository repository}
 * instances to the CloudFree Platform.
 * <p>
 * Repository providers can be dynamically registered to the CloudFree Platform
 * by registering {@link RepositoryProvider} instances as OSGi services.
 * Repository providers are considered core elements of the CloudFree Platform.
 * Security restrictions may be used to only allow a set of well known (i.e.
 * trusted) providers.
 * </p>
 * <p>
 * Repository providers do not represent a concrete repository. They will be
 * used, however, to create concrete repository instances.
 * </p>
 * <p>
 * This class must be subclassed by clients that want to contribute a repository
 * provider to the CloudFree Platform. However, it is typically not referenced
 * directly outside the CloudFree Platform.
 * </p>
 */
public abstract class RepositoryProvider extends PlatformObject {

	/** repository provider providerId */
	private final String providerId;

	/** repository type */
	private final Class<? extends Repository> repositoryType;

	/**
	 * Creates a new instance using the specified provider providerId.
	 * 
	 * @param providerId
	 *            the repository provider providerId (may not be
	 *            <code>null</code>, will be
	 *            {@link Repository#isValidId(String) validated})
	 * @param repositoryType
	 *            the public repository type contract provided by this provider
	 *            (may not be <code>null</code>, the type name will be
	 *            {@link Repository#isValidId(String) validated})
	 * @see Repository#isValidId(String)
	 */
	protected RepositoryProvider(final String id, final Class<? extends Repository> repositoryType) {
		if (null == id) {
			throw new IllegalArgumentException("repository provider providerId must not be null");
		}
		if (!Repository.isValidId(id)) {
			throw new IllegalArgumentException(MessageFormat.format("repository provider providerId \"{0}\" is invalid; valid chars are US-ASCII a-z / A-Z / 0-9 / '.' / '-' / '_'", id));
		}
		providerId = id;

		if (null == repositoryType) {
			throw new IllegalArgumentException("repository type must not be null");
		}
		if (!Repository.isValidId(repositoryType.getName())) {
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
	 * Returns the name of the repository. This is the name of the class without
	 * the package name and used by {@link #toString()}.
	 * 
	 * @return the name of the repository
	 */
	private final String getName() {
		String string = getClass().getName();
		final int index = string.lastIndexOf('.');
		if (index != -1) {
			string = string.substring(index + 1, string.length());
		}
		return string;
	}

	/**
	 * Returns the repository provider providerId.
	 * 
	 * @return the repository provider providerId
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
	 * The type name was {@link Repository#isValidId(String) validated} during
	 * provider initialization.
	 * </p>
	 * 
	 * @return the public repository type contract name
	 * @see Repository#isValidId(String)
	 * @see RepositoryProvider#getRepositoryType()
	 */
	public final String getRepositoryTypeName() {
		return getRepositoryType().getName();
	}

	/**
	 * Returns a string containing a concise, human-readable description of the
	 * repository.
	 * 
	 * @return a string representation of the repository
	 */
	@Override
	public final String toString() {
		return getName() + " {" + getProviderId() + "}";
	}
}
