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

import org.eclipse.gyrex.persistence.storage.Repository;

/**
 * Base class for exceptions thrown by {@link Repository repositories}.
 * <p>
 * The Repository API defines a basic set of exceptions as a foundation of
 * structured error handling. The base class is {@link RepositoryException}
 * which extends {@link RuntimeException}. Repository implementors are
 * encouraged to re-use as many exceptions as possible. This allows client code
 * to handle a basic set of the common exceptions without depending on
 * implementation specific code (eg. JPA, JDO, JDBC, etc.).
 * </p>
 * <p>
 * This class may be subclassed by repository implementors to provide a more
 * sophisticated exception handling. Implementors are encourage to contribute
 * additional exceptions to the Repository API package.
 * </p>
 */
public abstract class RepositoryException extends RuntimeException {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance.
	 * 
	 * @param message
	 *            the error message with detailed information
	 */
	public RepositoryException(final String message) {
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
	public RepositoryException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
