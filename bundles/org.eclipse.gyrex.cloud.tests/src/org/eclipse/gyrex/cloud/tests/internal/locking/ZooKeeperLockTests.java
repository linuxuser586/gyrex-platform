/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.tests.internal.locking;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import org.eclipse.gyrex.cloud.internal.locking.ZooKeeperLock;

import org.junit.Test;

/**
 *
 */
public class ZooKeeperLockTests {

	private void testRecoverKey(final String lockName, final String nodeContent) {
		final String recoveryKey = ZooKeeperLock.createRecoveryKey(lockName, nodeContent);
		final String[] details = ZooKeeperLock.extractRecoveryKeyDetails(recoveryKey);
		assertNotNull(details);
		assertEquals(2, details.length);
		assertEquals(lockName, details[0]);
		assertEquals(nodeContent, details[1]);
	}

	@Test
	public void testRecoveryKey001() throws Exception {
		testRecoverKey("123", "567");
		testRecoverKey("abc-00002", "casdcöslcda-sdcsad-asdc-sad cascdasc");
		testRecoverKey("def_00394", "cqew098432u92hjc3p98h7fc9wqh4:024d1:0918zfd_9f18");
		testRecoverKey("sbcscacs:!§rf2", "931jcirenvcie_adsv-asdv");
	}
}
