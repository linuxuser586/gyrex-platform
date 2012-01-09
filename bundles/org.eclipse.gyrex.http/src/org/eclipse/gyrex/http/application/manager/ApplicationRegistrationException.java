/*******************************************************************************
 * Copyright (c) 2008, 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.application.manager;

import org.eclipse.gyrex.http.internal.HttpActivator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;

/**
 * Indicates that an application could not be registered because an application
 * with the specified id is already registered.
 */
public class ApplicationRegistrationException extends CoreException {

	/** serialVersionUID */
	private static final long serialVersionUID = 3874306747279648987L;

	/**
	 * Creates a new instance.
	 * 
	 * @param applicationId
	 *            the application id
	 */
	public ApplicationRegistrationException(final String applicationId) {
		super(new Status(IStatus.ERROR, HttpActivator.SYMBOLIC_NAME, NLS.bind("Application '{0}' already registered.", applicationId), null));
	}
}
