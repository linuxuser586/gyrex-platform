/*******************************************************************************
 * Copyright (c) 2010, 2012 AGETO Service GmbH and others.
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

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for Gyrex ZooKeeper event handlers
 */
public class ZooKeeperMonitor implements Watcher {

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperMonitor.class);

	/**
	 * A child has been created or removed for the specified path.
	 * 
	 * @param path
	 */
	protected void childrenChanged(final String path) {
		// empty
	}

	/**
	 * The path has been created (it may now exist)
	 * 
	 * @param path
	 */
	protected void pathCreated(final String path) {
		// empty
	}

	/**
	 * The path has been deleted (it may now no longer exist)
	 * 
	 * @param path
	 */
	protected void pathDeleted(final String path) {
		// empty
	}

	@Override
	public void process(final WatchedEvent event) {
		switch (event.getType()) {
			case NodeChildrenChanged:
				childrenChanged(event.getPath());
				break;
			case NodeCreated:
				pathCreated(event.getPath());
				break;
			case NodeDeleted:
				pathDeleted(event.getPath());
				break;
			case NodeDataChanged:
				recordChanged(event.getPath());
				break;
			case None:
				// state of the connection changed
				// TODO: how to handle/consume this event here?
				// our prefered way of working with ZK connect/disconnect/expired events
				// is using the ZKGate which has it's own connect listener (integrated into ZKBasedService)
//			switch (event.getState()) {
//				case SyncConnected:
//					// connection established, nothing to do because watches
//					// will be automatically re-registered and delivered by ZooKeeper
//					connected();
//					break;
//				case Disconnected:
//					// session disconnected, the gate will recover
//					disconnected();
//					break;
//				case Expired:
//					// session expired, the gate will close
//					expired();
//					break;
//			}
				break;
			default:
				LOG.warn("Unhandled event ({}) in ({})", new Object[] { event, this });
				break;
		}
	}

	/**
	 * Data has been updated.
	 * 
	 * @param path
	 */
	protected void recordChanged(final String path) {
		// empty
	}

}
