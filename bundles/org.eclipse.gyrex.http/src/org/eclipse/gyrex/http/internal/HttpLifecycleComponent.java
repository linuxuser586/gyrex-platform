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
package org.eclipse.gyrex.http.internal;

import org.eclipse.gyrex.cloud.events.ICloudEventConstants;
import org.eclipse.gyrex.http.application.manager.IApplicationManager;
import org.eclipse.gyrex.http.internal.application.gateway.HttpGatewayBinding;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationManager;

import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 *
 */
public class HttpLifecycleComponent implements EventHandler {

	private ApplicationManager applicationManager;
	private ServiceRegistration<IApplicationManager> appManagerServiceRegistration;
	private HttpGatewayBinding gatewayBinding;

	private synchronized void activate() {
		if (null != applicationManager) {
			// already active
			return;
		}

		applicationManager = new ApplicationManager();
		appManagerServiceRegistration = HttpActivator.getInstance().getServiceHelper().registerService(IApplicationManager.class, applicationManager, "Eclipse Gyrex", "Gyrex Web Application Management Service", null, null);

		gatewayBinding = new HttpGatewayBinding(HttpActivator.getInstance().getBundle().getBundleContext(), applicationManager, HttpActivator.getInstance().getProviderRegistry());
		gatewayBinding.open();
	}

	private synchronized void deactivate() {
		if (null != gatewayBinding) {
			gatewayBinding.close();
			gatewayBinding = null;
		}

		if (null != appManagerServiceRegistration) {
			appManagerServiceRegistration.unregister();
			appManagerServiceRegistration = null;
		}

		if (null != applicationManager) {
			applicationManager = null;
		}
	}

	@Override
	public void handleEvent(final Event event) {
		if (ICloudEventConstants.TOPIC_NODE_ONLINE.equals(event.getTopic())) {
			activate();
		} else if (ICloudEventConstants.TOPIC_NODE_OFFLINE.equals(event.getTopic())) {
			deactivate();
		}
	}

}
