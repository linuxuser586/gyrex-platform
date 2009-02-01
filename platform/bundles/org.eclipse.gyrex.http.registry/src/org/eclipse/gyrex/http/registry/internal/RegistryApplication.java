/**
 * Copyright (c) 2008 AGETO Service GmbH and others.
 * All rights reserved. 
 * 
 * This program and the accompanying materials are made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.cloudfree.http.registry.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.cloudfree.common.context.IContext;
import org.eclipse.cloudfree.http.application.Application;
import org.eclipse.cloudfree.http.application.servicesupport.IApplicationServiceSupport;
import org.eclipse.cloudfree.http.registry.ApplicationCustomizer;
import org.eclipse.core.runtime.CoreException;

/**
 * 
 */
public class RegistryApplication extends Application {

	private final AtomicReference<ApplicationCustomizer> customizerRef = new AtomicReference<ApplicationCustomizer>();

	RegistryApplication(final String id, final IContext context) {
		super(id, context);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.http.application.Application#doDestroy()
	 */
	@Override
	protected void doDestroy() {
		RegistryApplicationProvider.getInstance().removeApplication(getId());

		// call customizer after regular destroy
		try {
			final ApplicationCustomizer customizer = getCustomizer();
			if (null != customizer) {
				customizer.onDestroy(this);
			}
		} finally {
			setCustomizer(null);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.http.application.Application#doInit()
	 */
	@Override
	protected void doInit() throws CoreException {
		RegistryApplicationProvider.getInstance().initApplication(this);

		// call customizer after initialization
		final ApplicationCustomizer customizer = getCustomizer();
		if (null != customizer) {
			customizer.onInit(this);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.http.application.Application#getApplicationServiceSupport()
	 */
	@Override
	protected IApplicationServiceSupport getApplicationServiceSupport() {
		return super.getApplicationServiceSupport();
	}

	/**
	 * Returns the customizer
	 * 
	 * @return the customizer (may be <code>null</code>)
	 */
	ApplicationCustomizer getCustomizer() {
		return customizerRef.get();
	}

	/**
	 * Sets the customizer
	 * 
	 * @param customizer
	 *            the customizer to set (or <code>null</code> to unset)
	 */
	void setCustomizer(final ApplicationCustomizer customizer) {
		customizerRef.set(customizer);
	}
}
