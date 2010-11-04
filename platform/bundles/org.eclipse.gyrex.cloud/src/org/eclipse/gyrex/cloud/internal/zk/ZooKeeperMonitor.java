/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
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

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

/**
 * Base class for Gyrex ZooKeeper event handlers
 */
public class ZooKeeperMonitor implements Watcher {

	/**
	 * A child has been created for the specified path.
	 * 
	 * @param path
	 */
	protected void childCreated(final String path) {
		// empty
	}

	/**
	 * Called when the gate is closing.
	 * 
	 * @param reason
	 */
	protected void closing(final Code reason) {
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
		if (event.getType() == Event.EventType.None) {
			// state of the connection changed
			switch (event.getState()) {
				case SyncConnected:
					// connection established, nothing to do because watches
					// will be automatically re-registered and delivered by ZooKeeper
					break;
				case Expired:
					// session expired, the gate will close
					closing(KeeperException.Code.SESSIONEXPIRED);
					break;
			}
		} else {
			final String path = event.getPath();
			if (path != null) {
				switch (event.getType()) {
					case NodeChildrenChanged:
						childCreated(path);
						break;
					case NodeCreated:
						pathCreated(path);
						break;
					case NodeDeleted:
						pathDeleted(path);
						break;
					case NodeDataChanged:
						recordChanged(path);
						break;
				}
			}
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
