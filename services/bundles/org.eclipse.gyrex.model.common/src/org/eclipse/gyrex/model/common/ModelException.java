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
package org.eclipse.cloudfree.model.common;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;

/**
 * Checked exception for the CloudFree model.
 * <p>
 * Model operations will typically throw this exception in case of problems
 * occurred in the model. A status can be further queried for the problem
 * details and severity.
 * </p>
 * <p>
 * This class may be instantiated or subclassed by model contributors.
 * </p>
 */
public class ModelException extends CoreException {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance.
	 * 
	 * @param status
	 */
	public ModelException(final IStatus status) {
		super(status);
	}

}
