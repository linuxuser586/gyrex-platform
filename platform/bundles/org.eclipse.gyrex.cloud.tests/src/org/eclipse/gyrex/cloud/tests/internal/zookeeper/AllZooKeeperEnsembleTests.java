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
package org.eclipse.gyrex.cloud.tests.internal.zookeeper;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * special suite that tests the cloud connection robustness with ZooKeeper
 */
@RunWith(Suite.class)
@SuiteClasses({ ZooKeeperGateTests.class })
public class AllZooKeeperEnsembleTests {

	@BeforeClass
	public static void setUpClass() throws Exception {
		ZooKeeperEnsembleHelper.startEnsemble();
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		ZooKeeperEnsembleHelper.stopEnsemble();
	}

}
