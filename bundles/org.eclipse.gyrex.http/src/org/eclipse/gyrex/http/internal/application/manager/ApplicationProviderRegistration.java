/*******************************************************************************
 * Copyright (c) 2008, 2011 Gunnar Wagenknecht and others.
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

import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.provider.ApplicationProvider;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A registered {@link ApplicationProvider}
 */
public class ApplicationProviderRegistration {

	private static final Logger LOG = LoggerFactory.getLogger(ApplicationProviderRegistration.class);

	private final AtomicReference<ApplicationProvider> provider = new AtomicReference<ApplicationProvider>();
	private final List<ApplicationRegistration> activeApplications = new CopyOnWriteArrayList<ApplicationRegistration>();

	private final String contributorInfo;
	private final ServiceReference<ApplicationProvider> reference;
	private final String providerId;

	/**
	 * Creates a new instance.
	 * 
	 * @param reference
	 * @param provider
	 */
	public ApplicationProviderRegistration(final ServiceReference<ApplicationProvider> reference, final ApplicationProvider provider) {
		this.reference = reference;
		providerId = provider.getId();
		this.provider.set(provider);

		final Bundle bundle = reference.getBundle();
		contributorInfo = null != bundle ? String.format("%s (%s)", bundle.getSymbolicName(), bundle.getVersion().toString()) : "<unknown>";
	}

	/**
	 * Creates a new application for the specified registration.
	 * 
	 * @param applicationRegistration
	 * @return a new application instance or <code>null</code> if the provider
	 *         has been destroyed
	 * @throws Exception
	 */
	Application createApplication(final ApplicationRegistration applicationRegistration) throws Exception {
		final ApplicationProvider provider = this.provider.get();
		if (null == provider) {
			// destroyed
			return null;
		}
		final Application application = provider.createApplication(applicationRegistration.getApplicationId(), applicationRegistration.getContext());
		if (null == application) {
			LOG.error("Provider '{}' did not return an application instance", provider.getId());
			return null;
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

	/**
	 * Returns the contributorInfo.
	 * 
	 * @return the contributorInfo
	 */
	public String getContributorInfo() {
		return contributorInfo;
	}

	/**
	 * Returns the providerId.
	 * 
	 * @return the providerId
	 */
	public String getProviderId() {
		return providerId;
	}

	public String getProviderInfo() {
		final Object description = reference.getProperty(Constants.SERVICE_DESCRIPTION);
		if (description instanceof String) {
			return (String) description;
		}

		return "<unknown>";
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ApplicationProviderRegistration [");
		final ApplicationProvider provider = this.provider.get();
		if (null != provider) {
			builder.append(provider.getId());
			builder.append(", provider=").append(provider);
		} else {
			builder.append("provider=(destroyed)");
		}
		builder.append(", contributedBy=").append(contributorInfo).append(", activeApplications=").append(activeApplications.size()).append("]");
		return builder.toString();
	}
}
