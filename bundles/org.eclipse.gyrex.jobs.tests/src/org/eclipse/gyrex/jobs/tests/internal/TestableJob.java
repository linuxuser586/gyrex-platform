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
package org.eclipse.gyrex.jobs.tests.internal;

import org.eclipse.gyrex.jobs.IJobContext;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 *
 */
public class TestableJob extends Job {

	private final IJobContext context;

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 * @param name
	 */
	public TestableJob(final IJobContext context) {
		super("Test Job");
		this.context = context;
	}

	/**
	 * Returns the context.
	 * 
	 * @return the context
	 */
	public IJobContext getContext() {
		return context;
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

}
