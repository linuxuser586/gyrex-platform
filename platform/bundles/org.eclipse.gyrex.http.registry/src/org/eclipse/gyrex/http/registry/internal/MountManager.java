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

public class MountManager implements ExtensionPointTracker.Listener {

	private static final String MOUNTS_EXTENSION_POINT = "org.eclipse.cloudfree.http.applications"; //$NON-NLS-1$
	private static final String URL = "url"; //$NON-NLS-1$
	private static final String MOUNT = "mount"; //$NON-NLS-1$

	private static final String APPLICATION_ID = "applicationId"; //$NON-NLS-1$

	private final ExtensionPointTracker tracker;

	private final List<IConfigurationElement> registered = new ArrayList<IConfigurationElement>();

	private final ApplicationRegistryManager applicationRegistryManager;

	public MountManager(final ApplicationRegistryManager httpRegistryManager, final ServiceReference reference, final IExtensionRegistry registry) {
		applicationRegistryManager = httpRegistryManager;
		tracker = new ExtensionPointTracker(registry, MOUNTS_EXTENSION_POINT, this);
	}

	public void added(final IExtension extension) {
		final IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			final IConfigurationElement mountElement = elements[i];
			if (!MOUNT.equals(mountElement.getName())) {
				continue;
			}

			final String url = mountElement.getAttribute(URL);
			if (url == null) {
				continue; // alias is mandatory - ignore this.
			}

			String applicationId = mountElement.getAttribute(APPLICATION_ID);
			if (applicationId == null) {
				continue; // applicationId is mandatory - ignore this.
			}

			if (applicationId.indexOf('.') == -1) {
				applicationId = mountElement.getNamespaceIdentifier() + "." + applicationId; //$NON-NLS-1$
			}

			if (applicationRegistryManager.addMountContribution(url, applicationId, extension.getContributor())) {
				registered.add(mountElement);
			}
		}
	}

	public void removed(final IExtension extension) {
		final IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			final IConfigurationElement mountElement = elements[i];
			if (registered.remove(mountElement)) {
				final String url = mountElement.getAttribute(URL);
				final String applicationId = mountElement.getAttribute(APPLICATION_ID);
				applicationRegistryManager.removeMountContribution(url, applicationId);
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
