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
package org.eclipse.gyrex.cloud.services.queue;

/**
 * A queue message.
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
public interface IMessage {

	/**
	 * Returns the message body.
	 * <p>
	 * Note, the message body is returned directly, i.e. no copy. Any
	 * modification to the returned array will disrupt the message body. Callers
	 * must create a backup/copy themselves if they want to modify the returned
	 * array without disrupting the message body.
	 * </p>
	 * 
	 * @return the message body
	 */
	byte[] getBody();

	/**
	 * Returns the identifier of the queue.
	 * 
	 * @return the queue identifier
	 */
	String getQueueId();

	/**
	 * Returns a human readable string describing the message.
	 * <p>
	 * The returned string should be used for logging purposes. It gives further
	 * information about the message to administrators, operators and developers
	 * which aid in analyzing and discovering message issues.
	 * </p>
	 * 
	 * @return a human readable string
	 */
	@Override
	public String toString();
}
