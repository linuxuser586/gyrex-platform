/*******************************************************************************
 * Copyright (c) 2011, 2013 AGETO Service GmbH and others.
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
 * Interface with shared property constants.
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IQueueServiceProperties {

	/**
	 * Optional property which allows to specify an authorization token for a
	 * queue. A queue service may protect access to queues using such a token.
	 */
	String AUTHORIZATION_TOKEN = "queue.authorization.token";

	/**
	 * Optional property which allows to specify a visibility timeout for
	 * received messages in milliseconds.
	 * <p>
	 * The property value must be of type {@link Long} or <code>long</code>.
	 * </p>
	 * <p>
	 * Queue implementation providers may only support a limited range. Due to
	 * the communication overhead with a queue service through a network it may
	 * not make sense to use too small values.
	 * </p>
	 * 
	 * @see IQueue#receiveMessages(int, java.util.Map)
	 */
	String MESSAGE_RECEIVE_TIMEOUT = "queue.message.receive.timeout";

}
