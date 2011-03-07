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

import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.gyrex.common.identifiers.IdHelper;

/**
 * A simple queue service.
 * <p>
 * A queue service allows to create queues, submit messages to and retrieve
 * messages from them. It does not define any strong guarantees about typical
 * queue characteristics (ordering, reliability, etc.) but an underlying
 * implementation may do so.
 * </p>
 * <p>
 * In general, clients using a queue service should have a few things in mind.
 * <ul>
 * <li>Due to the nature of cloud services and especially distributed queues
 * it's not easy for queue service implementors to guarantee a FIFO behavior.
 * Therefore, FIFO behavior must not be relied upon when working with queues.
 * <li>
 * <li>Some cloud queue service might store copies of your messages on multiple
 * servers. It may happen that a server dies right when sending a message. When
 * it it is restored it may send the message again. Therefore, an application
 * must be idempotent regarding to message retrieval, i.e. it must be prepared
 * to handle the same message twice.</li>
 * </ul>
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
public interface IQueueService {

	/**
	 * Creates a queue with the specified id.
	 * <p>
	 * The properties may be used to configure certain queue capabilities and/or
	 * to provide authentication information. Please check the underlying queue
	 * service documentation what capabilities are available.
	 * </p>
	 * 
	 * @param id
	 *            the queue id (must be a valid
	 *            {@link IdHelper#isValidId(String) API id})
	 * @param properties
	 *            the create queue request properties (maybe <code>null</code>)
	 * @return a handle to the created queue
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 * @throws IllegalStateException
	 *             if the cloud queue service is unavailable
	 * @throws SecurityException
	 *             if security restrictions (either in the system or on the
	 *             cloud queue service) prevented the requests
	 * @throws DuplicateQueueException
	 *             if a queue with the same id already exists
	 */
	IQueue createQueue(String id, Map<String, ?> properties) throws IllegalArgumentException, IllegalStateException, SecurityException, DuplicateQueueException;

	/**
	 * Deletes a queue with the specified id, regardless of the queue is empty.
	 * <p>
	 * The properties may be used to provide authentication information.
	 * </p>
	 * 
	 * @param id
	 *            the queue id (must be a valid
	 *            {@link IdHelper#isValidId(String) API id})
	 * @param properties
	 *            the remove queue request properties (maybe <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 * @throws IllegalStateException
	 *             if the cloud queue service is unavailable
	 * @throws SecurityException
	 *             if security restrictions (either in the system or on the
	 *             cloud queue service) prevented the requests
	 * @throws NoSuchElementException
	 *             if a queue of the specified id does not exist
	 */
	void deleteQueue(String id, Map<String, ?> properties) throws IllegalArgumentException, IllegalStateException, SecurityException, NoSuchElementException;

	/**
	 * Returns a queue with the specified id.
	 * <p>
	 * The properties may be used to provide authentication information.
	 * </p>
	 * 
	 * @param id
	 *            the queue id (must be a valid
	 *            {@link IdHelper#isValidId(String) API id})
	 * @param properties
	 *            the get queue request properties (maybe <code>null</code>)
	 * @return a handle to the queue (maybe <code>null</code> if a queue of the
	 *         specified id does not exist)
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 * @throws IllegalStateException
	 *             if the cloud queue service is unavailable
	 * @throws SecurityException
	 *             if security restrictions (either in the system or on the
	 *             cloud queue service) prevented the requests
	 */
	IQueue getQueue(String id, Map<String, ?> properties) throws IllegalArgumentException, IllegalStateException, SecurityException;

	/**
	 * Updates a queue with the specified id.
	 * <p>
	 * The properties may be used to configure certain queue capabilities and/or
	 * to provide authentication information. Please check the underlying queue
	 * service documentation what capabilities are available.
	 * </p>
	 * 
	 * @param id
	 *            the queue id (must be a valid
	 *            {@link IdHelper#isValidId(String) API id})
	 * @param properties
	 *            the update queue request properties (maybe <code>null</code>)
	 * @return a handle to the updated queue
	 * @throws IllegalArgumentException
	 *             if any of the arguments is invalid
	 * @throws IllegalStateException
	 *             if the cloud queue service is unavailable
	 * @throws SecurityException
	 *             if security restrictions (either in the system or on the
	 *             cloud queue service) prevented the requests
	 * @throws NoSuchElementException
	 *             if a queue of the specified id does not exist
	 */
	IQueue updateQueue(String id, Map<String, ?> properties) throws IllegalArgumentException, IllegalStateException, SecurityException, NoSuchElementException;
}
