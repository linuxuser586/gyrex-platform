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
package org.eclipse.cloudfree.services.common;


import org.eclipse.cloudfree.common.context.IContext;
import org.eclipse.cloudfree.services.common.provider.BaseService;
import org.eclipse.cloudfree.services.common.provider.ServiceProvider;
import org.eclipse.core.runtime.IAdaptable;

/**
 * A CloudFree services.
 * <p>
 * It implements business operations and query capabilities. The service is the
 * lowest possible access to the service. It is intended to be used by a service
 * layer built on top of it which would provide more powerful capabilities such
 * as meaningful business operations.
 * </p>
 * <p>
 * Service operations - by definition - can be long running, work fully within a
 * distributed environment, do implement a particular business context and can
 * execute across several objects.
 * </p>
 * <p>
 * Service implementations depend on a particular
 * <em>{@link IContext context}</em>. Thus, there is no central service
 * registry. Instead, clients contribute a {@link ServiceProvider service
 * factory} which creates service instances based on a {@link IContext context}.
 * This also means that callers must not hold onto a specific service instance
 * but obtain a fresh one for a particular {@link IContext context} using
 * {@link ServiceUtil#getService(Class, IContext)}.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients directly. Clients
 * that want to contribute a service <strong>must</strong> subclass
 * {@link BaseService} instead. These implementations must be contributed using
 * a {@link ServiceProvider factory}.
 * </p>
 * 
 * @see ServiceUtil#getService(Class, IContext)
 * @see ServiceProvider
 * @noimplement This interface is not intended to be implemented by clients
 *              directly. They must subclass {@link BaseService} instead.
 */
public interface IService extends IAdaptable {

	/**
	 * Returns the context the service operates in.
	 * 
	 * @return the service context
	 */
	IContext getContext();

}
