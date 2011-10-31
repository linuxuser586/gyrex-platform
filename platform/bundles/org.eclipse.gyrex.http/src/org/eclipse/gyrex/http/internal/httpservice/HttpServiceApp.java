/*******************************************************************************
 * Copyright (c) 2010, 2011 AGETO Service GmbH and others.
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

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.equinox.http.servlet.ExtendedHttpService;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.internal.HttpActivator;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

/**
 * The HttpService application.
 */
public class HttpServiceApp extends Application implements ServiceFactory<HttpService> {

	private ServiceRegistration serviceRegistration;
	private Filter filter;

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
		filter = null;
	}

	@Override
	protected void doInit() throws Exception {
		final String filterStr = getApplicationContext().getInitProperties().get("filter");
		if (null != filterStr) {
			try {
				filter = FrameworkUtil.createFilter(filterStr);
			} catch (final InvalidSyntaxException e) {
				throw new IllegalArgumentException(String.format("The specified filter '%s' is invalid. Please check the application configuration. %s", filterStr, e.getMessage()), e);
			}
		}

		final Dictionary<String, Object> props = new Hashtable<String, Object>(getApplicationContext().getInitProperties());

		if (null == props.get(Constants.SERVICE_DESCRIPTION)) {
			props.put(Constants.SERVICE_DESCRIPTION, "Gyrex web application based OSGi HttpService");
		}
		if (null == props.get(Constants.SERVICE_VENDOR)) {
			props.put(Constants.SERVICE_VENDOR, "Eclipse Gyrex");
		}
		if (null == props.get(Constants.SERVICE_PID)) {
			props.put(Constants.SERVICE_PID, HttpActivator.SYMBOLIC_NAME.concat(".service-").concat(getId()));
		}

		serviceRegistration = HttpActivator.getInstance().getBundle().getBundleContext().registerService(new String[] { HttpService.class.getName(), ExtendedHttpService.class.getName() }, this, props);
	}

	@Override
	public HttpService getService(final Bundle bundle, final ServiceRegistration<HttpService> registration) {
		return new HttpServiceImpl(getApplicationContext(), bundle, filter);
	}

	@Override
	public void ungetService(final Bundle bundle, final ServiceRegistration<HttpService> registration, final HttpService service) {
		((HttpServiceImpl) service).unregisterAll();
	}
}
