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
package org.eclipse.gyrex.cloud.tests.internal.zookeeper;

import org.apache.zookeeper.ZKTestCase;
import org.junit.After;
import org.junit.Before;

/**
 * Base class for all ensemble tests
 */
public class BaseEnsambleTest extends ZKTestCase {

	protected static final int CONNECT_TIMEOUT = 10000;

	@Before
	public void setUp() throws Exception {
		EnsembleHelper.assertRunningAndConnected();
	}

	@After
	public void tearDown() throws Exception {
	}

}
