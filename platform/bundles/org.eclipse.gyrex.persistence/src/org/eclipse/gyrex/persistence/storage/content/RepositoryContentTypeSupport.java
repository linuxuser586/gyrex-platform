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
package org.eclipse.cloudfree.persistence.storage.content;


import org.eclipse.cloudfree.persistence.storage.Repository;
import org.eclipse.core.runtime.IStatus;

/**
 * Provides support for working with content types in a repository.
 * <p>
 * This class must be subclassed by clients that contribute a repository type to
 * the CloudFree Platform.
 * </p>
 */
public abstract class RepositoryContentTypeSupport {

	/**
	 * Returns the repository.
	 * 
	 * @return the repository
	 */
	public abstract Repository getRepository();

	/**
	 * Indicates if the specified content type is supported by the
	 * {@link #getRepository() repository}.
	 * 
	 * @param contentType
	 *            the content type (may not be <code>null</code>)
	 * @return <code>true</code> if the content type is supported,
	 *         <code>false</code> otherwise
	 */
	public abstract boolean isSupported(RepositoryContentType contentType);

	/**
	 * Provisions the specified content type for the {@link #getRepository()
	 * repository}.
	 * <p>
	 * The method returns a {@link IStatus status object} indicating the result
	 * of the provision operation. In case the {@link IStatus#getSeverity()
	 * severity} of the status object returned is one of {@link IStatus#OK},
	 * {@link IStatus#INFO} or {@link IStatus#WARNING} the provisioning
	 * operation is considered successful, i.e. subsequent calls to
	 * {@link #isSupported(RepositoryContentType)} will return <code>true</code>
	 * after this method returned. In all other cases the provisioning operation
	 * must be considered failed.
	 * </p>
	 * 
	 * @param contentType
	 *            the content type (may not be <code>null</code>)
	 * @return a status indicating the result of the provisioning operation
	 */
	public final IStatus provision(final RepositoryContentType contentType) {
		// TODO Auto-generated method stub
		return null;
	}

}
