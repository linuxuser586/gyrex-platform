/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.equinoxhttpservice.internal;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;

import org.osgi.framework.BundleContext;

public class HttpServiceActivator extends BaseBundleActivator {

	/** bundle symbolic name */
	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.http.equinoxhttpservice";

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	/**
	 * Creates a new instance.
	 */
	public HttpServiceActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		HttpServiceActivator.context = context;
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		HttpServiceActivator.context = null;
	}

}
