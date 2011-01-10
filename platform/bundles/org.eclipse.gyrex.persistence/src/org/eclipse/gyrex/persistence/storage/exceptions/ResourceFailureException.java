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
 * Repository exception thrown when an underlying resource failure occurred.
 * <p>
 * This exception might be temporary (eg. intermittent connection issues) or
 * permanent (eg. a broken database). Typically, when such an exception is
 * received, the operation might be retried at a later point in time. However,
 * there is no guarantee that it will succeed the next time. Therefore, other
 * steps of handling the issues (eg. informing administrators) should be
 * considered as well.
 * </p>
 * <p>
 * This class may be subclassed by repository implementors to provide a more
 * detailed failure information. Although clients might not be interested in
 * handling implementation specific failures, administrators are.
 * </p>
 */
public class ResourceFailureException extends RepositoryException {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance.
	 * 
	 * @param message
	 *            the error message with detailed information
	 */
	public ResourceFailureException(final String message) {
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
	public ResourceFailureException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
