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

import org.eclipse.gyrex.model.common.IModelManager;

/**
 * A model object that is aware of its {@link IModelManager model manager}.
 * 
 * @param <M>
 *            the model manager API type
 */
public interface IModelManagerAware<M extends IModelManager> {

	/**
	 * Returns the model manager which is responsible for the manager.
	 * 
	 * @return the model manager
	 * @throws IllegalStateException
	 *             if the manager could not be determined
	 */
	M getManager() throws IllegalStateException;

}
