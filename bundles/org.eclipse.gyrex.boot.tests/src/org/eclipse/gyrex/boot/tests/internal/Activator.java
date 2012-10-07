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
package org.eclipse.gyrex.boot.tests.internal;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;

import org.osgi.framework.BundleContext;

public class Activator extends BaseBundleActivator {

	private static final String SYMBOLIC_NAME = "org.eclipse.gyrex.boot.tests";
	private static volatile Activator instance;

	/**
	 * Returns the instance.
	 * 
	 * @return the instance
	 */
	public static Activator getInstance() {
		final Activator activator = instance;
		if (activator == null)
			throw new IllegalStateException("inactive");
		return activator;
	}

	/**
	 * Creates a new instance.
	 */
	public Activator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instance = this;
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instance = null;
	}

}
