/*******************************************************************************
 * Copyright (c) 2008, 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Cognos Incorporated, IBM Corporation - concept/implementation from
 *                                            org.eclipse.equinox.http.registry
 *     Gunnar Wagenknecht - adaption to Gyrex
 *******************************************************************************/
package org.eclipse.gyrex.http.registry.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.gyrex.http.registry.ApplicationCustomizer;
import org.eclipse.gyrex.http.registry.internal.ExtensionPointTracker.Listener;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionRegistry;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationManager implements Listener {

	private static final String APPLICATIONS_EXTENSION_POINT = "org.eclipse.gyrex.http.applications"; //$NON-NLS-1$
	private static final String APPLICATION = "application"; //$NON-NLS-1$
	private static final String ID = "id"; //$NON-NLS-1$
	private static final String CONTEXT_PATH = "contextPath"; //$NON-NLS-1$
	private static final String CUSTOMIZER_CLASS = "customizerClass"; //$NON-NLS-1$

	private static final Logger LOG = LoggerFactory.getLogger(ApplicationManager.class);

	public static ApplicationCustomizer createCustomizer(final IConfigurationElement configurationElement) {
		try {
			if (StringUtils.isBlank(configurationElement.getAttribute(CUSTOMIZER_CLASS))) {
				return null;
			}
			return (ApplicationCustomizer) configurationElement.createExecutableExtension(CUSTOMIZER_CLASS);
		} catch (final Exception e) {
			// TODO consider logging this
			e.printStackTrace();
		}
		return null;
	}

	private final List<IConfigurationElement> registered = new ArrayList<IConfigurationElement>();
	private final ApplicationRegistryManager applicationRegistryManager;

	private final ExtensionPointTracker tracker;

	public ApplicationManager(final ApplicationRegistryManager applicationRegistryManager, final IExtensionRegistry registry) {
		this.applicationRegistryManager = applicationRegistryManager;
		tracker = new ExtensionPointTracker(registry, APPLICATIONS_EXTENSION_POINT, this);
	}

	@Override
	public void added(final IExtension extension) {
		final IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			final IConfigurationElement applicationElement = elements[i];
			if (!APPLICATION.equals(applicationElement.getName())) {
				continue;
			}

			String applicationId = applicationElement.getAttribute(ID);
			if (applicationId == null) {
				LOG.warn("Ignoring application extension element contributed by {}. Does not contain an application id.", applicationElement.getContributor());
				continue;
			}

			if (applicationId.indexOf('.') == -1) {
				applicationId = applicationElement.getNamespaceIdentifier() + "." + applicationId; //$NON-NLS-1$
			}

			String contextPath = applicationElement.getAttribute(CONTEXT_PATH);
			if (contextPath == null) {
				contextPath = "/";
			}

			if (HttpRegistryDebug.extensionRegistration) {
				LOG.debug("Trying to add application {} (contributed by {}).", applicationId, applicationElement.getContributor());
			}

			if (applicationRegistryManager.addApplicationContribution(applicationId, contextPath, applicationElement)) {
				if (HttpRegistryDebug.extensionRegistration) {
					LOG.debug("Successfully added application {} (contributed by {}).", applicationId, applicationElement.getContributor());
				}
				registered.add(applicationElement);
			} else {
				if (HttpRegistryDebug.extensionRegistration) {
					LOG.debug("Did not add application {} (contributed by {}). ", applicationId, applicationElement.getContributor());
				}
			}
		}
	}

	@Override
	public void removed(final IExtension extension) {
		final IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			final IConfigurationElement applicationElement = elements[i];
			if (!APPLICATION.equals(applicationElement.getName())) {
				continue;
			}

			String applicationId = applicationElement.getAttribute(ID);
			if (applicationId == null) {
				LOG.warn("Ignoring application extension element contributed by {}. Does not contain an application id.", applicationElement.getContributor());
				continue;
			}
			if (applicationId.indexOf('.') == -1) {
				applicationId = applicationElement.getNamespaceIdentifier() + "." + applicationId; //$NON-NLS-1$
			}

			if (HttpRegistryDebug.extensionRegistration) {
				LOG.debug("Trying to remove application {} (contributed by {}).", applicationId, applicationElement.getContributor());
			}

			if (registered.remove(applicationElement)) {
				if (HttpRegistryDebug.extensionRegistration) {
					LOG.debug("Successfully removed application {} (contributed by {}).", applicationId, applicationElement.getContributor());
				}
				applicationRegistryManager.removeApplicationContribution(applicationId);
			} else {
				if (HttpRegistryDebug.extensionRegistration) {
					LOG.debug("Did not remove application {} (contributed by {}). ", applicationId, applicationElement.getContributor());
				}
			}
		}
	}

	public void start() {
		tracker.open();
	}

	public void stop() {
		tracker.close();
	}
}
