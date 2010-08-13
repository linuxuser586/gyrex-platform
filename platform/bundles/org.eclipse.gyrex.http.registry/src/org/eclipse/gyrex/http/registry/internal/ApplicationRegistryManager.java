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

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;
import org.eclipse.gyrex.http.application.context.IResourceProvider;
import org.eclipse.gyrex.http.application.context.NamespaceException;
import org.eclipse.gyrex.http.application.manager.ApplicationRegistrationException;
import org.eclipse.gyrex.http.application.manager.IApplicationManager;
import org.eclipse.gyrex.http.application.manager.MountConflictException;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Path;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gate between extension registry and {@link IApplicationManager}.
 */
public class ApplicationRegistryManager {

	class ApplicationContribution {
		final String applicationId;
		final String contextPath;
		final IConfigurationElement configurationElement;

		public ApplicationContribution(final String applicationId, final String contextPath, final IConfigurationElement configurationElement) {
			this.applicationId = applicationId;
			this.contextPath = contextPath;
			this.configurationElement = configurationElement;
		}

		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder();
			builder.append("ApplicationContribution [");
			if (applicationId != null) {
				builder.append("applicationId=");
				builder.append(applicationId);
				builder.append(", ");
			}
			if (contextPath != null) {
				builder.append("contextPath=");
				builder.append(contextPath);
				builder.append(", ");
			}
			if (configurationElement != null) {
				builder.append("contributor=");
				builder.append(configurationElement.getContributor());
			}
			builder.append("]");
			return builder.toString();
		}
	}

	class MountContribution {
		final IContributor contributor;
		final String applicationId;
		final String url;

		public MountContribution(final String applicationId, final String url, final IContributor contributor) {
			this.applicationId = applicationId;
			this.url = url;
			this.contributor = contributor;
		}

		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder();
			builder.append("MountContribution [");
			if (url != null) {
				builder.append("url=");
				builder.append(url);
				builder.append(", ");
			}
			if (applicationId != null) {
				builder.append("applicationId=");
				builder.append(applicationId);
				builder.append(", ");
			}
			if (contributor != null) {
				builder.append("contributor=");
				builder.append(contributor);
			}
			builder.append("]");
			return builder.toString();
		}
	}

	class ResourcesContribution {
		final String alias;
		final String path;
		final String applicationId;
		final IResourceProvider resourceProvider;
		final IContributor contributor;

		public ResourcesContribution(final String alias, final String path, final String applicationId, final IResourceProvider resourceProvider, final IContributor contributor) {
			this.alias = alias;
			this.path = path;
			this.applicationId = applicationId;
			this.resourceProvider = resourceProvider;
			this.contributor = contributor;
		}

		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder();
			builder.append("ResourcesContribution [");
			if (alias != null) {
				builder.append("alias=");
				builder.append(alias);
				builder.append(", ");
			}
			if (applicationId != null) {
				builder.append("applicationId=");
				builder.append(applicationId);
				builder.append(", ");
			}
			if (contributor != null) {
				builder.append("contributor=");
				builder.append(contributor);
			}
			builder.append("]");
			return builder.toString();
		}
	}

	class ServletContribution {
		final String alias;
		final Servlet servlet;
		final Map<String, String> initparams;
		final String applicationId;
		final IContributor contributor;

		public ServletContribution(final String alias, final Servlet servlet, final Map<String, String> initparams, final String applicationId, final IContributor contributor) {
			this.alias = alias;
			this.servlet = servlet;
			this.initparams = initparams;
			this.applicationId = applicationId;
			this.contributor = contributor;
		}

		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder();
			builder.append("ServletContribution [");
			if (alias != null) {
				builder.append("alias=");
				builder.append(alias);
				builder.append(", ");
			}
			if (applicationId != null) {
				builder.append("applicationId=");
				builder.append(applicationId);
				builder.append(", ");
			}
			if (contributor != null) {
				builder.append("contributor=");
				builder.append(contributor);
			}
			builder.append("]");
			return builder.toString();
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(ApplicationRegistryManager.class);

	private final ApplicationManager applicationManager;
	private final ServletManager servletManager;
	private final ResourceManager resourceManager;
	private final MountManager mountManager;
	private final IApplicationManager httpApplicationManager;
	private final PackageAdmin packageAdmin;
	private final RegistryApplicationProvider applicationProvider;
	private final Map<String, ApplicationContribution> applications = new HashMap<String, ApplicationContribution>();
	private final Map<String, RegistryApplication> activeApplications = new HashMap<String, RegistryApplication>();
	private final Map<String, ServletContribution> servlets = new HashMap<String, ServletContribution>();
	private final Map<String, ResourcesContribution> resources = new HashMap<String, ResourcesContribution>();
	private final Map<String, MountContribution> mounts = new HashMap<String, MountContribution>();
	private final IRuntimeContextRegistry contextRegistry;

	public ApplicationRegistryManager(final ServiceReference reference, final IApplicationManager httpApplicationManager, final PackageAdmin packageAdmin, final IExtensionRegistry registry, final IRuntimeContextRegistry contextRegistry) {
		this.httpApplicationManager = httpApplicationManager;
		this.packageAdmin = packageAdmin;
		this.contextRegistry = contextRegistry;

		applicationProvider = RegistryApplicationProvider.getInstance();

		applicationManager = new ApplicationManager(this, registry);
		servletManager = new ServletManager(this, reference, registry);
		resourceManager = new ResourceManager(this, reference, registry);
		mountManager = new MountManager(this, reference, registry);
	}

	public synchronized boolean addApplicationContribution(final String applicationId, final String contextPath, final IConfigurationElement configurationElement) {
		if (applications.containsKey(applicationId)) {
			LOG.warn("Application {} already registered (existing registration: {}).", applicationId, applications.get(applicationId));
			return false;
		}

		final IRuntimeContext context = contextRegistry.get(null != contextPath ? new Path(contextPath) : Path.EMPTY);
		if (null == context) {
			LOG.warn("Context path {} requested by application {} is unknown.", contextPath, applicationId);
			return false;
		}

		applications.put(applicationId, new ApplicationContribution(applicationId, contextPath, configurationElement));
		try {
			if (HttpRegistryDebug.applicationLifecycle) {
				LOG.debug("Registering application {}.", applicationId);
			}
			httpApplicationManager.register(applicationId, RegistryApplicationProvider.ID, context, null);
		} catch (final ApplicationRegistrationException e) {
			LOG.warn("Could not register application {}. {}", applicationId, e.getMessage());
			return false;
		}

		for (final Iterator<MountContribution> it = mounts.values().iterator(); it.hasNext();) {
			final MountContribution contribution = it.next();
			if (applicationId.equals(contribution.applicationId)) {
				mountApplication(contribution);
			}
		}

		return true;
	}

	public synchronized boolean addMountContribution(final String url, final String applicationId, final IContributor contributor) {
		if (mounts.containsKey(url)) {
			LOG.warn("Mount point {} already in use (application trying to use {}, existing registration {}.", new Object[] { url, applicationId, mounts.get(url) });
			return false;
		}

		final MountContribution mountContribution = new MountContribution(applicationId, url, contributor);
		mounts.put(url, mountContribution);
		if (applications.containsKey(applicationId)) {
			return mountApplication(mountContribution);
		}
		return true;
	}

	public synchronized boolean addResourcesContribution(final String alias, final String path, final String applicationId, final IContributor contributor) {
		final String registrationKey = applicationId + ":" + alias;
		if (resources.containsKey(registrationKey) || servlets.containsKey(registrationKey)) {
			LOG.warn("Alias {} already registered for application {} (existing registrations: {}, {}).", new Object[] { alias, applicationId, resources.containsKey(registrationKey), servlets.containsKey(registrationKey) });
			return false;
		}

		final ResourcesContribution contribution = new ResourcesContribution(alias, path, applicationId, new BundleResourceProvider(getBundle(contributor)), contributor);
		resources.put(registrationKey, contribution);
		if (activeApplications.containsKey(applicationId)) {
			registerResources(contribution, activeApplications.get(applicationId));
		}

		return true;
	}

	public synchronized boolean addServletContribution(final String alias, final Servlet servlet, final Map<String, String> initparams, final String applicationId, final IContributor contributor) {
		final String registrationKey = applicationId + ":" + alias;
		if (resources.containsKey(registrationKey) || servlets.containsKey(registrationKey)) {
			LOG.warn("Alias {} already registered for application {} (existing registrations: {}, {}).", new Object[] { alias, applicationId, resources.containsKey(registrationKey), servlets.containsKey(registrationKey) });
			return false;
		}

		final ServletContribution contribution = new ServletContribution(alias, servlet, initparams, applicationId, contributor);
		servlets.put(registrationKey, contribution);
		if (activeApplications.containsKey(applicationId)) {
			registerServlet(contribution, activeApplications.get(applicationId));
		}

		return true;
	}

	public synchronized void closeApplication(final RegistryApplication application) {
		final String applicationId = application.getId();
		if (null != activeApplications.remove(applicationId)) {
			// TODO: this should not be necessary as everything is discarded anyway
			for (final Iterator it = resources.values().iterator(); it.hasNext();) {
				final ResourcesContribution contribution = (ResourcesContribution) it.next();
				if (applicationId.equals(contribution.applicationId)) {
					unregister(contribution.alias, application);
				}
			}

			for (final Iterator it = servlets.values().iterator(); it.hasNext();) {
				final ServletContribution contribution = (ServletContribution) it.next();
				if (applicationId.equals(contribution.applicationId)) {
					unregister(contribution.alias, application);
				}
			}
		}
	}

	public Bundle getBundle(final IContributor contributor) {
		return getBundle(contributor.getName());
	}

	public Bundle getBundle(final String symbolicName) {
		final Bundle[] bundles = packageAdmin.getBundles(symbolicName, null);
		if (bundles == null) {
			return null;
		}
		//Return the first bundle that is not installed or uninstalled
		for (int i = 0; i < bundles.length; i++) {
			if ((bundles[i].getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
				return bundles[i];
			}
		}
		return null;
	}

	public synchronized void initApplication(final RegistryApplication registryApplication) {
		final String applicationId = registryApplication.getId();
		if (activeApplications.containsKey(applicationId)) {
			return;
		}
		activeApplications.put(applicationId, registryApplication);

		// set customizer
		final ApplicationContribution applicationContribution = applications.get(applicationId);
		registryApplication.setCustomizer(ApplicationManager.createCustomizer(applicationContribution.configurationElement));

		// register resources
		for (final Iterator it = resources.values().iterator(); it.hasNext();) {
			final ResourcesContribution contribution = (ResourcesContribution) it.next();
			if (applicationId.equals(contribution.applicationId)) {
				registerResources(contribution, registryApplication);
			}
		}

		// register servlets
		for (final Iterator it = servlets.values().iterator(); it.hasNext();) {
			final ServletContribution contribution = (ServletContribution) it.next();
			if (applicationId.equals(contribution.applicationId)) {
				registerServlet(contribution, registryApplication);
			}
		}
	}

	private boolean mountApplication(final MountContribution contribution) {
		try {
			if (HttpRegistryDebug.applicationLifecycle) {
				LOG.debug("Mounting application {} to {}.", contribution.applicationId, contribution.url);
			}
			httpApplicationManager.mount(contribution.url, contribution.applicationId);
		} catch (final MountConflictException e) {
			LOG.warn("Could not mount application {}. {}", contribution.applicationId, e.getMessage());
			return false;
		} catch (final MalformedURLException e) {
			LOG.warn("Could not mount application {}. {}", contribution.applicationId, e.getMessage());
			return false;
		}
		return true;
	}

	private void registerResources(final ResourcesContribution contribution, final RegistryApplication registryApplication) {
		try {
			if (HttpRegistryDebug.applicationLifecycle) {
				LOG.debug("Registering resource {} with application {}.", new Object[] { contribution.alias, contribution.applicationId });
			}
			registryApplication.getApplicationServiceSupport().registerResources(contribution.alias, contribution.path, contribution.resourceProvider);
		} catch (final NamespaceException e) {
			LOG.warn("Could not register resource {} with application {}. {}", new Object[] { contribution.alias, contribution.applicationId, e.getMessage() });
		} catch (final IllegalStateException e) {
			LOG.warn("Could not register resource {} with application {}. {}", new Object[] { contribution.alias, contribution.applicationId, e.getMessage() });
		}
	}

	private void registerServlet(final ServletContribution contribution, final RegistryApplication registryApplication) {
		try {
			if (HttpRegistryDebug.applicationLifecycle) {
				LOG.debug("Registering servlet {} with application {}.", new Object[] { contribution.alias, contribution.applicationId });
			}
			registryApplication.getApplicationServiceSupport().registerServlet(contribution.alias, contribution.servlet, contribution.initparams);
		} catch (final NamespaceException e) {
			LOG.warn("Could not register servlet {} with application {}. {}", new Object[] { contribution.alias, contribution.applicationId, e.getMessage() });
		} catch (final ServletException e) {
			LOG.warn("Could not register servlet {} with application {}. Error in servlet initialization. {}", new Object[] { contribution.alias, contribution.applicationId, e.getMessage() });
		} catch (final IllegalStateException e) {
			LOG.warn("Could not register servlet {} with application {}. {}", new Object[] { contribution.alias, contribution.applicationId, e.getMessage() });
		}
	}

	public synchronized void removeApplicationContribution(final String applicationId) {
		if (applications.remove(applicationId) != null) {
			if (HttpRegistryDebug.applicationLifecycle) {
				LOG.debug("Unregistering application {}.", applicationId);
			}
			httpApplicationManager.unregister(applicationId);
		}
	}

	public synchronized void removeContribution(final String alias, final String applicationId) {
		resources.remove(applicationId + ":" + alias);
		servlets.remove(applicationId + ":" + alias);
		if (activeApplications.containsKey(applicationId)) {
			unregister(alias, activeApplications.get(applicationId));
		}
	}

	public synchronized void removeMountContribution(final String url, final String applicationId) {
		if (mounts.remove(url) != null) {
			try {
				if (HttpRegistryDebug.applicationLifecycle) {
					LOG.debug("Unmounting {} from application {}.", new Object[] { url, applicationId });
				}
				httpApplicationManager.unmount(url);
			} catch (final Exception e) {
				LOG.warn("Could not unmount {} from application {}. {}", new Object[] { url, applicationId, e.getMessage() });
			}
		}
	}

	public void start() {
		applicationProvider.setManager(this);
		applicationManager.start();
		servletManager.start();
		resourceManager.start();
		mountManager.start();
	}

	public void stop() {
		mountManager.stop();
		resourceManager.stop();
		servletManager.stop();
		applicationManager.stop();
		applicationProvider.setManager(null);
	}

	private void unregister(final String alias, final RegistryApplication application) {
		try {
			if (HttpRegistryDebug.applicationLifecycle) {
				LOG.debug("Removing registration {} from application {}", new Object[] { alias, application.getId() });
			}
			application.getApplicationServiceSupport().unregister(alias);
		} catch (final IllegalArgumentException e) {
			LOG.warn("Could not unregister {} from application {}. {}", new Object[] { alias, application.getId(), e.getMessage() });
		} catch (final IllegalStateException e) {
			LOG.warn("Could not unregister {} from application {}. {}", new Object[] { alias, application.getId(), e.getMessage() });
		}
	}
}
