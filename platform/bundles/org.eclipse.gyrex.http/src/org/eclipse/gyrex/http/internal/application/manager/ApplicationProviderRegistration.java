/*******************************************************************************
 * Copyright (c) 2008, 2009 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.internal.application.manager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.provider.ApplicationProvider;
import org.eclipse.gyrex.http.internal.HttpActivator;
import org.osgi.framework.ServiceReference;

/**
 * A registered {@link ApplicationProvider}
 */
public class ApplicationProviderRegistration {

	private final AtomicReference<ApplicationProvider> provider;
	private final List<ApplicationRegistration> activeApplications = new CopyOnWriteArrayList<ApplicationRegistration>();

	/**
	 * Creates a new instance.
	 * 
	 * @param reference
	 * @param provider
	 */
	public ApplicationProviderRegistration(final ServiceReference reference, final ApplicationProvider provider) {
		this.provider = new AtomicReference<ApplicationProvider>(provider);
	}

	/**
	 * Creates a new application for the specified registration.
	 * 
	 * @param applicationRegistration
	 * @return a new application instance or <code>null</code> if the provider
	 *         has been destroyed
	 * @throws CoreException
	 */
	Application createApplication(final ApplicationRegistration applicationRegistration) throws CoreException {
		final ApplicationProvider provider = this.provider.get();
		if (null == provider) {
			// destroyed
			return null;
		}
		final Application application = provider.createApplication(applicationRegistration.getApplicationId(), applicationRegistration.getContext());
		if (null == application) {
			HttpActivator.getInstance().getStatusUtil().createError(0, "Provider '" + provider.getId() + "' did not return an application instance", null);
		}
		activeApplications.add(applicationRegistration);
		return application;
	}

	/**
	 * Destroys the provider instance and all created application instances.
	 */
	void destroy() {
		provider.set(null);
		for (final ApplicationRegistration applicationRegistration : activeApplications) {
			applicationRegistration.destroy();
		}
		activeApplications.clear();
	}
}
