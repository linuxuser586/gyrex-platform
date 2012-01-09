/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.tests.internal;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;

import org.osgi.framework.BundleContext;

public class CloudTestsActivator extends BaseBundleActivator {

	private static final String SYMBOLIC_NAME = "org.eclipse.gyrex.cloud.tests";

	private static volatile CloudTestsActivator instance;

	/**
	 * Returns the instance.
	 *
	 * @return the instance
	 */
	public static CloudTestsActivator getInstance() {
		final CloudTestsActivator activator = instance;
		if (activator == null) {
			throw new IllegalStateException("inactive");
		}
		return activator;
	}

	public CloudTestsActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instance = this;
	};

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instance = null;
	}
}
