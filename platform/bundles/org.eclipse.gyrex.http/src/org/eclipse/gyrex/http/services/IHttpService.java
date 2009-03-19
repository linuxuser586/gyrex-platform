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
package org.eclipse.gyrex.http.services;

import org.eclipse.core.runtime.IAdaptable;

/**
 * A Gyrex HTTP service.
 * <p>
 * A central concept of Gyrex are HTTP services. Services
 * provide a common interface to access functionality offered by applications.
 * </p>
 * <p>
 * Gyrex HTTP services follow a REST style. Typically, they wrap a Java
 * service provided by the application service layer. This allows to develop the
 * actual service functionality independent from the technology used to export
 * the services via HTTP.
 * </p>
 * <p>
 * This interface must be implemented by clients that want to contribute a HTTP
 * service to the platform.
 * </p>
 */
public interface IHttpService extends IAdaptable {

}
