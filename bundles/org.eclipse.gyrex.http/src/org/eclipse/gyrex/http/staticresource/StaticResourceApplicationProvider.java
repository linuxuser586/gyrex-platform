/*******************************************************************************
 * Copyright (c) 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Andreas Mihm - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.staticresource;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.provider.ApplicationProvider;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the ApplicationProvider for the {@link StaticResourceApplication}
 */
public class StaticResourceApplicationProvider extends ApplicationProvider {

	private static final Logger LOG = LoggerFactory.getLogger(StaticResourceApplicationProvider.class);

	/**
	 * A component property for a component configuration that contains the
	 * {@link ApplicationProvider#getId() application provider id} which shall
	 * be used for registering the application provider. The value of this
	 * property must be of type {@code String}. If no property is provided, a
	 * fallback will be attempted to the
	 * {@link ComponentConstants#COMPONENT_NAME} property.
	 */
	public static final String APPLICATION_PROVIDER_ID = "applicationProviderId";

	private BundleContext bundleContext;

	private String bundleResourcePath;
	private String devModeDocRootEnvVar;

	public void activate(final ComponentContext context) {
		LOG.debug("StaticResourceApplicationProvider activation triggered for component '{}' (bundle {})", context.getProperties().get(ComponentConstants.COMPONENT_NAME), context.getBundleContext().getBundle());

		// initialize application id
		final String applicationProviderId = getApplicationProviderId(context);
		try {
			LOG.debug("Using application provider id '{}' for component '{}' (bundle {})", new Object[] { applicationProviderId, context.getProperties().get(ComponentConstants.COMPONENT_NAME), context.getBundleContext().getBundle() });
			setId(applicationProviderId);
		} catch (final IllegalStateException e) {
			// compare and only continue if match
			if (!applicationProviderId.equals(getId()))
				throw new IllegalStateException(String.format("The StaticResourceApplicationProvider has already been initialized with an application provider id (%s) and cannot be initialized again with a different id (%s). Please check your component configuration!", getId(), applicationProviderId), e);
		}

		// remember bundle for later use
		bundleContext = context.getBundleContext();

		// remember resource path
		bundleResourcePath = (String) context.getProperties().get("resource.path");
		devModeDocRootEnvVar = (String) context.getProperties().get("resource.devModeDocRootEnvVar");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.http.application.provider.ApplicationProvider#createApplication(java.lang.String, org.eclipse.gyrex.context.IRuntimeContext)
	 */
	@Override
	public Application createApplication(final String applicationId, final IRuntimeContext context) throws Exception {
		return new StaticResourceApplication(applicationId, context, bundleContext.getBundle(), bundleResourcePath, devModeDocRootEnvVar);
	}

	public void deactivate(final ComponentContext context) {
		LOG.debug("StaticResourceApplicationProvider de-activation triggered for component '{}' (bundle {})", context.getProperties().get(ComponentConstants.COMPONENT_NAME), context.getBundleContext().getBundle());
		bundleContext = null;
	}

	private String getApplicationProviderId(final ComponentContext context) {
		final Object applicationProviderIdValue = context.getProperties().get(APPLICATION_PROVIDER_ID);

		if (null == applicationProviderIdValue)
			return (String) context.getProperties().get(ComponentConstants.COMPONENT_NAME);

		if (!(applicationProviderIdValue instanceof String))
			throw new IllegalStateException("The StaticResourceApplicationProvider property 'applicationProviderId' must be of type String!");
		return (String) applicationProviderIdValue;
	}

}
