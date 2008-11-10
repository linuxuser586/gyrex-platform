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
package org.eclipse.cloudfree.persistence.storage.type;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

/**
 * A registration exception indicates that a repository type registration was
 * not successful.
 */
public class RegistrationException extends CoreException {

	/** error code that indicates a conflicting id */
	public static final int CONFLICTING_ID = 1;

	/** serialVersionUID */
	private static final long serialVersionUID = -3308934889615200412L;

	/**
	 * Creates a new registration exception using the specified status.
	 * 
	 * @param status
	 *            the status
	 */
	public RegistrationException(final IStatus status) {
		super(status);
	}

}
