/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.services.locking;

/**
 * A distributed, global synchronous lock used to control access to an exclusive
 * resource.
 * <p>
 * This lock is distributed and not reentrant. It exists on a remote system and
 * is globally synchronous, i.e. at any snapshot in time no two clients think
 * they hold the same lock. A client in this sense may be another process,
 * another system or another thread.
 * </p>
 * <p>
 * It is very important that acquired locks eventually get released. Calls to
 * release should be done in a finally block to ensure they execute. For
 * example:
 * 
 * <pre>
 * try {
 * 	lock.acquire(timeout);
 * 	// ... do work here ...
 * } finally {
 * 	lock.release();
 * }
 * </pre>
 * 
 * Note: <code>lock.acquire</code> may fail. As such, it is good practice to
 * place it inside the try block. Releasing without acquiring is far less
 * catastrophic than acquiring without releasing.
 * </p>
 * <p>
 * Exclusive locks are persistent locks. However, due to the nature of
 * distributed systems they are designed with failures in mind. Therefore, the
 * lock lifecycle is bound to a timeout. A client which acquires a lock receives
 * a lease that must be renewed regularly within a defined timeout.
 * </p>
 * <p>
 * This interface is typically not implemented by clients but by service
 * providers. As such it is considered part of a service provider API which may
 * evolve faster than the general API. Please get in touch with the development
 * team through the prefered channels listed on <a
 * href="http://www.eclipse.org/gyrex">the Gyrex website</a> to stay up-to-date
 * of possible changes for implementors.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IExclusiveLock {

}
