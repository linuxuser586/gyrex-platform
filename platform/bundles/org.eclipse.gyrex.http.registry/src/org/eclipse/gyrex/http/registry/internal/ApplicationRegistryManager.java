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

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.gyrex.http.application.manager.ApplicationRegistrationException;
import org.eclipse.gyrex.http.application.manager.IApplicationManager;
import org.eclipse.gyrex.http.application.manager.MountConflictException;
import org.eclipse.gyrex.http.application.servicesupport.IResourceProvider;
import org.eclipse.gyrex.http.application.servicesupport.NamespaceException;
import org.eclipse.gyrex.http.internal.apps.dummy.RootContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * 
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
	}

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

	public ApplicationRegistryManager(final ServiceReference reference, final IApplicationManager httpApplicationManager, final PackageAdmin packageAdmin, final IExtensionRegistry registry) {
		this.httpApplicationManager = httpApplicationManager;
		this.packageAdmin = packageAdmin;

		applicationProvider = RegistryApplicationProvider.getInstance();

		applicationManager = new ApplicationManager(this, registry);
		servletManager = new ServletManager(this, reference, registry);
		resourceManager = new ResourceManager(this, reference, registry);
		mountManager = new MountManager(this, reference, registry);
	}

	public synchronized boolean addApplicationContribution(final String applicationId, final String contextPath, final IConfigurationElement configurationElement) {
		if (applications.containsKey(applicationId)) {
			return false; // TODO: should log this
		}

		applications.put(applicationId, new ApplicationContribution(applicationId, contextPath, configurationElement));
		try {
			httpApplicationManager.register(applicationId, RegistryApplicationProvider.ID, new RootContext(), null);
		} catch (final ApplicationRegistrationException e) {
			return false; // TODO: should log this
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
			return false; // TODO: should log this
		}

		final MountContribution mountContribution = new MountContribution(applicationId, url, contributor);
		mounts.put(url, mountContribution);
		if (applications.containsKey(applicationId)) {
			return mountApplication(mountContribution);
		}
		return true;
	}

	public synchronized boolean addResourcesContribution(final String alias, final String path, final String applicationId, final IContributor contributor) {
		if (resources.containsKey(applicationId + ":" + alias) || servlets.containsKey(applicationId + ":" + alias)) {
			return false; // TODO: should log this
		}

		final ResourcesContribution contribution = new ResourcesContribution(alias, path, applicationId, new BundleResourceProvider(getBundle(contributor)), contributor);
		resources.put(applicationId + ":" + alias, contribution);
		if (activeApplications.containsKey(applicationId)) {
			registerResources(contribution, activeApplications.get(applicationId));
		}

		return true;
	}

	public synchronized boolean addServletContribution(final String alias, final Servlet servlet, final Map<String, String> initparams, final String applicationId, final IContributor contributor) {
		if (resources.containsKey(applicationId + ":" + alias) || servlets.containsKey(applicationId + ":" + alias)) {
			return false; // TODO: should log this
		}

		final ServletContribution contribution = new ServletContribution(alias, servlet, initparams, applicationId, contributor);
		servlets.put(applicationId + ":" + alias, contribution);
		if (activeApplications.containsKey(applicationId)) {
			registerServlet(contribution, activeApplications.get(applicationId));
		}

		return true;
	}

	/**
	 * @param registryApplication
	 */
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

	/**
	 * @param registryApplication
	 */
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

	/**
	 * @param contribution
	 */
	private boolean mountApplication(final MountContribution contribution) {
		try {
			httpApplicationManager.mount(contribution.url, contribution.applicationId);
		} catch (final MountConflictException e) {
			return false; // TODO: should log this
		} catch (final MalformedURLException e) {
			return false; // TODO: should log this
		}
		return true;
	}

	private void registerResources(final ResourcesContribution contribution, final RegistryApplication registryApplication) {
		try {
			registryApplication.getApplicationServiceSupport().registerResources(contribution.alias, contribution.path, contribution.resourceProvider);
		} catch (final NamespaceException e) {
			// TODO: should log this
			e.printStackTrace();
		} catch (final Throwable t) {
			// TODO: should log this
			t.printStackTrace();
		}
	}

	private void registerServlet(final ServletContribution contribution, final RegistryApplication registryApplication) {
		try {
			registryApplication.getApplicationServiceSupport().registerServlet(contribution.alias, contribution.servlet, contribution.initparams);
		} catch (final NamespaceException e) {
			// TODO: should log this
			e.printStackTrace();
		} catch (final ServletException e) {
			// TODO: should log this
			e.printStackTrace();
		} catch (final Throwable t) {
			// TODO: should log this
			t.printStackTrace();
		}
	}

	public synchronized void removeApplicationContribution(final String applicationId) {
		if (applications.remove(applicationId) != null) {
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

	/**
	 * @param url
	 * @param applicationId
	 */
	public synchronized void removeMountContribution(final String url, final String applicationId) {
		if (mounts.remove(url) != null) {
			try {
				httpApplicationManager.unmount(url);
			} catch (final Exception e) {
				// TODO should log this
				e.printStackTrace();
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
			application.getApplicationServiceSupport().unregister(alias);
		} catch (final Throwable t) {
			// TODO: should log this
			t.printStackTrace();
		}
	}
}
