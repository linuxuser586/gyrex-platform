/*******************************************************************************
 * Copyright (c) 2010 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.model.common.contracts;

/**
 * A model object which can be modified in memory.
 * 
 * @param <M>
 *            the model manager API type
 */
public interface IModifiableInMemory {

	/**
	 * Indicates if a model object has been modified from its original loaded
	 * state.
	 * <p>
	 * This method <strong>does not</strong> check if the model object differs
	 * from the data stored in the underlying repository. It only indicates if
	 * the object has been modified <em>in memory</em> since it was loaded from
	 * the repository. Thus, this method may be called by clients in order to
	 * determine if a save operation should be performed.
	 * </p>
	 * <p>
	 * This method always returns <code>true</code> for {@link #isTransient()
	 * transient} objects.
	 * </p>
	 * 
	 * @return <code>true</code> if the model object has been modified since it
	 *         was loaded, <code>false</code> otherwise
	 */
	boolean isDirty();

	/**
	 * Indicates if a model object is transient.
	 * <p>
	 * A transient model object has never been loaded from a repository, i.e. it
	 * only exists locally in memory.
	 * </p>
	 * 
	 * @return <code>true</code> if the object has never been loaded from a
	 *         repository, <code>false</code> otherwise
	 */
	boolean isTransient();

}
