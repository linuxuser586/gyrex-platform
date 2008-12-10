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

import org.eclipse.cloudfree.common.context.IContext;
import org.eclipse.cloudfree.model.common.provider.BaseModelManager;
import org.eclipse.cloudfree.model.common.provider.ModelProvider;
import org.eclipse.core.runtime.IAdaptable;

/**
 * A model manager provides access to {@link IModelObject model objects}.
 * <p>
 * It implements CRUD model operations and basic query capabilities. The model
 * manager is the lowest possible access to the model. It is intended to be used
 * by a service layer built on top of it which would provide more powerful
 * capabilities such as meaningful business operations.
 * </p>
 * <p>
 * Model manager operations are - by definition - mostly simple, short running
 * and not distributed, do not implement a particular business context and
 * typically execute within their objects only.
 * </p>
 * <p>
 * Model manager implementations depend on a particular {@link IContext context}
 * and a repository assigned to it. Thus, there is no central manager registry.
 * Instead, clients contribute a {@link ModelProvider model manager factory}
 * which creates model manager instances based on a {@link IContext context}.
 * This also means that callers must not hold onto a specific model manager
 * instance but obtain a fresh one for a particular {@link IContext context}
 * using {@link ModelUtil#getManager(Class, IContext)}.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients directly. Clients
 * that want to contribute a model <strong>must</strong> subclass
 * {@link BaseModelManager} instead. These implementations must be contributed
 * using a {@link ModelProvider factory}.
 * </p>
 * 
 * @see ModelUtil#getManager(Class, IContext)
 * @see ModelProvider
 * @noimplement This interface is not intended to be implemented by clients
 *              directly. They must subclass {@link BaseModelManager} instead.
 */
public interface IModelManager extends IAdaptable {

	/**
	 * Returns the context the manager operates in.
	 * 
	 * @return the manager context
	 */
	IContext getContext();

}
