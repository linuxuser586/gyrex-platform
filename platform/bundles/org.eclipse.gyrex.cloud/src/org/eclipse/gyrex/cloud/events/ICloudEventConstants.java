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
package org.eclipse.gyrex.cloud.events;

import java.util.Set;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Interface with public constants relevant for cloud events.
 * <p>
 * Cloud events are delivered through the OSGi {@link EventAdmin}. This allows
 * interesting clients to participate in cloud events.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ICloudEventConstants {

	/**
	 * Event property identifying the node id.
	 * <p>
	 * This property may be supplied in the {@code properties}
	 * {@code Dictionary} or {@code Map} passed to an {@link Event}. The value
	 * of this property must be of type {@link String}.
	 * </p>
	 */
	String NODE_ID = "node.id";

	/**
	 * Event property identifying the node tags.
	 * <p>
	 * This property may be supplied in the {@code properties}
	 * {@code Dictionary} or {@code Map} passed to an {@link Event}. The value
	 * of this property must be of type {@link Set} of {@link String}.
	 * </p>
	 */
	String NODE_TAGS = "node.tags";

	/**
	 * Topic under which an event is published when <em>this</em> node became
	 * <strong>online</strong> (value
	 * <code>org/eclipse/gyrex/cloud/node/online</code>).
	 * <p>
	 * Note, this event may be triggered multiple times while online. There is
	 * also no guarantee that an {@link #TOPIC_NODE_OFFLINE OFFLINE} event is
	 * preceeding each {@link #TOPIC_NODE_ONLINE ONLINE} event.
	 * </p>
	 */
	String TOPIC_NODE_ONLINE = "org/eclipse/gyrex/cloud/node/online";

	/**
	 * Topic under which an event is published when <em>this</em> node became
	 * <strong>interrupted</strong> (value
	 * <code>org/eclipse/gyrex/cloud/node/interrupted</code>).
	 * <p>
	 * Note, this event may be triggered multiple times while interrupted. There
	 * is also no guarantee that an {@link #TOPIC_NODE_ONLINE ONLINE} event is
	 * preceeding each {@link #TOPIC_NODE_INTERRUPTED INTERRUPTED} event.
	 * </p>
	 */
	String TOPIC_NODE_INTERRUPTED = "org/eclipse/gyrex/cloud/node/interrupted";

	/**
	 * Topic under which an event is published when <em>this</em> node became
	 * offline (value <code>org/eclipse/gyrex/cloud/node/offline</code>).
	 * <p>
	 * Note, this event may be triggered multiple times while offline. There is
	 * also no guarantee that an {@link #TOPIC_NODE_ONLINE ONLINE} event is
	 * preceeding an {@link #TOPIC_NODE_OFFLINE OFFLINE} event.
	 * </p>
	 */
	String TOPIC_NODE_OFFLINE = "org/eclipse/gyrex/cloud/node/offline";

}
