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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.context.IApplicationContext;
import org.eclipse.gyrex.http.internal.HttpActivator;
import org.eclipse.gyrex.http.internal.HttpDebug;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A registered {@link Application}.
 */
public class ApplicationRegistration {

	public static interface DestroyListener {
		void applicationDestroyed(ApplicationRegistration registration);
	}

	private static final Logger LOG = LoggerFactory.getLogger(ApplicationRegistration.class);
	private static final Map<String, String> NO_INIT_PROPERTIES = Collections.emptyMap();

	private final String applicationId;
	private final String providerId;
	private final IRuntimeContext context;
	private final ConcurrentMap<IApplicationContext, ApplicationInstance> activeApplications = new ConcurrentHashMap<IApplicationContext, ApplicationInstance>(1);

	private final Lock applicationCreationLock = new ReentrantLock();
	private final ApplicationManager manager;

	private final List<DestroyListener> destoryListeners = new CopyOnWriteArrayList<ApplicationRegistration.DestroyListener>();

	/**
	 * Creates a new instance.
	 * 
	 * @param applicationId
	 * @param providerId
	 * @param context
	 * @param initProperties
	 */
	public ApplicationRegistration(final String applicationId, final String providerId, final IRuntimeContext context, final ApplicationManager manager) {
		this.applicationId = applicationId;
		this.providerId = providerId;
		this.context = context;
		this.manager = manager;
	}

	public void addDestroyListener(final DestroyListener listener) {
		destoryListeners.add(listener);
	}

	/**
	 * Destroys all created application instances.
	 * <p>
	 * Note, this is called by the provider after it has been removed from the
	 * registry.
	 * </p>
	 */
	void destroy() {
		// Note, there is a little concurrency gap here.
		// But it should be safe because the provider is
		// already gone at this time. Thus, now new
		// applications should arrive.
		for (final ApplicationInstance instance : activeApplications.values()) {
			instance.destroy();
		}
		activeApplications.clear();

		for (final DestroyListener l : destoryListeners) {
			try {
				l.applicationDestroyed(this);
			} catch (final Exception e) {
				// ignore
			}
		}
		destoryListeners.clear();
	}

	/**
	 * Destroy an existing application instance for the specified application
	 * context if one exists.
	 * <p>
	 * This method ensures that any created instance for the specified context
	 * is properly destroyed.
	 * </p>
	 * 
	 * @param applicationContext
	 *            the application context
	 */
	public void destroyApplication(final IApplicationContext applicationContext) {
		// remove the instance
		// do this inside the lock to prevent parallel creation
		ApplicationInstance instance;
		final Lock lock = applicationCreationLock;
		lock.lock();
		try {
			instance = activeApplications.remove(applicationContext);
		} finally {
			lock.unlock();
		}

		// destroy
		if (null != instance) {
			instance.destroy();
		}
	}

	/**
	 * Gets an existing or creates a new application instance for the specified
	 * application context.
	 * <p>
	 * This method ensures that at most one application instance is created for
	 * the specified context.
	 * </p>
	 * <p>
	 * Note, the application will not be initialized. The caller is responsible
	 * for initializing the application (calling
	 * {@link ApplicationInstance#initialize()}) when appropriate. This allows
	 * to implement a more flexible lifecycle.
	 * </p>
	 * 
	 * @param applicationContext
	 *            the application context
	 * @return an application instance
	 */
	public ApplicationInstance getApplication(final IApplicationContext applicationContext) throws Exception {
		// get application
		ApplicationInstance instance = activeApplications.get(applicationContext);
		if (null != instance) {
			return instance;
		}

		if (HttpDebug.applicationLifecycle) {
			LOG.debug("Creating new application of type {} for context {}", providerId, applicationContext);
		}

		// TODO: should support multiple provider versions somehow
		// (maybe through the context which defines which version to use?)
		// (another alternative would be version ranges at registration times)
		final ApplicationProviderRegistration providerRegistration = HttpActivator.getInstance().getProviderRegistry().getProviderRegistration(getProviderId());
		if (null == providerRegistration) {
			return null;
		}

		// we must ensure that only *one* application instance is
		// created per application context
		final Lock lock = applicationCreationLock;
		lock.lock();
		try {
			instance = activeApplications.get(applicationContext);
			if (null != instance) {
				return instance;
			}

			// create the application
			final Application application = providerRegistration.createApplication(this);
			if (null == application) {
				// provider might be destroyed meanwhile
				return null;
			}

			// remember the instance
			instance = new ApplicationInstance(application, applicationContext);
			activeApplications.put(applicationContext, instance);
		} finally {
			lock.unlock();
		}

		return instance;
	}

	/**
	 * Returns the applicationId.
	 * 
	 * @return the applicationId
	 */
	public String getApplicationId() {
		return applicationId;
	}

	/**
	 * Returns the context.
	 * 
	 * @return the context
	 */
	public IRuntimeContext getContext() {
		return context;
	}

	/**
	 * Returns the initProperties.
	 * 
	 * @return the initProperties
	 */
	public Map<String, String> getInitProperties() {
		// dynamically load from manager
		final Map<String, String> p = manager.getProperties(applicationId);
		if (null != p) {
			return p;
		}
		return NO_INIT_PROPERTIES;
	}

	/**
	 * Returns the providerId.
	 * 
	 * @return the providerId
	 */
	public String getProviderId() {
		return providerId;
	}

	public void removeDestroyListener(final DestroyListener listener) {
		destoryListeners.remove(listener);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ApplicationRegistration [applicationId=").append(applicationId).append(", providerId=").append(providerId).append(", context=").append(context.getContextPath()).append(", activeApplications=").append(activeApplications.size()).append("]");
		return builder.toString();
	}

}
