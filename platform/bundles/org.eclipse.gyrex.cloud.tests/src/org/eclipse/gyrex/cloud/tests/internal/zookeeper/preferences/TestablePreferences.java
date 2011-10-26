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
package org.eclipse.gyrex.cloud.tests.internal.zookeeper.preferences;

import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.gyrex.cloud.internal.preferences.ZooKeeperBasedPreferences;
import org.eclipse.gyrex.cloud.internal.preferences.ZooKeeperPreferencesService;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;

/**
 *
 */
public class TestablePreferences extends ZooKeeperBasedPreferences {

	/**
	 * Creates a new instance.
	 * 
	 * @param parent
	 * @param name
	 * @param service
	 */
	public TestablePreferences(final IEclipsePreferences parent, final String name, final ZooKeeperPreferencesService service) {
		super(parent, name, service);
	}

	@Override
	protected ZooKeeperBasedPreferences newChild(final String name) {
		return new TestablePreferences(this, name, getService());
	}

	@Override
	protected ConcurrentMap<String, ZooKeeperBasedPreferences> testableGetChildren() {
		return super.testableGetChildren();
	}

	@Override
	protected int testableGetChildrenVersion() {
		return super.testableGetChildrenVersion();
	}

	@Override
	protected Properties testableGetProperties() {
		return super.testableGetProperties();
	}

	@Override
	protected int testableGetPropertiesVersion() {
		return super.testableGetPropertiesVersion();
	}

	@Override
	public String testableGetZooKeeperPath() {
		return super.testableGetZooKeeperPath();
	}
}
