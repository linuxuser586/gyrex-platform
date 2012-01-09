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
package org.eclipse.gyrex.http.application.context;

import org.eclipse.gyrex.http.internal.HttpActivator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * Indicated that a requested alias is already in use.
 * <p>
 * Thrown to indicate an error with the caller's request to register a servlet
 * or resources into the URI namespace of the Application.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public final class NamespaceException extends CoreException {

	/** serialVersionUID */
	private static final long serialVersionUID = 68131811778573786L;

	/**
	 * Creates a new namespace exception for the specified alias.
	 * 
	 * @param alias
	 *            the alias already in use
	 */
	public NamespaceException(final String alias) {
		super(new Status(IStatus.ERROR, HttpActivator.SYMBOLIC_NAME, "Alias '" + alias + "' already in use.", null));
	}
}
