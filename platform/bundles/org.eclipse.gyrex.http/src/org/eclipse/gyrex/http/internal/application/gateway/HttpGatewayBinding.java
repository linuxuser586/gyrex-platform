/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.internal.application.gateway;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.gyrex.http.internal.HttpAppManagerApplication;
import org.eclipse.gyrex.http.internal.HttpDebug;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationManager;
import org.eclipse.gyrex.http.internal.application.manager.ApplicationRegistration;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.util.tracker.ServiceTracker;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The binding ensures that each {@link IHttpGateway} has all active
 * applications mounted.
 */
public class HttpGatewayBinding extends ServiceTracker<IHttpGateway, IHttpGateway> {

	private static final Logger LOG = LoggerFactory.getLogger(HttpGatewayBinding.class);

	private final Lock bindingModificationLock = new ReentrantLock();
	private final ConcurrentMap<String, ApplicationRegistration> applicationsById = new ConcurrentHashMap<String, ApplicationRegistration>(1);

	/** listens for new or removed applications */
	final INodeChangeListener applicationsListener = new INodeChangeListener() {

		@Override
		public void added(final NodeChangeEvent event) {
			if (isEmpty()) {
				return;
			}

			final IEclipsePreferences appNode = (IEclipsePreferences) event.getChild();
			final String applicationId = appNode.name();
			if (HttpDebug.gatewayBinding) {
				LOG.debug("Processing added application {}", applicationId);
			}

			// add activation/deactivation monitor
			appNode.addPreferenceChangeListener(applicationActiveListener);

			// register if active
			if (appNode.getBoolean(ApplicationManager.KEY_ACTIVE, true)) {
				// register
				mountApp(applicationId);
			}
		}

		@Override
		public void removed(final NodeChangeEvent event) {
			if (isEmpty()) {
				return;
			}

			final String applicationId = event.getChild().name();
			if (HttpDebug.gatewayBinding) {
				LOG.debug("Processing unregistered application {}", applicationId);
			}

			// (listeners should get removed automatically, we can call only #name anyway)

			// unregister
			unmountApp(applicationId);
		}
	};

	/** listens for application activation/deactivation events */
	final IPreferenceChangeListener applicationActiveListener = new IPreferenceChangeListener() {
		@Override
		public void preferenceChange(final PreferenceChangeEvent event) {
			if (isEmpty()) {
				return;
			}

			if (ApplicationManager.KEY_ACTIVE.equals(event.getKey())) {
				final String applicationId = event.getNode().name();
				if (HttpDebug.gatewayBinding) {
					LOG.debug("Processing activation change event for application {}", applicationId);
				}

				final boolean active = Boolean.TRUE.equals(event.getNewValue());
				if (active) {
					mountApp(applicationId);
				} else {
					unmountApp(applicationId);
				}
			}
		}
	};

	/** listens for url mount events */
	final IPreferenceChangeListener urlMountListener = new IPreferenceChangeListener() {
		@Override
		public void preferenceChange(final PreferenceChangeEvent event) {
			if (isEmpty()) {
				return;
			}

			final String url = event.getKey();
			try {
				final String unmountedApplicationId = (String) event.getOldValue();
				final String mountedApplicationId = (String) event.getNewValue();

				// unmount first
				if (null != unmountedApplicationId) {
					if (HttpDebug.gatewayBinding) {
						LOG.debug("Unmounting application {} from url {}", unmountedApplicationId);
					}
					unmountUrl(url, unmountedApplicationId);
				}

				// mount first
				if (null != mountedApplicationId) {
					if (HttpDebug.gatewayBinding) {
						LOG.debug("Mounting application {} at url {}", unmountedApplicationId);
					}
					mountUrl(url, mountedApplicationId);
				}
			} catch (final ClassCastException e) {
				LOG.warn("Invalid preference value for url {} at node {}. {}", new Object[] { url, event.getNode(), ExceptionUtils.getRootCauseMessage(e) });
			}
		}
	};

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 */
	public HttpGatewayBinding(final BundleContext context) {
		super(context, IHttpGateway.class, null);
	}

	@Override
	public IHttpGateway addingService(final ServiceReference<IHttpGateway> reference) {
		// get service
		final IHttpGateway gateway = super.addingService(reference);
		if (HttpDebug.gatewayBinding) {
			LOG.debug("New HTTP gateway {}: {}", gateway.getName(), gateway);
		}

		// register all existing applications
		try {
			final IEclipsePreferences applicationsNode = ApplicationManager.getAppsNode();
			final String[] names = applicationsNode.childrenNames();
			for (final String applicationId : names) {
				final IEclipsePreferences appNode = (IEclipsePreferences) applicationsNode.node(applicationId);

				// register listener
				appNode.addPreferenceChangeListener(applicationActiveListener);

				// register if active
				if (appNode.getBoolean(ApplicationManager.KEY_ACTIVE, true)) {
					mountAppAtGatway(applicationId, gateway);
				}
			}
		} catch (final BackingStoreException e) {
			// TODO make this a background job that retries periodically if failed
			LOG.error("Unable to read existing applications. {}", ExceptionUtils.getRootCauseMessage(e), e);
		}

		return gateway;
	}

