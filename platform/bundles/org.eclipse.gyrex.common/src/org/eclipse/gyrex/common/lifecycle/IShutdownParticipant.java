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
package org.eclipse.gyrex.common.lifecycle;

/**
 * A shutdown listener that gets notified whenever the bundle it is registered
 * with is shut down.
 * <p>
 * A shutdown listener can be registered with a bundle. When the bundle is shut
 * down the <code>{@link #shutdown()}</code> method will be called to allow the
 * listener to participate in the shutdown process.
 * </p>
 * <p>
 * This interface is intended to be implemented by clients.
 * </p>
 */
public interface IShutdownParticipant {

	/**
	 * Called when the bundle is shut down.
	 * <p>
	 * Implementor should perform any necessary cleanup and release of
	 * resources.
	 * </p>
	 * <p>
	 * Note that although the shutdown participant is allowed to throw
	 * exceptions in the shutdown process it is generally anticipated that all
	 * resources are released anyhow so that the participant is prepared for
	 * garbage collection.
	 * </p>
	 * 
	 * @throws Exception
	 *             if an exception occurred during the shutdown process
	 */
	void shutdown() throws Exception;

}
