/*******************************************************************************
 * Copyright (c) 2008, 2012 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.tests.internal;

import static junit.framework.Assert.assertNotNull;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;

import org.osgi.framework.BundleContext;

public class PersistenceTestsActivator extends BaseBundleActivator {

	private static BundleContext context;

	private static PersistenceTestsActivator instance;

	public static BundleContext getContext() {
		final BundleContext bundleContext = context;
		assertNotNull(bundleContext);
		return bundleContext;
	}

	public static PersistenceTestsActivator getInstance() {
		final PersistenceTestsActivator activator = instance;
		assertNotNull(activator);
		return activator;
	}

	public PersistenceTestsActivator() {
		super("org.eclipse.gyrex.persistence.tests");
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instance = this;
		PersistenceTestsActivator.context = context;
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instance = null;
		PersistenceTestsActivator.context = null;
	}
}
