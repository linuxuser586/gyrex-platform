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
package org.eclipse.gyrex.cloud.internal.preferences;

import org.eclipse.gyrex.cloud.internal.zk.IZooKeeperLayout;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

/**
 *
 */
public class NodePreferences extends ZooKeeperBasedPreferences {

	/**
	 * Creates a new instance.
	 * 
	 * @param parent
	 * @param name
	 * @param zooKeeperParentPath
	 */
	public NodePreferences(final IEclipsePreferences parent, final String name) {
		super(parent, name, IZooKeeperLayout.PATH_PREFERENCES_ROOT);
	}

	@Override
	protected ZooKeeperBasedPreferences newChild(final String name) {
		return new NodePreferences(this, name);
	}

}
