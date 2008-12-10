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

import org.eclipse.core.runtime.IAdaptable;

/**
 * Marker interface for all model objects.
 * <p>
 * Model objects are part of the model layer and made accessible through
 * {@link IModelManager model managers}.
 * </p>
 * <p>
 * In order to allow for great flexibility and extensibility this interface
 * extends the {@link IAdaptable} interface.
 * </p>
 * <p>
 * By definition all model objects in the CloudFree platform
 * <strong>must</strong> implement this interface.
 * </p>
 */
public interface IModelObject extends IAdaptable {

}
