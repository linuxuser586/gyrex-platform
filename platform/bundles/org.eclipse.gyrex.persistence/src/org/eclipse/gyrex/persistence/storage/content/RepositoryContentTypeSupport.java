/*******************************************************************************
 * Copyright (c) 2008, 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.storage.content;

import org.eclipse.gyrex.persistence.storage.Repository;

/**
 * Provides support for working with content types in a repository.
 * <p>
 * In Gyrex a repository is associated with a {@link RepositoryContentType
 * content type}. This association is verified and managed using
 * {@link RepositoryContentTypeSupport}. Typically, a repository provides an
 * implementation which may be used for verifying that a repository
 * {@link #isSupported(RepositoryContentType) supports} a particular content
 * type. However, {@link #isSupported(RepositoryContentType) supported} does not
 * mean that it's ready to be used. There might be pending provisioning
 * operations that need to be executed because of version differences between a
 * given content type and the content type in the repository.
 * </p>
 * <p>
 * The {@link RepositoryContentTypeSupport} heavily relies on correct
 * {@link RepositoryContentType#getVersion() content type versioning}. It is
 * expected, that the versioning follows OSGi semantics especially regarding
 * compatible and breaking backwards compatible changes.
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
public abstract class RepositoryContentTypeSupport {

//	/**
//	 * Returns the provisioning status of the specified content type.
//	 *
//	 * @param contentType
//	 *            the content type (may not be <code>null</code>)
//	 * @param progressMonitor
//	 *            a monitor for reporting progress an checking cancellation if
//	 *            supported (may be <code>null</code> if progress reporting is
//	 *            not desired)
//	 * @return a status indicating the provisioning status of the specified
//	 *         content type
//	 * @throws IllegalArgumentException
//	 *             if the content type is not supported at all (eg.
//	 *             incompatible)
//	 * @throws RepositoryException
//	 *             in case of errors communicating or working with the
//	 *             repository
//	 */
//	public abstract ProvisioningStatus getProvisioningStatus(RepositoryContentType contentType, final IProgressMonitor progressMonitor) throws IllegalArgumentException, RepositoryException;
//
	/**
	 * Returns the repository.
	 * 
	 * @return the repository
	 */
	public abstract Repository getRepository();

	/**
	 * Indicates if the specified content type is supported by the
	 * {@link #getRepository() repository}.
	 * <p>
	 * Returns <code>true</code> if the repository is capable of storing and
	 * configured properly in order to store content of the specified type. For
	 * example, a relational database backed repository might return
	 * <code>true</code> if it <em>should</em> contain the tables for storing
	 * data of the specified type. However, the database schema may not
	 * represent exactly the specified content type version.
	 * </p>
	 * 
	 * @param contentType
	 *            the content type (may not be <code>null</code>)
	 * @return <code>true</code> if the content type is supported,
	 *         <code>false</code> otherwise
	 */
	public abstract boolean isSupported(RepositoryContentType contentType);

//	/**
//	 * Provisions the specified content type for the {@link #getRepository()
//	 * repository}.
//	 * <p>
//	 * The method returns a {@link IStatus status object} indicating the result
//	 * of the provision operation. In case the {@link IStatus#getSeverity()
//	 * severity} of the status object returned is one of {@link IStatus#OK},
//	 * {@link IStatus#INFO} or {@link IStatus#WARNING} the provisioning
//	 * operation is considered successful, i.e. subsequent calls to
//	 * {@link #isSupported(RepositoryContentType)} will return <code>true</code>
//	 * after this method returned. In all other cases the provisioning operation
//	 * must be considered failed.
//	 * </p>
//	 * <p>
//	 * Note that it's not possible to provision individual pending operations
//	 * but only the full set of pending operations as detected by
//	 * {@link #getProvisioningStatus(RepositoryContentType, IProgressMonitor)}.
//	 * </p>
//	 *
//	 * @param contentType
//	 *            the content type (may not be <code>null</code>)
//	 * @param progressMonitor
//	 *            a monitor for reporting progress an checking cancellation if
//	 *            supported (may be <code>null</code> if progress reporting is
//	 *            not desired)
//	 * @return a status indicating the result of the provisioning operation
//	 * @throws IllegalArgumentException
//	 *             if the content type is not supported at all (eg.
//	 *             incompatible)
//	 */
//	public abstract IStatus provision(final RepositoryContentType contentType, final IProgressMonitor progressMonitor) throws IllegalArgumentException;

}
