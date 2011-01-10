/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.storage.exceptions;

/**
 * Exception thrown when a key conflict occurred while creating (adding) or
 * updating data in a repository.
 * <p>
 * An example of a typical failure which causes this exception is a unique key
 * constraint violation.
 * </p>
 */
public class KeyConflictException extends RepositoryException {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance.
	 * 
	 * @param message
	 *            the error message with detailed information
	 */
	public KeyConflictException(final String message) {
		super(message);
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param message
	 *            the error message with detailed information
	 * @param cause
	 *            the root cause (eg. underlying SQLException)
	 */
	public KeyConflictException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
