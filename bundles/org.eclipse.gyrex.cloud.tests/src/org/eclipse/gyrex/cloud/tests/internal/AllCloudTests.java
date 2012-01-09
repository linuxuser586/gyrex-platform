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
package org.eclipse.gyrex.cloud.tests.internal;

import org.eclipse.gyrex.cloud.tests.internal.locking.ZooKeeperLockTestSuite;
import org.eclipse.gyrex.cloud.tests.internal.queue.ZooKeeperQueueTests;
import org.eclipse.gyrex.cloud.tests.internal.zookeeper.preferences.AllZooKeeperPreferencesNonEnsembleTests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ ZooKeeperQueueTests.class, ZooKeeperLockTestSuite.class, AllZooKeeperPreferencesNonEnsembleTests.class })
public class AllCloudTests {

}
