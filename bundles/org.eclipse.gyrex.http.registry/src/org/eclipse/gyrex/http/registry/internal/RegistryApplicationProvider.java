/**
 * Copyright (c) 2008, 2009 AGETO Service GmbH and others.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.provider.ApplicationProvider;

/**
 * 
 */
public class RegistryApplicationProvider extends ApplicationProvider {

	public static final String ID = "org.eclipse.gyrex.http.registry.application";
	private static final AtomicReference<RegistryApplicationProvider> instanceRef = new AtomicReference<RegistryApplicationProvider>();

	/**
	 * Returns the instance.
	 * 
	 * @return the instance
	 */
	public static RegistryApplicationProvider getInstance() {
		final RegistryApplicationProvider instance = instanceRef.get();
		if (null != instance) {
			return instance;
		}
		instanceRef.compareAndSet(null, new RegistryApplicationProvider());
		return instanceRef.get();
	}

	private final Map<String, RegistryApplication> activeApplicationsById = new ConcurrentHashMap<String, RegistryApplication>();
	private ApplicationRegistryManager applicationRegistryManager;

	/**
	 * Creates a new instance.
	 * 
	 * @param id
	 */
	private RegistryApplicationProvider() {
		super(ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.http.application.provider.ApplicationProvider#createApplication(java.lang.String, org.eclipse.gyrex.context.IRuntimeContext)
	 */
	@Override
	public Application createApplication(final String applicationId, final IRuntimeContext context) throws CoreException {
		final RegistryApplication application = new RegistryApplication(applicationId, context);
		activeApplicationsById.put(applicationId, application);
		return application;
	}

	public synchronized void initApplication(final RegistryApplication registryApplication) {
		if (null != applicationRegistryManager) {
			applicationRegistryManager.initApplication(registryApplication);
		}
	}

	public synchronized void removeApplication(final String id) {
		final RegistryApplication registryApplication = activeApplicationsById.remove(id);
		if ((null != applicationRegistryManager) && (null != registryApplication)) {
			applicationRegistryManager.closeApplication(registryApplication);
		}
	}

	public void setManager(final ApplicationRegistryManager applicationRegistryManager) {
		this.applicationRegistryManager = applicationRegistryManager;
	}

}
