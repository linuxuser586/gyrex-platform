/*******************************************************************************
 * Copyright (c) 2010, 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.preferences.internal;

import org.eclipse.gyrex.cloud.internal.preferences.ZooKeeperBasedPreferences;
import org.eclipse.gyrex.cloud.internal.preferences.ZooKeeperPreferencesService;
import org.eclipse.gyrex.preferences.ModificationConflictException;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.zookeeper.KeeperException.BadVersionException;
import org.apache.zookeeper.KeeperException.NoNodeException;

/**
 * ZooKeeper based preferences.
 */
public class CloudPreferences extends ZooKeeperBasedPreferences {

	/**
	 * Creates a new instance.
	 * 
	 * @param parent
	 * @param name
	 * @param service
	 * @param zooKeeperParentPath
	 */
	public CloudPreferences(final IEclipsePreferences parent, final String name, final ZooKeeperPreferencesService service) {
		super(parent, name, service);
	}

	@Override
	protected BackingStoreException createBackingStoreException(final String action, final Exception cause) {
		// ZooKeeper bad version
		if (cause instanceof BadVersionException)
			return new ModificationConflictException(String.format("Concurrent modification error %s (node %s). %s", action, absolutePath(), null != cause.getMessage() ? cause.getMessage() : ExceptionUtils.getMessage(cause)), cause);

		// ZooKeeper NoNode usually also means a concurrent modification
		if (cause instanceof NoNodeException)
			return new ModificationConflictException(String.format("Concurrent modification error %s (node %s). %s", action, absolutePath(), null != cause.getMessage() ? cause.getMessage() : ExceptionUtils.getMessage(cause)), cause);

		// nested exceptions are ugly but may happen
		if (cause instanceof ModificationConflictException)
			return new ModificationConflictException(String.format("Concurrent modification error %s (node %s). %s", action, absolutePath(), null != cause.getMessage() ? cause.getMessage() : ExceptionUtils.getMessage(cause)), cause);

		// default
		return super.createBackingStoreException(action, cause);
	}

	@Override
	protected ZooKeeperBasedPreferences newChild(final String name) {
		return new CloudPreferences(this, name, getService());
	}

}
