/*******************************************************************************
 * Copyright (c) 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.junit.internal;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;

import org.osgi.framework.BundleContext;

public class JUnitActivator extends BaseBundleActivator {

	private static final String SYMBOLIC_NAME = "org.eclipse.gyrex.junit";

	private static volatile JUnitActivator instance;

	public static JUnitActivator getInstance() {
		final JUnitActivator activator = instance;
		if (activator == null)
			throw new IllegalStateException("JUnit helper bundle not active");
		return activator;
	}

	private GyrexStarter starter;

	public JUnitActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instance = this;
		starter = new GyrexStarter(context);
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instance = null;
		starter.dispose();
		starter = null;
	}

	public GyrexStarter getGyrexStarter() {
		return starter;
	}
}
