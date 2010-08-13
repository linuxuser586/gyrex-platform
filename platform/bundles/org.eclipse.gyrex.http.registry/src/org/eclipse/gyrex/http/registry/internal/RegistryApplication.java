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
package org.eclipse.gyrex.http.registry.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.context.IApplicationContext;
import org.eclipse.gyrex.http.registry.ApplicationCustomizer;

import org.eclipse.core.runtime.CoreException;

/**
 * Lazy application instance contributed via extension registry.
 */
public class RegistryApplication extends Application {

	private final AtomicReference<ApplicationCustomizer> customizerRef = new AtomicReference<ApplicationCustomizer>();

	RegistryApplication(final String id, final IRuntimeContext context) {
		super(id, context);
	}

	@Override
	protected void doDestroy() {
		// notify provider
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

	@Override
	protected void doInit() throws CoreException {
		// notify provider
		RegistryApplicationProvider.getInstance().initApplication(this);

		// call customizer after initialization
		final ApplicationCustomizer customizer = getCustomizer();
		if (null != customizer) {
			customizer.onInit(this);
		}
	}

	@Override
	protected IApplicationContext getApplicationServiceSupport() {
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
