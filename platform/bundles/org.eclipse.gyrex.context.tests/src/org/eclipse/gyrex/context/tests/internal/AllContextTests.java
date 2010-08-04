/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Context Test Suite.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ ContextualRuntimeBlackBoxTests.class, PreferencesBlackBoxTests.class })
public class AllContextTests {

}