	@Override
	public void close() {
		// close
		super.close();

		// remove preference listener
		try {
			ApplicationManager.getUrlsNode().removePreferenceChangeListener(urlMountListener);
			final IEclipsePreferences applicationsNode = ApplicationManager.getAppsNode();
			applicationsNode.removeNodeChangeListener(applicationsListener);
			final String[] childrenNames = applicationsNode.childrenNames();
			for (final String appId : childrenNames) {
				((IEclipsePreferences) applicationsNode.node(appId)).removePreferenceChangeListener(applicationActiveListener);
			}
		} catch (final BackingStoreException e) {
			LOG.warn("Error removing change listeners. Memory may be leaked. {}", ExceptionUtils.getRootCauseMessage(e));
		}
	}

	/**
	 * Returns the registration for the specified application id.
	 * 
	 * @param applicationId
	 *            the application id
	 * @return the application registration, or <code>null</code> if no
	 *         application is registered (or the registration has been removed)
	 */
	public ApplicationRegistration getApplicationRegistration(final String applicationId) {
		ApplicationRegistration applicationRegistration = applicationsById.get(applicationId);
		if (null == applicationRegistration) {
			applicationsById.putIfAbsent(applicationId, HttpAppManagerApplication.getInstance().getApplicationManager().getApplicationRegistration(applicationId));
			applicationRegistration = applicationsById.get(applicationId);
		}
		return applicationRegistration;
	}

	/**
	 * Mounts all URLs registered for an application.
	 * 
	 * @param applicationId
	 */
	void mountApp(final String applicationId) {
		bindingModificationLock.lock();
		try {
			final Object[] services = getServices();
			if (null == services) {
				return;
			}

			for (int i = 0; i < services.length; i++) {
				final IHttpGateway gateway = (IHttpGateway) services[i];
				mountAppAtGatway(applicationId, gateway);
			}
		} catch (final Exception e) {
			LOG.error("Error registering application {}: {}", new Object[] { applicationId, ExceptionUtils.getRootCauseMessage(e), e });
		} finally {
			bindingModificationLock.unlock();
		}
	}

	private void mountAppAtGatway(final String applicationId, final IHttpGateway gateway) {
		bindingModificationLock.lock();
		try {
			if (HttpDebug.gatewayBinding) {
				LOG.debug("Registering application {} at {}", applicationId, gateway.getName());
			}

			// read all urls
			final IEclipsePreferences urlsNode = ApplicationManager.getUrlsNode();
			for (final String url : urlsNode.keys()) {
				if (StringUtils.equals(applicationId, urlsNode.get(url, null))) {
					mountUrlAtGateway(url, applicationId, gateway);
				}
			}

		} catch (final Exception e) {
			LOG.error("Error registering application {}: {}", new Object[] { applicationId, ExceptionUtils.getRootCauseMessage(e), e });
		} finally {
			bindingModificationLock.unlock();
		}
	}

	/**
	 * Mounts a single url
	 * 
	 * @param url
	 * @param unmountedApplicationId
	 */
	void mountUrl(final String url, final String applicationId) {
		bindingModificationLock.lock();
		try {
			final Object[] services = getServices();
			if (null == services) {
				return;
			}

			for (int i = 0; i < services.length; i++) {
				final IHttpGateway gateway = (IHttpGateway) services[i];
				mountUrlAtGateway(url, applicationId, gateway);
			}
		} catch (final Exception e) {
			LOG.error("Error registering application {}: {}", new Object[] { applicationId, ExceptionUtils.getRootCauseMessage(e), e });
		} finally {
			bindingModificationLock.unlock();
		}
	}

	private void mountUrlAtGateway(final String url, final String applicationId, final IHttpGateway gateway) {
		if (HttpDebug.gatewayBinding) {
			LOG.debug("Mounting url {} for application {} at {}", new Object[] { url, applicationId, gateway });
		}
		gateway.getUrlRegistry(this).registerIfAbsent(url, applicationId);
	}

	@Override
	public void open() {
		ApplicationManager.getAppsNode().addNodeChangeListener(applicationsListener);
		ApplicationManager.getUrlsNode().addPreferenceChangeListener(urlMountListener);

		// open tracker
		super.open();
	}

	@Override
	public void removedService(final ServiceReference<IHttpGateway> reference, final IHttpGateway gateway) {
		// if the gateway is removed we don't bother at all
		// we assume that the gateway does the proper cleanup
		if (HttpDebug.gatewayBinding) {
			LOG.debug("Removed HTTP gateway {}: {}", gateway.getName(), gateway);
		}

		// unget service
		super.removedService(reference, gateway);
	}

	/**
	 * Unmounts all
	 * 
	 * @param applicationId
	 */
	void unmountApp(final String applicationId) {
		final Object[] services = getServices();
		if (null == services) {
			return;
		}

		for (int i = 0; i < services.length; i++) {
			final IHttpGateway gateway = (IHttpGateway) services[i];
			unmountAppAtGatway(applicationId, gateway);
		}

	}

	private void unmountAppAtGatway(final String applicationId, final IHttpGateway gateway) {
		gateway.getUrlRegistry(this).applicationUnregistered(applicationId);
	}

	/**
	 * Unmounts a single url
	 * 
	 * @param url
	 * @param applicationId
	 */
	void unmountUrl(final String url, final String applicationId) {
		final Object[] services = getServices();
		if (null == services) {
			return;
		}

		for (int i = 0; i < services.length; i++) {
			final IHttpGateway gateway = (IHttpGateway) services[i];
			unmountUrlAtGatway(url, applicationId, gateway);
		}
	}

	private void unmountUrlAtGatway(final String url, final String applicationId, final IHttpGateway gateway) {
		gateway.getUrlRegistry(this).unregister(url);
	}

}
