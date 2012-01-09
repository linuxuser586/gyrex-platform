/**
 * Copyright (c) 2009, 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.context.registry;

import org.eclipse.gyrex.context.IRuntimeContext;

import org.eclipse.core.runtime.IPath;

/**
 * A registry for accessing {@link IRuntimeContext contexts}.
 * <p>
 * The registry provides APIs for accessing {@link IRuntimeContext contexts}. It
 * is made available to clients as an OSGi service. Security restrictions may
 * not allow all code to access the registry.
 * </p>
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 * @see IRuntimeContext
 */
public interface IRuntimeContextRegistry {

	/**
	 * Returns the runtime context with the specified path.
	 * <p>
	 * Note, security may be used to verify that the caller is allowed to access
	 * the specified context.
	 * </p>
	 * 
	 * @param contextPath
	 *            the context path
	 * @return the context (maybe <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if the context path is <code>null</code>
	 */
	public IRuntimeContext get(final IPath contextPath) throws IllegalArgumentException;

}
