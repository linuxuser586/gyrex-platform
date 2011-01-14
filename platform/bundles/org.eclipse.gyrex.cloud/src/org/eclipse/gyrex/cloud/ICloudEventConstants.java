/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud;

import org.osgi.service.event.EventAdmin;

/**
 * Interface with public constants relevant for cloud events.
 * <p>
 * Cloud events are delivered through the OSGi {@link EventAdmin}. This allows
 * interesting clients to participate in cloud events.
 * </p>
 */
public interface ICloudEventConstants {

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
	 * offline (value <code>org/eclipse/gyrex/cloud/node/offline</code>).
	 * <p>
	 * Note, this event may be triggered multiple times while offline. There is
	 * also no guarantee that an {@link #TOPIC_NODE_ONLINE ONLINE} event is
	 * preceeding an {@link #TOPIC_NODE_OFFLINE OFFLINE} event.
	 * </p>
	 */
	String TOPIC_NODE_OFFLINE = "org/eclipse/gyrex/cloud/node/offline";

}
