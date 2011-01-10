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
package org.eclipse.gyrex.model.common.exceptions;

import org.eclipse.gyrex.model.common.ModelException;
import org.eclipse.gyrex.model.common.internal.ModelActivator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Exception thrown when an object with the same identifier already exists while
 * creating (adding) or updating a model object in a repository.
 * <p>
 * An example of a typical failure which causes this exception is a unique key
 * constraint violation.
 * </p>
 * <p>
 * This class may be instantiated or subclassed by model contributors.
 * </p>
 */
public class DuplicateObjectException extends ModelException {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance.
	 * 
	 * @param status
	 */
	public DuplicateObjectException(final IStatus status) {
		super(status);
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param message
	 *            the error message with detailed information
	 */
	public DuplicateObjectException(final String message) {
		super(new Status(IStatus.ERROR, ModelActivator.SYMBOLIC_NAME, IModelErrorCodes.DUPLICATE_OBJECT, message, null));
	}
}
