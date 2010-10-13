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
package org.eclipse.gyrex.model.common.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;

import org.eclipse.osgi.util.NLS;

import org.osgi.framework.BundleContext;

/**
 * The core model plug-in.
 */
public class ModelActivator extends BaseBundleActivator {

	/** PLUGIN_ID */
	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.model.common";

	/** the shared instance */
	private static final AtomicReference<ModelActivator> sharedInstance = new AtomicReference<ModelActivator>();

	/**
	 * Returns the shared instance.
	 * <p>
	 * A <code>{@link IllegalStateException}</code> will be thrown if the bundle
	 * has not been started.
	 * </p>
	 * 
	 * @return the shared instance
	 * @throws IllegalStateException
	 *             if the bundle has not been started
	 */
	public static ModelActivator getInstance() {
		final ModelActivator activator = sharedInstance.get();
		if (null == activator) {
			throw new IllegalStateException(NLS.bind("Bundle {0} has not been started.", SYMBOLIC_NAME));
		}

		return activator;
	}

	/**
	 * Creates a new instance.
	 * <p>
	 * Note, this is called by the OSGi platform. <b>Clients should never call
	 * this method.</b>
	 * </p>
	 */
	public ModelActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		sharedInstance.set(this);
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		sharedInstance.set(null);
	}
}
