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
package org.eclipse.gyrex.cloud.services.queue;

/**
 * An unchecked exception thrown by {@link IQueueService} when a queue already
 * exists.
 */
public final class DuplicateQueueException extends RuntimeException {

	/** serialVersionUID */
	private static final long serialVersionUID = -2844312618464571431L;

	/**
	 * Creates a new instance.
	 * 
	 * @param message
	 */
	public DuplicateQueueException(final String message) {
		super(message);
	}

}
