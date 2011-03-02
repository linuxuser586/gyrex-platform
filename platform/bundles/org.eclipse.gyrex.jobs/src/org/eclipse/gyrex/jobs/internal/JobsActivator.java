/**
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.jobs.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;

import org.osgi.framework.BundleContext;

/**
 * Bundle activator.
 */
public class JobsActivator extends BaseBundleActivator {

	/** SYMBOLIC_NAME */
	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.jobs";
	private static final AtomicReference<JobsActivator> instanceRef = new AtomicReference<JobsActivator>();

	/**
	 * Creates a new instance.
	 */
	public JobsActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instanceRef.set(this);
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instanceRef.set(null);
	}

	@Override
	protected Class getDebugOptions() {
		return JobsDebug.class;
	}
}
