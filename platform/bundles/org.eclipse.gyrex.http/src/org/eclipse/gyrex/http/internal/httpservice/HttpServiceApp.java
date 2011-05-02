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
package org.eclipse.gyrex.http.internal.httpservice;

import org.eclipse.equinox.http.servlet.ExtendedHttpService;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.internal.HttpActivator;

import org.eclipse.core.runtime.CoreException;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

/**
 * The HttpService application.
 */
public class HttpServiceApp extends Application implements ServiceFactory<HttpService> {

	private ServiceRegistration serviceRegistration;

	/**
	 * Creates a new instance.
	 * 
	 * @param id
	 * @param context
	 */
	public HttpServiceApp(final String id, final IRuntimeContext context) {
		super(id, context);
	}

	@Override
	protected void doDestroy() {
		serviceRegistration.unregister();
		serviceRegistration = null;
	}

	@Override
	protected void doInit() throws CoreException {
		serviceRegistration = HttpActivator.getInstance().getServiceHelper().registerService(new String[] { HttpService.class.getName(), ExtendedHttpService.class.getName() }, this, "Eclipse Gyrex", "Gyrex web application based OSGi HttpService", HttpActivator.SYMBOLIC_NAME.concat(".service-").concat(getId()), null);
	}

	@Override
	public HttpService getService(final Bundle bundle, final ServiceRegistration<HttpService> registration) {
		return new HttpServiceImpl(getApplicationContext(), bundle);
	}

	@Override
	public void ungetService(final Bundle bundle, final ServiceRegistration<HttpService> registration, final HttpService service) {
		((HttpServiceImpl) service).unregisterAll();
	}
}
