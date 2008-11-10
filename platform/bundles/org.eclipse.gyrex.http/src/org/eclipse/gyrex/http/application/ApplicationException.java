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
package org.eclipse.cloudfree.http.application;

/**
 * An <code>ApplicationException</code> is thrown to indicate an error occurred
 * which interferes with the normal application operation.
 * <p>
 * This class contains additional information about the HTTP status code which
 * needs to be send back to the client. If the response does not permit sending
 * of a status code (eg., it has been committed already), the exception may be
 * logged by the Platform in an application specific manner.
 * </p>
 * <p>
 * This class extends runtime exception because the exception is handled at the
 * runtime by the CloudFree Platform.
 * </p>
 * <p>
 * This class may be extended by clients.
 * </p>
 */
public class ApplicationException extends RuntimeException {

	/** serialVersionUID */
	private static final long serialVersionUID = -7570131397288613535L;

	/** the HTTP status code */
	private final int status;

	/**
	 * Creates a new application exception using the specified HTTP status code
	 * and message.
	 * 
	 * @param status
	 *            the HTTP status code
	 * @param message
	 *            the status message
	 */
	public ApplicationException(final int status, final String message) {
		this(status, message, null);
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param status
	 *            the HTTP status code
	 * @param message
	 *            the status message
	 * @param cause
	 *            an optional Throwable which may caused this status
	 */
	public ApplicationException(final int status, final String message, final Throwable cause) {
		super(message, cause);
		this.status = status;
		if (null == message) {
			throw new IllegalArgumentException("message must not be null");
		}
	}

	/**
	 * Creates a new application exception using HTTP status code 500 and the
	 * specified message.
	 * 
	 * @param message
	 *            the status message
	 */
	public ApplicationException(final String message) {
		this(500, message, null);
	}

	/**
	 * Creates a new application exception using HTTP status code 500 and the
	 * message provided by the specified <code>Throwable</code>.
	 * 
	 * @param status
	 *            the HTTP status code
	 * @param message
	 *            the status message
	 * @throws NullPointerException
	 *             if <code>cause</code> is <code>null</code>
	 */
	public ApplicationException(final Throwable cause) {
		this(500, null != cause.getMessage() ? cause.getMessage() : "Internal Error", cause);
	}

	/**
	 * Returns the HTTP status.
	 * 
	 * @return the HTTP status
	 */
	public int getStatus() {
		return status;
	}

}
