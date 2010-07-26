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

public class MountManager implements ExtensionPointTracker.Listener {

	private static final String MOUNTS_EXTENSION_POINT = "org.eclipse.gyrex.http.applications"; //$NON-NLS-1$
	private static final String URL = "url"; //$NON-NLS-1$
	private static final String MOUNT = "mount"; //$NON-NLS-1$
	private static final String APPLICATION_ID = "applicationId"; //$NON-NLS-1$

	private static final Logger LOG = LoggerFactory.getLogger(MountManager.class);

	private final ExtensionPointTracker tracker;
	private final List<IConfigurationElement> registered = new ArrayList<IConfigurationElement>();
	private final ApplicationRegistryManager applicationRegistryManager;

	public MountManager(final ApplicationRegistryManager httpRegistryManager, final ServiceReference reference, final IExtensionRegistry registry) {
		applicationRegistryManager = httpRegistryManager;
		tracker = new ExtensionPointTracker(registry, MOUNTS_EXTENSION_POINT, this);
	}

	@Override
	public void added(final IExtension extension) {
		final IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			final IConfigurationElement mountElement = elements[i];
			if (!MOUNT.equals(mountElement.getName())) {
				continue;
			}

			final String url = mountElement.getAttribute(URL);
			if (url == null) {
				LOG.warn("Ignoring mount extension element contributed by {}. Does not contain an alias.", mountElement.getContributor());
				continue; // alias is mandatory - ignore this.
			}

			String applicationId = mountElement.getAttribute(APPLICATION_ID);
			if (applicationId == null) {
				LOG.warn("Ignoring mount extension element contributed by {}. Does not contain an application id.", mountElement.getContributor());
				continue; // applicationId is mandatory - ignore this.
			}

			if (applicationId.indexOf('.') == -1) {
				applicationId = mountElement.getNamespaceIdentifier() + "." + applicationId; //$NON-NLS-1$
			}

			if (HttpRegistryDebug.extensionRegistration) {
				LOG.debug("Trying to add mount {} for application {} (contributed by {}).", new Object[] { url, applicationId, mountElement.getContributor() });
			}
			if (applicationRegistryManager.addMountContribution(url, applicationId, extension.getContributor())) {
				if (HttpRegistryDebug.extensionRegistration) {
					LOG.debug("Successfully added mount {} for application {} (contributed by {}).", new Object[] { url, applicationId, mountElement.getContributor() });
				}
				registered.add(mountElement);
			} else {
				if (HttpRegistryDebug.extensionRegistration) {
					LOG.debug("Did not add mount {} for application {} (contributed by {}).", new Object[] { url, applicationId, mountElement.getContributor() });
				}
			}
		}
	}

	public void removed(final IExtension extension) {
		final IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			final IConfigurationElement mountElement = elements[i];
			if (!MOUNT.equals(mountElement.getName())) {
				continue;
			}

			final String url = mountElement.getAttribute(URL);
			if (url == null) {
				LOG.warn("Ignoring mount extension element contributed by {}. Does not contain an alias.", mountElement.getContributor());
				continue; // alias is mandatory - ignore this.
			}

			final String applicationId = mountElement.getAttribute(APPLICATION_ID);
			if (applicationId == null) {
				LOG.warn("Ignoring mount extension element contributed by {}. Does not contain an application id.", mountElement.getContributor());
				continue; // applicationId is mandatory - ignore this.
			}

			if (HttpRegistryDebug.extensionRegistration) {
				LOG.debug("Trying to remove mount {} for application {} (contributed by {}).", new Object[] { url, applicationId, mountElement.getContributor() });
			}
			if (registered.remove(mountElement)) {
				if (HttpRegistryDebug.extensionRegistration) {
					LOG.debug("Successfully removed mount {} for application {} (contributed by {}).", new Object[] { url, applicationId, mountElement.getContributor() });
				}
				applicationRegistryManager.removeMountContribution(url, applicationId);
			} else {
				if (HttpRegistryDebug.extensionRegistration) {
					LOG.debug("Did not remove mount {} for application {} (contributed by {}).", new Object[] { url, applicationId, mountElement.getContributor() });
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
