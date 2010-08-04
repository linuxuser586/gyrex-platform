/*******************************************************************************
 * Copyright (c) 2010 AGETO and others.
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

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.osgi.framework.BundleContext;

public class Activator extends BaseBundleActivator {

	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.context.tests";

	private static Activator activator;

	/**
	 * Returns the activator.
	 * 
	 * @return the activator
	 */
	public static Activator getActivator() {
		final Activator activator = Activator.activator;
		if (null == activator) {
			throw new IllegalStateException("inactive");
		}
		return activator;
	}

	private BundleContext context;

	public Activator() {
		super(SYMBOLIC_NAME);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStart(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStart(final BundleContext context) throws Exception {
		activator = this;
		this.context = context;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStop(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStop(final BundleContext context) throws Exception {
		activator = null;
		this.context = null;
	}

	/**
	 * Returns the context.
	 * 
	 * @return the context
	 */
	public BundleContext getContext() {
		final BundleContext context = this.context;
		if (null == context) {
			throw new IllegalStateException("inactive");
		}

		return context;
	}
}
