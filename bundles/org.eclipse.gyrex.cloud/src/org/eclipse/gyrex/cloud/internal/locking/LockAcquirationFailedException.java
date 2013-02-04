/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal.locking;

import org.apache.commons.lang.exception.ExceptionUtils;

/**
 *
 */
public class LockAcquirationFailedException extends IllegalStateException {

	/** serialVersionUID */
	private static final long serialVersionUID = -3221127134646900105L;

	/**
	 * Creates a new instance.
	 * 
	 * @param lockId
	 * @param reason
	 */
	public LockAcquirationFailedException(final String lockId, final String reason) {
		super(String.format("Unable to acquire lock %s. %s", lockId, reason));
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param lockId
	 * @param cause
	 */
	public LockAcquirationFailedException(final String lockId, final Throwable cause) {
		super(String.format("Unable to acquire lock %s. %s", lockId, ExceptionUtils.getRootCauseMessage(cause)), cause);
	}
}
