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
package org.eclipse.gyrex.cloud.internal.zk;

/**
 * Connection listener contract for {@link ZooKeeperGate}.
 */
public interface ZooKeeperGateListener {

	/**
	 * Session to ZooKeeper has been closed.
	 * <p>
	 * No interaction with ZooKeeper is possible.
	 * </p>
	 * 
	 * @param gate
	 *            the gate which is down
	 */
	void gateDown(ZooKeeperGate gate);

	/**
	 * The gate is in the process of reestablishing a connection to ZooKeeper
	 * and recovering a session.
	 * <p>
	 * No interaction with ZooKeeper is possible.
	 * </p>
	 * 
	 * @param gate
	 *            the gate which is recovering
	 */
	void gateRecovering(ZooKeeperGate gate);

	/**
	 * A session has been establish (or recovered) and the system can interact
	 * with ZooKeeper.
	 * 
	 * @param gate
	 *            the gate which is up
	 */
	void gateUp(ZooKeeperGate gate);
}