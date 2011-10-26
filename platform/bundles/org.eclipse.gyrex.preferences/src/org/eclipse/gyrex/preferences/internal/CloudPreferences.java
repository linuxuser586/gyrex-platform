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

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

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
	protected ZooKeeperBasedPreferences newChild(final String name) {
		return new CloudPreferences(this, name, getService());
	}

}
