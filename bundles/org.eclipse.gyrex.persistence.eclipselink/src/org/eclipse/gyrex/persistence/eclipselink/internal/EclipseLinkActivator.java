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
package org.eclipse.gyrex.persistence.eclipselink.internal;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class EclipseLinkActivator extends BaseBundleActivator {

	/** symbolic name */
	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.persistence.eclipselink";

	private static volatile EclipseLinkActivator instance;

	/**
	 * Returns the instance.
	 * 
	 * @return the instance
	 */
	public static EclipseLinkActivator getInstance() {
		final EclipseLinkActivator activator = instance;
		if (null == activator) {
			throw new IllegalStateException(String.format("bundle %s is inactive", SYMBOLIC_NAME));
		}
		return activator;
	}

	/**
	 * Creates a new instance.
	 */
	public EclipseLinkActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instance = this;

		// make sure the Gemini JPA bundle is started
		final Bundle[] bundles = context.getBundles();
		for (final Bundle b : bundles) {
			if (b.getSymbolicName().equals("org.eclipse.gemini.jpa")) {
				if (b.getState() != Bundle.ACTIVE) {
					b.start(Bundle.START_TRANSIENT);
				}
			}
		}
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instance = null;
	}

}
