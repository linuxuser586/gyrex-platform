/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context.services;

/**
 * A service locator may be used to locate OSGi services in a context dependent
 * way.
 * <p>
 * Easily put, this is a centrally implemented context specific service ranking
 * or filtering mechanism. The called just wants an OSGi service. However,
 * multiple implementations might be available at runtime. Not all contexts
 * might be privileged to access all services. The service locator makes this
 * <em>configurable</em> at runtime.
 * </p>
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IRuntimeContextServiceLocator {

}
