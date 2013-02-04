/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.services.locking;

/**
 * A distributed, global synchronous lock used to control access to an exclusive
 * resource.
 * <p>
 * This lock is distributed and not reentrant. It exists on a remote system and
 * is globally synchronous, i.e. at any snapshot in time no two clients think
 * they hold the same lock. A client in this definition may be another thread in
 * another process or system or another thread within the same process.
 * </p>
 * <p>
 * Exclusive locks are <em>session</em> locks. A session in this definition may
 * be a single, uninterrupted connection to the lock system. Thus, whenever the
 * connection is lost (or interrupted) the session may end and the lock is
 * released automatically by the system. Therefore, a client which acquires a
 * lock must check regularly if it still {@link #isValid()} owns a lock. The
 * documentation of the underlying implementation should provide more
 * information on the term session.
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
public interface IExclusiveLock extends IDistributedLock {

}
