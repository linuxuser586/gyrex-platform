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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionRegistry;

import org.osgi.framework.ServiceReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServletManager implements ExtensionPointTracker.Listener {

	private static class ServletWrapper implements Servlet {

		private static final String CLASS = "class"; //$NON-NLS-1$
		private final IConfigurationElement element;
		private Servlet delegate;
		private ServletConfig config;
		private boolean loadOnStartup = false;

		public ServletWrapper(final IConfigurationElement element) {
			this.element = element;
		}

		public void destroy() {
			destroyDelegate();
		}

		private void destroyDelegate() {
			if (delegate != null) {
				final Servlet doomedDelegate = delegate;
				delegate = null;
				doomedDelegate.destroy();
			}
		}

		public ServletConfig getServletConfig() {
			return config;
		}

		public String getServletInfo() {
			return ""; //$NON-NLS-1$
		}

		public void init(final ServletConfig config) throws ServletException {
			this.config = config;
			if (loadOnStartup) {
				initializeDelegate();
			}
		}

		private void initializeDelegate() throws ServletException {
			if (delegate == null) {
				try {
					final Servlet newDelegate = (Servlet) element.createExecutableExtension(CLASS);
					newDelegate.init(config);
					delegate = newDelegate;
				} catch (final CoreException e) {
					throw new ServletException(e);
				}
			}
		}

		public void service(final ServletRequest arg0, final ServletResponse arg1) throws ServletException, IOException {
			initializeDelegate();
			delegate.service(arg0, arg1);
		}

		public void setLoadOnStartup() {
			loadOnStartup = true;
		}
	}

	private static final String SERVLETS_EXTENSION_POINT = "org.eclipse.gyrex.http.applications"; //$NON-NLS-1$
	private static final String PARAM_VALUE = "value"; //$NON-NLS-1$
	private static final String PARAM_NAME = "name"; //$NON-NLS-1$
	private static final String INIT_PARAM = "init-param"; //$NON-NLS-1$
	private static final String SERVLET = "servlet"; //$NON-NLS-1$
	private static final String ALIAS = "alias"; //$NON-NLS-1$
	private static final String LOAD_ON_STARTUP = "load-on-startup"; //$NON-NLS-1$
	private static final String APPLICATION_ID = "applicationId"; //$NON-NLS-1$

	private static final Logger LOG = LoggerFactory.getLogger(ServletManager.class);

	private final ExtensionPointTracker tracker;
	private final ApplicationRegistryManager httpRegistryManager;
	private final List<IConfigurationElement> registered = new ArrayList<IConfigurationElement>();

	public ServletManager(final ApplicationRegistryManager httpRegistryManager, final ServiceReference reference, final IExtensionRegistry registry) {
		this.httpRegistryManager = httpRegistryManager;
		tracker = new ExtensionPointTracker(registry, SERVLETS_EXTENSION_POINT, this);
	}

	public void added(final IExtension extension) {
		final IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			final IConfigurationElement servletElement = elements[i];
			if (!SERVLET.equals(servletElement.getName())) {
				continue;
			}

			final ServletWrapper wrapper = new ServletWrapper(servletElement);
			final String alias = servletElement.getAttribute(ALIAS);
			if (alias == null) {
				LOG.warn("Ignoring servlet extension element contributed by {}. Does not contain an alias.", servletElement.getContributor());
				continue; // alias is mandatory - ignore this.
			}

			final Map<String, String> initparams = new HashMap<String, String>();
			final IConfigurationElement[] initParams = servletElement.getChildren(INIT_PARAM);
			for (int j = 0; j < initParams.length; ++j) {
				final String paramName = initParams[j].getAttribute(PARAM_NAME);
				final String paramValue = initParams[j].getAttribute(PARAM_VALUE);
				initparams.put(paramName, paramValue);
			}

			final boolean loadOnStartup = new Boolean(servletElement.getAttribute(LOAD_ON_STARTUP)).booleanValue();
			if (loadOnStartup) {
				wrapper.setLoadOnStartup();
			}

			String applicationId = servletElement.getAttribute(APPLICATION_ID);
			if (applicationId == null) {
				LOG.warn("Ignoring servlet extension element contributed by {}. Does not contain an application id.", servletElement.getContributor());
				continue; // alias is mandatory - ignore this.
			}

			if (applicationId.indexOf('.') == -1) {
				applicationId = servletElement.getNamespaceIdentifier() + "." + applicationId; //$NON-NLS-1$
			}

			if (HttpRegistryDebug.extensionRegistration) {
				LOG.debug("Trying to add servlet {} to application {} (contributed by {}).", new Object[] { alias, applicationId, servletElement.getContributor() });
			}
			if (httpRegistryManager.addServletContribution(alias, wrapper, initparams, applicationId, extension.getContributor())) {
				if (HttpRegistryDebug.extensionRegistration) {
					LOG.debug("Successfully added servlet {} to application {} (contributed by {}).", new Object[] { alias, applicationId, servletElement.getContributor() });
				}
				registered.add(servletElement);
			} else {
				if (HttpRegistryDebug.extensionRegistration) {
					LOG.debug("Did not add servlet {} to application {} (contributed by {}).", new Object[] { alias, applicationId, servletElement.getContributor() });
				}
			}
		}
	}

	public void removed(final IExtension extension) {
		final IConfigurationElement[] elements = extension.getConfigurationElements();
		for (int i = 0; i < elements.length; i++) {
			final IConfigurationElement servletElement = elements[i];
			if (!SERVLET.equals(servletElement.getName())) {
				continue;
			}

			final String alias = servletElement.getAttribute(ALIAS);
			if (alias == null) {
				LOG.warn("Ignoring mount extension element contributed by {}. Does not contain an alias.", servletElement.getContributor());
				continue; // alias is mandatory - ignore this.
			}

			final String applicationId = servletElement.getAttribute(APPLICATION_ID);
			if (applicationId == null) {
				LOG.warn("Ignoring mount extension element contributed by {}. Does not contain an application id.", servletElement.getContributor());
				continue; // alias is mandatory - ignore this.
			}

			if (HttpRegistryDebug.extensionRegistration) {
				LOG.debug("Trying to remove servlet {} to application {} (contributed by {}).", new Object[] { alias, applicationId, servletElement.getContributor() });
			}
			if (registered.remove(servletElement)) {
				if (HttpRegistryDebug.extensionRegistration) {
					LOG.debug("Successfully removed servlet {} to application {} (contributed by {}).", new Object[] { alias, applicationId, servletElement.getContributor() });
				}
				httpRegistryManager.removeContribution(alias, applicationId);
			} else {
				if (HttpRegistryDebug.extensionRegistration) {
					LOG.debug("Did not remove servlet {} to application {} (contributed by {}).", new Object[] { alias, applicationId, servletElement.getContributor() });
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
