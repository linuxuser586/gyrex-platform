/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal.queue;

import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * Exception thrown whenever a queue operation failed.
 */
public class QueueOperationFailedException extends IllegalStateException {

	/** serialVersionUID */
	private static final long serialVersionUID = 8107783506957003714L;

	static String getMessage(final String queueId, final String operationDetail, final Throwable cause) {
		if (cause != null) {
			return String.format("Operation '%s' on queue '%s' failed. %s", operationDetail, queueId, ExceptionUtils.getRootCauseMessage(cause));
		} else {
			return String.format("Operation '%s' on queue '%s' failed. %s", operationDetail, queueId, "Please check the server log files.");
		}
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param message
	 * @param cause
	 */
	public QueueOperationFailedException(final String queueId, final String operationDetail, final Throwable cause) {
		super(getMessage(queueId, operationDetail, cause), cause);
	}

}
