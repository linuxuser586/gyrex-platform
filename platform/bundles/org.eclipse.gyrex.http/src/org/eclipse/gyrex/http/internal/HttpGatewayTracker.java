/*******************************************************************************
 * Copyright (c) 2008, 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.gyrex.http.internal.application.gateway.IHttpGateway;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationManager;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A server tracker that creates {@link ApplicationManager}s for
 */
public class HttpGatewayTracker extends ServiceTracker {

	private final ConcurrentMap<IHttpGateway, ApplicationManager> appManagerByGateway = new ConcurrentHashMap<IHttpGateway, ApplicationManager>(2);

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 */
	public HttpGatewayTracker(final BundleContext context) {
		super(context, IHttpGateway.class.getName(), null);
	}

	@Override
	public Object addingService(final ServiceReference reference) {
		final IHttpGateway httpGateway = (IHttpGateway) super.addingService(reference); // calls context.getService(reference);

		// start the application registry
		final ApplicationManager applicationManager = new ApplicationManager(context, httpGateway);
		if (null == appManagerByGateway.putIfAbsent(httpGateway, applicationManager)) {
			applicationManager.open();
		}

		return httpGateway;
	}

	@Override
	public void removedService(final ServiceReference reference, final Object service) {
		final ApplicationManager applicationManager = appManagerByGateway.remove(service);
		if (null != applicationManager) {
			applicationManager.close();
		}

		super.removedService(reference, service); // calls context.ungetService(reference);
	}

}
