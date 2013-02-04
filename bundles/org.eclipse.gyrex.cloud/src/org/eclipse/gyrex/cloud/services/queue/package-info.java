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

/**
 * A simple and lightweight cloud queue service.
 * <p>
 * This services allows create queues and submit messages to and retrieve
 * messages from them. No strong guarantees are defined regarding the actual
 * implementation which allows a broad set of possible implementations (eg.
 * Amazon SQS, RabbitMQ, OpenStack, etc.).
 * </p>
 * <p>
 * Clients which do not rely on any specific behavior implied by a specific
 * implementation (eg. FIFO) and just use this API as is will be able to
 * implement once and run in any cloud which offers a suitable implementation.
 * </p>
 * <p>
 * At a minimum API the following operations are offered by this package.
 * </p>
 * <ul>
 * <li>Ensure that a queue exists (creates it if necessary).</li>
 * <li>Publish a message (set of bytes) to any queue.</li>
 * <li>Receive a message from a queue.</li>
 * <li>Delete a message from a queue.</li>
 * </ul>
 * <p>
 * Although the API offers basic methods for queue management, an actual
 * implementation may offer an advanced queue management interface.
 * </p>
 * <p>
 * No guarantees are given regarding message delivery/ordering other than the
 * guarantees given by the underlying implementations.
 * </p>
 * <p>
 * This queue service is <em>not</em> a distributed event bus.
 * </p>
 * <p>
 * This package contains a lot of API which must be implemented by contributors of
 * a service implementation. Therefore, this API is considered part of a service provider
 * API which may evolve faster than the general API. Please get in touch with the
 * development team through the prefered channels listed on <a
 * href="http://www.eclipse.org/gyrex">the Gyrex website</a> to stay up-to-date
 * of possible changes.
 * </p>
 */
package org.eclipse.gyrex.cloud.services.queue;

