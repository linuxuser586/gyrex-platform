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
package org.eclipse.gyrex.services.common;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.services.common.provider.BaseService;
import org.eclipse.gyrex.services.common.provider.ServiceProvider;

/**
 * A Gyrex service.
 * <p>
 * In Gyrex services implement business operations and more sophisticated
 * query capabilities. A service provides more powerful capabilities on top of
 * the model layer. As such, services might be exposed to a broader group then
 * the underlying model layer.
 * </p>
 * <p>
 * Service operations - by definition - can be long running, work fully within a
 * distributed environment, do implement a particular business context and can
 * execute across several objects.
 * </p>
 * <p>
 * Service implementations depend on a particular
 * <em>{@link IRuntimeContext context}</em>. Thus, there is no central service
 * registry. Instead, clients contribute a {@link ServiceProvider service
 * factory} which creates service instances based on a {@link IRuntimeContext context}.
 * This also means that callers must not hold onto a specific service instance
 * but obtain a fresh one for a particular {@link IRuntimeContext context} using
 * {@link ServiceUtil#getService(Class, IRuntimeContext)}.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients directly. Clients
 * that want to contribute a service <strong>must</strong> subclass
 * {@link BaseService} instead. These implementations must be contributed using
 * a {@link ServiceProvider factory}.
 * </p>
 * 
 * @see ServiceUtil#getService(Class, IRuntimeContext)
 * @see ServiceProvider
 * @noimplement This interface is not intended to be implemented by service
 *              implementors directly. They must subclass {@link BaseService}
 *              instead.
 */
public interface IService extends IAdaptable {

	/**
	 * Returns the context the service operates in.
	 * 
	 * @return the service context
	 */
	IRuntimeContext getContext();

}
