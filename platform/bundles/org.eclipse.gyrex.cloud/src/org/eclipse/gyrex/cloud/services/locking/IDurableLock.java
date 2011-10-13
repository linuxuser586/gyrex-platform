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
package org.eclipse.gyrex.cloud.services.locking;

/**
 * A distributed, durable, global synchronous lock used to control access to an
 * exclusive resource with recovery semantics.
 * <p>
 * This lock is distributed and not reentrant. It exists on a remote system and
 * is globally synchronous, i.e. at any snapshot in time no two clients think
 * they hold the same lock. A client in this definition may be another thread in
 * another process or system or another thread within the same process.
 * </p>
 * <p>
 * Persistent locks are durable <em>across</em> sessions. As such, they survive
 * typical failures in a distributed system for example connection loss or
 * interruption. However, the lock may become invalid ({@link #isValid()}
 * returns <code>false</code>) as a result of such a failure and must be
 * explicitly recovered. When a lock becomes invalid any operation protected by
 * the lock must be suspended and may only be resumed if the lock could be
 * recovered.
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
public interface IDurableLock extends IDistributedLock {

	/**
	 * Returns a key that must be provided for recovering the lock.
	 * <p>
	 * The key may only be used to recover the current lock. It cannot be used
	 * to recover a lock multiple times.
	 * </p>
	 * 
	 * @return the key for recovering the lock
	 */
	String getRecoveryKey();

}
