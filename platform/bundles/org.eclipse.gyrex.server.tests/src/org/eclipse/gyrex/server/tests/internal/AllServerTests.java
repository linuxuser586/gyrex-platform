/**
 * Copyright (c) 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.server.tests.internal;

import org.eclipse.gyrex.cloud.tests.internal.AllCloudTests;
import org.eclipse.gyrex.context.tests.internal.AllContextTests;
import org.eclipse.gyrex.search.solr.tests.AllSolrCdsTests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 *
 */
@RunWith(Suite.class)
@SuiteClasses({ AllCloudTests.class, AllContextTests.class, AllSolrCdsTests.class })
public class AllServerTests {

}
