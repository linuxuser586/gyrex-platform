/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.services.common.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.osgi.framework.BundleContext;

public class ServicesActivator extends BaseBundleActivator {

	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.services.common";

	private static final AtomicReference<ServicesActivator> instance = new AtomicReference<ServicesActivator>();

	/**
	 * Returns the shared instance.
	 * 
	 * @return the instance
	 */
	public static ServicesActivator getInstance() {
		final ServicesActivator activator = instance.get();
		if (null == activator) {
			throw new IllegalStateException("inactive");
		}
		return activator;
	}

	/**
	 * Creates a new instance (called by the OSGi framework).
	 */
	public ServicesActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instance.set(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.runtime.BaseBundleActivator#doStop(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instance.set(null);
	}
}
