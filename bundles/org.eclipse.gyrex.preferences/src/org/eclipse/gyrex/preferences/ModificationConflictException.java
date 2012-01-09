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
package org.eclipse.gyrex.preferences;

import org.osgi.service.prefs.BackingStoreException;

/**
 * Thrown to indicate that a preference modification operation could not be
 * completed because of a conflicting modification in the backing store.
 */
public class ModificationConflictException extends BackingStoreException {

	/** serialVersionUID */
	private static final long serialVersionUID = -7132658710115795768L;

	/**
	 * Creates a new instance.
	 * 
	 * @param message
	 */
	public ModificationConflictException(final String message) {
		super(message);
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param message
	 * @param cause
	 */
	public ModificationConflictException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
