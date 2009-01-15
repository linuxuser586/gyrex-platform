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
 *     Gunnar Wagenknecht - adaption to CloudFree
 *******************************************************************************/
package org.eclipse.cloudfree.http.registry.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.osgi.framework.ServiceReference;

public class ResourceManager implements ExtensionPointTracker.Listener {

	private static final String RESOURCES_EXTENSION_POINT = "org.eclipse.cloudfree.http.applications"; //$NON-NLS-1$

	private static final String PATH = "path"; //$NON-NLS-1$

	private static final String ALIAS = "alias"; //$NON-NLS-1$

	private static final String RESOURCE = "resource"; //$NON-NLS-1$

	private static final String APPLICATION_ID = "applicationId"; //$NON-NLS-1$

	private final ExtensionPointTracker tracker;

	private final List<IConfigurationElement> registered = new ArrayList<IConfigurationElement>();

	private final ApplicationRegistryManager applicationRegistryManager;

	public ResourceManager(final ApplicationRegistryManager httpRegistryManager, final ServiceReference reference, final IExtensionRegistry registry) {
		applicationRegistryManager = httpRegistryManager;
		tracker = new ExtensionPointTracker(registry, RESOURCES_EXTENSION_POINT, this);
	}

	public void added(final IExtension extension) {
		final IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			final IConfigurationElement resourceElement = elements[i];
			if (!RESOURCE.equals(resourceElement.getName())) {
				continue;
			}

			final String alias = resourceElement.getAttribute(ALIAS);
			if (alias == null) {
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
				continue; // applicationId is mandatory - ignore this.
			}

			if (applicationId.indexOf('.') == -1) {
				applicationId = resourceElement.getNamespaceIdentifier() + "." + applicationId; //$NON-NLS-1$
			}

			if (applicationRegistryManager.addResourcesContribution(alias, path, applicationId, extension.getContributor())) {
				registered.add(resourceElement);
			}
		}
	}

	public void removed(final IExtension extension) {
		final IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			final IConfigurationElement resourceElement = elements[i];
			if (registered.remove(resourceElement)) {
				final String alias = resourceElement.getAttribute(ALIAS);
				final String applicationId = resourceElement.getAttribute(APPLICATION_ID);
				applicationRegistryManager.removeContribution(alias, applicationId);
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
