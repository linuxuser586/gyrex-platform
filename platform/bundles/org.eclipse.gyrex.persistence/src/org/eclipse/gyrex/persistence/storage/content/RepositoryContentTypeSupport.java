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
package org.eclipse.gyrex.persistence.storage.content;

import org.eclipse.gyrex.persistence.storage.Repository;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Provides support for working with content types in a repository.
 * <p>
 * This class must be subclassed by clients that contribute a repository type to
 * Gyrex.
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
	 * @param progressMonitor
	 *            a monitor for reporting progress an checking cancellation if
	 *            supported (may be <code>null</code> if progress reporting is
	 *            not desired)
	 * @return a status indicating the result of the provisioning operation
	 */
	public final IStatus provision(final RepositoryContentType contentType, final IProgressMonitor progressMonitor) {
		return Status.CANCEL_STATUS;
	}

}
