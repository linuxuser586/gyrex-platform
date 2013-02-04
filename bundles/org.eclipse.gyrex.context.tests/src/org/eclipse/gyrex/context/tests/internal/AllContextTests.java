/*******************************************************************************
 * Copyright (c) 2010, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context.tests.internal;

import org.eclipse.gyrex.context.tests.internal.injection.ContextInjectionTests;
import org.eclipse.gyrex.junit.GyrexServerResource;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Context Test Suite.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ ContextRegistryTests.class, ContextualRuntimeBlackBoxTests.class, PreferencesBlackBoxTests.class, ContextInjectionTests.class })
public class AllContextTests {

	@ClassRule
	public static final GyrexServerResource server = new GyrexServerResource();

}
