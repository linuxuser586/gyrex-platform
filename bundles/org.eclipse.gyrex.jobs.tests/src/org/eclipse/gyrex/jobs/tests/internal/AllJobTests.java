/**
 * Copyright (c) 2011, 2013 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.jobs.tests.internal;

import org.eclipse.gyrex.jobs.tests.internal.storage.CloudHistoryStorageTest;
import org.eclipse.gyrex.jobs.tests.internal.storage.MockStorageTest;
import org.eclipse.gyrex.junit.GyrexServerResource;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ JobManagerBlackBoxTests.class, JobHungDetectionTests.class, MockStorageTest.class, CloudHistoryStorageTest.class })
public class AllJobTests {
	@ClassRule
	public static final GyrexServerResource server = new GyrexServerResource();

}
