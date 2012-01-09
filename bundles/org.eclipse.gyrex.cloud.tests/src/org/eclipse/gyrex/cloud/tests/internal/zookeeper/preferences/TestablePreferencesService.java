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
package org.eclipse.gyrex.cloud.tests.internal.zookeeper.preferences;

import org.eclipse.gyrex.cloud.internal.preferences.ZooKeeperPreferencesService;

/**
 *
 */
public class TestablePreferencesService extends ZooKeeperPreferencesService {

	/**
	 * Creates a new instance.
	 * 
	 * @param name
	 */
	public TestablePreferencesService(final String name) {
		super(name);
	}

}
