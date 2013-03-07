/**
 * Copyright (c) 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.context.definitions;

import java.util.List;

import org.eclipse.gyrex.context.IRuntimeContext;

import org.eclipse.core.runtime.IPath;

/**
 * A manager for defining {@link IRuntimeContext contexts}.
 * <p>
 * The manager provides APIs for managing {@link IRuntimeContext contexts}. It
 * is made available to clients as an OSGi service. Security restrictions may
 * not allow all code to access the manager.
 * </p>
 * <p>
 * Note, this class is part of an administration API which may evolve faster
 * than the general contextual runtime API. Please get in touch with the
 * development team through the prefered channels listed on <a
 * href="http://www.eclipse.org/gyrex">the Gyrex website</a> to stay up-to-date
 * of possible changes.
 * </p>
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 * @see IRuntimeContext
 */
public interface IRuntimeContextDefinitionManager {

	/**
	 * Returns all defined contexts.
	 * 
	 * @return an unmodifiable list of defined contexts
	 */
	List<ContextDefinition> getDefinedContexts();

	/**
	 * Returns the specified context definition.
	 * <p>
	 * Returns <code>null</code> if no such context is defined.
	 * </p>
	 * 
	 * @param contextPath
	 *            the context path
	 * @return the context definition (maybe <code>null</code>)
	 */
	ContextDefinition getDefinition(IPath contextPath);

	/**
	 * Removes (i.e. undefines) the specified context.
	 * 
	 * @param contextPath
	 *            the context path
	 * @throws Exception
	 *             if an error occurred removing the context definition
	 */
	void removeDefinition(final IPath contextPath) throws Exception;

	/**
	 * Saves the specified context definition.
	 * <p>
	 * If the context was not defined previously it will be defined as a result
	 * of this operation. If a context is already defined, it will be updated
	 * with values from the specified definition.
	 * </p>
	 * 
	 * @param contextDefinition
	 *            the context definition
	 * @throws Exception
	 *             if an error occurred defining or updating the context
	 *             definition
	 */
	void saveDefinition(final ContextDefinition contextDefinition) throws Exception;

}
