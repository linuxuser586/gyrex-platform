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
package org.eclipse.gyrex.http.internal;


import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.gyrex.common.logging.LogAudience;
import org.eclipse.gyrex.common.logging.LogImportance;
import org.eclipse.gyrex.common.logging.LogSource;
import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.helper.BaseDefaultHttpServiceTracker;
import org.eclipse.gyrex.http.internal.application.ApplicationHandlerServlet;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;

/**
 * A server tracker that registers the root servlet with every tracked HTTP
 * service.
 */
public class HttpServiceTracker extends BaseDefaultHttpServiceTracker {

	/** ROOT_ALIAS */
	private static final String ROOT_ALIAS = "/";

	/** alias for the error resources */
	public static final String ALIAS_RESOURCES = "/platform/internal/http/resources";

	private final ApplicationManager applicationManager;

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 * @param applicationManager
	 * @param filter
	 * @param customizer
	 */
	public HttpServiceTracker(final BundleContext context, final ApplicationManager applicationManager) {
		super(context);
		this.applicationManager = applicationManager;
	}

	/**
	 * Registers a {@link WidgetServiceServlet} with the {@link HttpService}.
	 * 
	 * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
	 */
	@Override
	public Object addingService(final ServiceReference reference) {
		final HttpService httpService = (HttpService) super.addingService(reference); // calls context.getService(reference);
		if (null == httpService) {
			return null;
		}

		// check if we are ok to register with the specified service
		if (isAllowedToUse(httpService)) {

			// create the root servlet
			final ApplicationHandlerServlet rootServlet = new ApplicationHandlerServlet(applicationManager);

			try {
				// register the resources
				httpService.registerResources(ALIAS_RESOURCES, "/resources", null);
				httpService.registerServlet(ROOT_ALIAS, rootServlet, null, null);
			} catch (final Exception e) {
				HttpActivator.getInstance().getLog().log(new Status(IStatus.ERROR, HttpActivator.PLUGIN_ID, "An error occurred while registering the root servlet.", e), null, LogImportance.BLOCKER, LogAudience.DEVELOPER, LogAudience.ADMIN, LogSource.PLATFORM);
			}
		}

		return httpService;
	}

	private boolean isAllowedToUse(final HttpService httpService) {
		// don't register with services inherited from our objects
		return !Application.class.isAssignableFrom(httpService.getClass());
	}

	/**
	 * Unregisters the registered {@link WidgetServiceServlet}.
	 * 
	 * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference,
	 *      java.lang.Object)
	 */
	@Override
	public void removedService(final ServiceReference reference, final Object service) {
		final HttpService httpService = (HttpService) service;

		// unregister 
		if (isAllowedToUse(httpService)) {
			httpService.unregister(ROOT_ALIAS);
			httpService.unregister(ALIAS_RESOURCES);
		}

		super.removedService(reference, service); // calls context.ungetService(reference);
	}
}
