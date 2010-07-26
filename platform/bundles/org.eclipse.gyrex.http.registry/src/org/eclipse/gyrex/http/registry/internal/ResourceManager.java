/*******************************************************************************
 * Copyright (c) 2008 AGETO Service GmbH and others.
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

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionRegistry;

import org.osgi.framework.ServiceReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceManager implements ExtensionPointTracker.Listener {

	private static final String RESOURCES_EXTENSION_POINT = "org.eclipse.gyrex.http.applications"; //$NON-NLS-1$
	private static final String PATH = "path"; //$NON-NLS-1$
	private static final String ALIAS = "alias"; //$NON-NLS-1$
	private static final String RESOURCE = "resource"; //$NON-NLS-1$
	private static final String APPLICATION_ID = "applicationId"; //$NON-NLS-1$

	private static final Logger LOG = LoggerFactory.getLogger(ResourceManager.class);

	private final ExtensionPointTracker tracker;
	private final List<IConfigurationElement> registered = new ArrayList<IConfigurationElement>();
	private final ApplicationRegistryManager applicationRegistryManager;

	public ResourceManager(final ApplicationRegistryManager httpRegistryManager, final ServiceReference reference, final IExtensionRegistry registry) {
		applicationRegistryManager = httpRegistryManager;
		tracker = new ExtensionPointTracker(registry, RESOURCES_EXTENSION_POINT, this);
	}

	@Override
	public void added(final IExtension extension) {
		final IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			final IConfigurationElement resourceElement = elements[i];
			if (!RESOURCE.equals(resourceElement.getName())) {
				continue;
			}

			final String alias = resourceElement.getAttribute(ALIAS);
			if (alias == null) {
				LOG.warn("Ignoring resource extension element contributed by {}. Does not contain an alias.", resourceElement.getContributor());
				continue; // alias is mandatory - ignore this.
			}

			String path = resourceElement.getAttribute(PATH);
			if (path == null) {
				path = ""; //$NON-NLS-1$
			}
			if (path.charAt(path.length() - 1) == '/') {
				path = path.substring(0, path.length() - 1);
			}

			String applicationId = resourceElement.getAttribute(APPLICATION_ID);
			if (applicationId == null) {
				LOG.warn("Ignoring resource extension element contributed by {}. Does not contain an application id.", resourceElement.getContributor());
				continue; // applicationId is mandatory - ignore this.
			}

			if (applicationId.indexOf('.') == -1) {
				applicationId = resourceElement.getNamespaceIdentifier() + "." + applicationId; //$NON-NLS-1$
			}

			if (HttpRegistryDebug.extensionRegistration) {
				LOG.debug("Trying to add resource {} to application {} (contributed by {}).", new Object[] { alias, applicationId, resourceElement.getContributor() });
			}
			if (applicationRegistryManager.addResourcesContribution(alias, path, applicationId, extension.getContributor())) {
				if (HttpRegistryDebug.extensionRegistration) {
					LOG.debug("Successfully added resource {} to application {} (contributed by {}).", new Object[] { alias, applicationId, resourceElement.getContributor() });
				}
				registered.add(resourceElement);
			} else {
				if (HttpRegistryDebug.extensionRegistration) {
					LOG.debug("Did not add resource {} to application {} (contributed by {}).", new Object[] { alias, applicationId, resourceElement.getContributor() });
				}
			}
		}
	}

	@Override
	public void removed(final IExtension extension) {
		final IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			final IConfigurationElement resourceElement = elements[i];
			if (!RESOURCE.equals(resourceElement.getName())) {
				continue;
			}

			final String alias = resourceElement.getAttribute(ALIAS);
			if (alias == null) {
				LOG.warn("Ignoring resource extension element contributed by {}. Does not contain an alias.", resourceElement.getContributor());
				continue; // alias is mandatory - ignore this.
			}

			final String applicationId = resourceElement.getAttribute(APPLICATION_ID);
			if (applicationId == null) {
				LOG.warn("Ignoring resource extension element contributed by {}. Does not contain an application id.", resourceElement.getContributor());
				continue; // applicationId is mandatory - ignore this.
			}

			if (HttpRegistryDebug.extensionRegistration) {
				LOG.debug("Trying to remove resource {} to application {} (contributed by {}).", new Object[] { alias, applicationId, resourceElement.getContributor() });
			}
			if (registered.remove(resourceElement)) {
				if (HttpRegistryDebug.extensionRegistration) {
					LOG.debug("Successfully removed resource {} to application {} (contributed by {}).", new Object[] { alias, applicationId, resourceElement.getContributor() });
				}
				applicationRegistryManager.removeContribution(alias, applicationId);
			} else {
				if (HttpRegistryDebug.extensionRegistration) {
					LOG.debug("Did not remove resource {} to application {} (contributed by {}).", new Object[] { alias, applicationId, resourceElement.getContributor() });
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
