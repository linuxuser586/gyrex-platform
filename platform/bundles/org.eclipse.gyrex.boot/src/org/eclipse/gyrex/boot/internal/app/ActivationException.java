/*******************************************************************************
 * Copyright (c) 2008, 2009 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.boot.internal.app;

/**
 * Exception during activation of a server role.
 */
public class ActivationException extends Exception {

	/** serialVersionUID */
	private static final long serialVersionUID = 1837297187148733546L;

	/**
	 * Creates a new instance.
	 * 
	 * @param message
	 */
	public ActivationException(final String message) {
		super(message);
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param message
	 * @param cause
	 */
	public ActivationException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
