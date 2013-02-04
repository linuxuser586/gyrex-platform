/**
 * Copyright (c) 2011, 2012 Gunnar Wagenknecht and others.
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

import static junit.framework.Assert.assertNotNull;

import org.eclipse.gyrex.context.tests.internal.BaseContextTest;
import org.eclipse.gyrex.jobs.manager.IJobManager;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.junit.Test;

/**
 *
 */
@SuppressWarnings("restriction")
public class JobHungDetectionTests extends BaseContextTest {

	@Override
	protected IPath getPrimaryTestContextPath() {
		return Path.ROOT;
	}

	@Test
	public void test() {
		// create jobs
		final IJobManager jobManager = getContext().get(IJobManager.class);
		assertNotNull(jobManager);

	}

}
