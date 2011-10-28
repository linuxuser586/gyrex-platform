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

import static junit.framework.Assert.fail;

import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.gyrex.cloud.internal.preferences.ZooKeeperBasedPreferences;
import org.eclipse.gyrex.cloud.internal.preferences.ZooKeeperPreferencesService;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

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
	public Preferences node(final String path) {
		if ((path.length() > 0) && (path.charAt(0) == IPath.SEPARATOR)) {
			fail("unable to traverse testable preferences from te ROOT, please fix your test case");
		}
		return super.node(path);
	}

	@Override
	public boolean nodeExists(final String pathName) throws BackingStoreException {
		if ((pathName.length() > 0) && (pathName.charAt(0) == IPath.SEPARATOR)) {
			fail("unable to traverse testable preferences from te ROOT, please fix your test case");
		}
		return super.nodeExists(pathName);
	}

	@Override
	public ConcurrentMap<String, ZooKeeperBasedPreferences> testableGetChildren() {
		return super.testableGetChildren();
	}

	@Override
	public int testableGetChildrenVersion() {
		return super.testableGetChildrenVersion();
	}

	@Override
	public Properties testableGetProperties() {
		return super.testableGetProperties();
	}

	@Override
	public int testableGetPropertiesVersion() {
		return super.testableGetPropertiesVersion();
	}

	@Override
	public String testableGetZooKeeperPath() {
		return super.testableGetZooKeeperPath();
	}

	@Override
	public boolean testableRemoved() {
		return super.testableRemoved();
	}
}
