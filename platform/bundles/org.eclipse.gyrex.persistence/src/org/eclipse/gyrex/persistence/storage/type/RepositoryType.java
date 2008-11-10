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
package org.eclipse.cloudfree.persistence.storage.type;


import org.eclipse.cloudfree.persistence.storage.Repository;
import org.eclipse.cloudfree.persistence.storage.settings.IRepositoryPreferences;
import org.eclipse.core.runtime.PlatformObject;

/**
 * A repository type defines the underlying storage type as well as the
 * technology used to store data (eg. a database based repository type accessed
 * via JDBC, a flat file based repository type, etc.).
 * <p>
 * Repository types can be dynamically registered to the CloudFree Platform by
 * registering {@link RepositoryType} instances as OSGi services. Repository
 * type providers are considered core elements of the CloudFree Platform.
 * Security may be used to only allow a set of well known (i.e. trusted)
 * providers.
 * </p>
 * <p>
 * Repository types do not represent a concrete repository. They will be used,
 * however, to create concrete repository instances.
 * </p>
 * <p>
 * You may think of repository types as a definition of a set of technologies
 * used to store data. For example, a very simple repository type could be a
 * hash map repository type. It stores data in a simple hash map. A more
 * advanced repository type could be a JDBC repository type. It would store data
 * using JDBC in a relational database. Another example for a repository type
 * would be a LDAP repository type. It stores data in an LDAP server.
 * </p>
 * <p>
 * This class must be subclassed by clients that want to contribute a repository
 * type to the CloudFree Platform. However, it is typically not referenced
 * directly outside the CloudFree Platform.
 * </p>
 */
public abstract class RepositoryType extends PlatformObject {

	/** repository type id */
	private final String id;

	/**
	 * Creates a new instance using the specified type id.
	 * 
	 * @param id
	 *            the repository type id (may not be <code>null</code>)
	 */
	protected RepositoryType(final String id) {
		if (null == id) {
			throw new IllegalArgumentException("repositoryTypeId must not be null");
		}
		this.id = id;
	}

	/**
	 * Returns the repository type id.
	 * 
	 * @return the repository type id
	 */
	public final String getId() {
		return id;
	}

	/**
	 * Returns the name of the repository. This is the name of the class without
	 * the package name and used by {@link #toString()}.
	 * 
	 * @return the name of the repository
	 */
	private String getName() {
		String string = getClass().getName();
		final int index = string.lastIndexOf('.');
		if (index != -1) {
			string = string.substring(index + 1, string.length());
		}
		return string;
	}

	/**
	 * Called by the persistence API to create a new repository instance.
	 * 
	 * @param repositoryId
	 *            the repository id
	 * @param repositoryPreferences
	 *            the repository preverences
	 * @return a new {@link Repository} instance
	 */
	public abstract Repository newRepositoryInstance(String repositoryId, IRepositoryPreferences repositoryPreferences);

	/**
	 * Returns a string containing a concise, human-readable description of the
	 * repository.
	 * 
	 * @return a string representation of the repository
	 */
	@Override
	public String toString() {
		return getName() + " {" + getId() + "}";
	}
}
