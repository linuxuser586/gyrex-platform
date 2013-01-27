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
package org.eclipse.gyrex.preferences.tests.internal;

import org.eclipse.gyrex.junit.GyrexServerResource;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Context Test Suite.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ CloudPreferencesBlackBoxTests.class })
public class AllPreferencesTests {
	@ClassRule
	public static final GyrexServerResource server = new GyrexServerResource();
}
