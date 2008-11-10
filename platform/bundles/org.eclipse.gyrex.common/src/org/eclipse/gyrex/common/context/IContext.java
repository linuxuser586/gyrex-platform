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
package org.eclipse.cloudfree.common.context;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;

/**
 * The context for defining the runtime environment.
 * <p>
 * In the CloudFree Platform several constraints are not static for every
 * execution. They depend on an environment. This interface defines this
 * environment and is used in the CloudFree Platform to bind runtime
 * constraints.
 * </p>
 * <p>
 * Clients may implement this interface directly. However, they may also extend
 * {@link AbstractContext} instead.
 * </p>
 * 
 * @see IAdaptable
 */
public interface IContext extends IAdaptable {

	/**
	 * Returns an object which is an instance of the given class associated with
	 * this object. Returns <code>null</code> if no such object can be found.
	 * <p>
	 * In addition to the general {@link IAdaptable} contract the context may
	 * perform further filtering depending on visibility and security
	 * constrains.
	 * </p>
	 * 
	 * @param adapter
	 *            the adapter class to look up
	 * @return a object castable to the given class, or <code>null</code> if
	 *         this object does not have an adapter for the given class
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter);

	/**
	 * Returns the context path.
	 * <p>
	 * Each context has a path which uniquely identifies the context across the
	 * CloudFree Platform in a persistent manner. This means that the identifier
	 * must not change across subsequent invocations and sessions.
	 * </p>
	 * 
	 * @return the context path (may not be <code>null</code>)
	 */
	IPath getContextPath();
}
