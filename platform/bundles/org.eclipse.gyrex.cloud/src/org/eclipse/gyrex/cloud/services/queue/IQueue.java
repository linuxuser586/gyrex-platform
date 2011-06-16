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
package org.eclipse.gyrex.cloud.services.queue;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 * A handle to a queue for sending and consuming messages.
 * <p>
 * A queue handle must be obtained from a queue services. References may be kept
 * around for a longer period of time as long as the queue service is still
 * active. Once a queue service goes away, any references must be released. From
 * that time on, any method invocation may throw {@link IllegalStateException}.
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
public interface IQueue {

	/**
	 * Consumes a message from the queue.
	 * <p>
	 * When a message is consumed it is immediately removed from the queue.
	 * </p>
	 * <p>
	 * If the queue is empty this method will block and wait for the specified
	 * time till a message becomes available which blocks the current thread.
	 * </p>
	 * <p>
	 * If the specified waiting time elapses and no message becomes available,
	 * <code>null</code> will be returned. If the time is less than or equal to
	 * zero, the method will not wait at all.
	 * </p>
	 * 
	 * @param timeout
	 *            the maximum wait time
	 * @param unit
	 *            the unit of the specified maximum wait time
	 * @return the message (maybe <code>null</code> if none is available)
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 * @throws IllegalStateException
	 *             if the cloud queue service is unavailable
	 * @throws SecurityException
	 *             if security restrictions (either in the system or on the
	 *             cloud queue service) prevented the requests
	 * @throws InterruptedException
	 *             if the thread has been interrupted while waiting for a
	 *             message
	 */
	IMessage consumeMessage(long timeout, TimeUnit unit) throws IllegalArgumentException, IllegalStateException, SecurityException, InterruptedException;

	/**
	 * Deletes a message from the queue.
	 * <p>
	 * This method must be called when a message is still invisible for other
	 * consumers otherwise a message will not be removed from the queue. This
	 * method will return <code>true</code> only upon successful deletion of the
	 * message from the queue. If <code>false</code> is returned it usually
	 * means that the message was not invisible anymore. It's then visible again
	 * to other consumers.
	 * </p>
	 * <p>
	 * If the message has already been deleted a {@link NoSuchElementException}
	 * will be thrown.
	 * </p>
	 * 
	 * @param message
	 *            the message to delete (must be
	 *            {@link #receiveMessages(int, Map) received} previously)
	 * @return <code>true</code> if the message has been deleted,
	 *         <code>false</code> if the message could not be deleted
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 * @throws IllegalStateException
	 *             if the cloud queue service is unavailable
	 * @throws SecurityException
	 *             if security restrictions (either in the system or on the
	 *             cloud queue service) prevented the requests
	 * @throws NoSuchElementException
	 *             if the message does not exist
	 */
	boolean deleteMessage(IMessage message) throws IllegalArgumentException, IllegalStateException, SecurityException, NoSuchElementException;

	/**
	 * Receives one or more available messages from the queue.
	 * <p>
	 * Less messages may be returned than requested. If no messages are
	 * available an empty list will be returned.
	 * </p>
	 * <p>
	 * Any received message is not removed from the queue. However, it will be
	 * hidden to other queue consumers until it will be deleted by this client
	 * or server event happens. In case of a server event the received messages
	 * will be made visible again for other consumers. Such a server event may
	 * be a (configurable) timeout and/or a client disconnect event. Please
	 * refer to the queue service provider documentation for further details.
	 * </p>
	 * <p>
	 * Some queue services may not allow to receive more than one message at a
	 * time.
	 * </p>
	 * 
	 * @param maxNumberOfMessages
	 *            the maximum number of messages to receive (positive integer
	 *            greater than zero)
	 * @param properties
	 *            additional properties for the queue service request
	 * @return list of received messages
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 * @throws IllegalStateException
	 *             if the cloud queue service is unavailable
	 * @throws SecurityException
	 *             if security restrictions (either in the system or on the
	 *             cloud queue service) prevented the requests
	 */
	List<IMessage> receiveMessages(int maxNumberOfMessages, Map<String, ?> properties) throws IllegalArgumentException, IllegalStateException, SecurityException;

	/**
	 * Sends a message to the queue.
	 * <p>
	 * It is highly recommended to keep the message body size small. If you need
	 * to transfer large data consider storing the data in a database or another
	 * system and just send a pointer to the data in the message.
	 * </p>
	 * <p>
	 * When this method returns the message has been successfully submitted to
	 * the queue.
	 * </p>
	 * 
	 * @param messageBody
	 *            the message body (may not be <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 * @throws IllegalStateException
	 *             if the cloud queue service is unavailable
	 * @throws SecurityException
	 *             if security restrictions (either in the system or on the
	 *             cloud queue service) prevented the requests
	 */
	void sendMessage(byte[] messageBody) throws IllegalArgumentException, IllegalStateException, SecurityException;
}
