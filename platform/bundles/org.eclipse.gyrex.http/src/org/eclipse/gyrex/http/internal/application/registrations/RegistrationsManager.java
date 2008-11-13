/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Cognos Incorporated, IBM Corporation - concept/implementation from 
 *                                            org.eclipse.equinox.http.servlet
 *     Gunnar Wagenknecht - adaption to CloudFree
 *******************************************************************************/
package org.eclipse.cloudfree.http.internal.application.registrations;

import java.security.AccessController;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.eclipse.cloudfree.common.lifecycle.IShutdownParticipant;
import org.eclipse.cloudfree.http.application.servicesupport.IResourceProvider;
import org.eclipse.cloudfree.http.application.servicesupport.NamespaceException;
import org.eclipse.cloudfree.http.internal.HttpActivator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

/**
 * Coordinates application {@link Registration registrations}.
 */
public class RegistrationsManager implements SynchronousBundleListener, IShutdownParticipant {

	/**
	 * servlet and resource registrations (modify protected by
	 * {@link #registrationLock})
	 */
	private final ConcurrentMap<String, Registration> registrations = new ConcurrentHashMap<String, Registration>(1);

	/**
	 * bundles with the registrations (modify protected by
	 * {@link #registrationLock})
	 */
	private final ConcurrentMap<Bundle, Set<Registration>> registrationsByBundle = new ConcurrentHashMap<Bundle, Set<Registration>>();

	/** registered servlets (fully protected by {@link #registrationLock}) */
	private final Set<Servlet> servlets = new HashSet<Servlet>(1);

	/** servlet and resource registration lock */
	private final Lock registrationLock = new ReentrantLock();

	private final AtomicBoolean active = new AtomicBoolean(false);

	/* (non-Javadoc)
	 * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
	 */
	@Override
	public void bundleChanged(final BundleEvent event) {
		if (event.getType() == BundleEvent.STOPPED) {
			final Bundle bundle = event.getBundle();

			// the bundle has been STOPPED
			// the event is processed synchronous (we are a SynchronousBundleListener)
			// therefor, it's safe to assume that no one starts the same bundle again as long as we are running
			// thus, it's ok to remove all registrations of the bundle

			// unregister all bundles registrations
			final Set<Registration> bundleRegistrations = registrationsByBundle.get(bundle);
			if ((null != bundleRegistrations) && registrationsByBundle.remove(bundle, bundleRegistrations)) {
				for (final Registration registration : bundleRegistrations) {
					registration.unregister(false);
				}
			}
		}
	}

	private void checkActive() {
		if (!active.get()) {
			throw new IllegalStateException("inactive");
		}

	}

	/**
	 * Checks the alias.
	 * 
	 * @param alias
	 * @throws NamespaceException
	 *             if an alias is already registered
	 */
	private void checkAlias(final String alias) throws NamespaceException {
		if (null == alias) {
			throw new IllegalArgumentException("alias must not be null");
		}

		// empty alias is not allowed
		if (alias.length() == 0) {
			throw new IllegalArgumentException("alias must not be empty");
		}

		// must start with /
		if (alias.charAt(0) != '/') {
			throw new IllegalArgumentException("alias '" + alias + "' must start with '/'");
		}

		// must not end with / (except the rool alias)
		if ((alias.length() > 1) && (alias.charAt(alias.length() - 1) == '/')) {
			throw new IllegalArgumentException("alias '" + alias + "' must not end with '/'");
		}

		// check alias is not registered
		if (registrations.containsKey(alias)) {
			throw new NamespaceException(alias);
		}
	}

	/**
	 * Destroys the manager instance and clears out all registrations.
	 * <p>
	 * This method is safe to be called multiple times.
	 * </p>
	 */
	public void destory() {
		if (!active.get()) {
			return;
		}
		if (active.compareAndSet(true, false)) {
			// destroy succeeded
			HttpActivator.getInstance().removeShutdownParticipant(this);
			HttpActivator.getInstance().getBundle().getBundleContext().removeBundleListener(this);
			registrations.clear();
			registrationsByBundle.clear();
		}
	}

	/**
	 * Returns the registration for the specified alias.
	 * 
	 * @param alias
	 *            the alias
	 * @return the registration (maybe <code>null</code>)
	 */
	public Registration get(final String alias) {
		return registrations.get(alias);
	}

	/**
	 * Initializes the registrations manager.
	 * <p>
	 * This method is safe to be called multiple times.
	 * </p>
	 */
	public void init() {
		if (active.get()) {
			return;
		}

		if (active.compareAndSet(false, true)) {
			// activation succeeded
			HttpActivator.getInstance().addShutdownParticipant(this);
			HttpActivator.getInstance().getBundle().getBundleContext().addBundleListener(this);
		}
	}

	/**
	 * Implementation of InternalApplication#registerResource.
	 */
	public void registerResource(String alias, final String name, final IResourceProvider provider, final ServletContext servletContext) throws NamespaceException {
		checkActive();
		// check alias
		checkAlias(alias);
		alias = alias.intern();

		if (null == name) {
			throw new IllegalArgumentException("name must not be null");
		}
		if (null == provider) {
			throw new IllegalArgumentException("provider must not be null");
		}
		final Bundle providerBundle = HttpActivator.getInstance().getBundleId(provider);
		if (null == providerBundle) {
			throw new IllegalArgumentException("provider must be created by an OSGi bundle");
		}

		// synchronize on registration lock
		final Lock lock = registrationLock;
		lock.lock();
		try {
			// check not registered
			if (registrations.containsKey(alias)) {
				throw new NamespaceException(alias);
			}

			// create resource registration
			final ResourceRegistration registration = new ResourceRegistration(alias, name, provider, servletContext, this, providerBundle, AccessController.getContext());

			// remember registration
			registrations.put(alias, registration);

			// remember registration for provider bundle
			if (!registrationsByBundle.containsKey(providerBundle)) {
				registrationsByBundle.put(providerBundle, new HashSet<Registration>(1));
			}
			registrationsByBundle.get(providerBundle).add(registration);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Implementation of InternalApplication#registerServlet.
	 */
	public void registerServlet(String alias, final Servlet servlet, final ServletConfig config) throws NamespaceException, ServletException {
		checkActive();

		// check alias
		checkAlias(alias);
		alias = alias.intern();

		if (null == servlet) {
			throw new IllegalArgumentException("servlet must not be null");
		}
		final Bundle servletBundle = HttpActivator.getInstance().getBundleId(servlet);
		if (null == servletBundle) {
			throw new IllegalArgumentException("servlet must be created by an OSGi bundle");
		}

		// the registration
		ServletRegistration registration;

		// synchronize on registration lock
		final Lock lock = registrationLock;
		lock.lock();
		try {
			// check not registered
			if (registrations.containsKey(alias)) {
				throw new NamespaceException(alias);
			}

			// check servlet not registered twice
			if (servlets.contains(servlet)) {
				throw new IllegalArgumentException("servlet instance already registered for a different alias but must not be registered twice");
			}

			// create servlet registration
			registration = new ServletRegistration(alias, servlet, this, servletBundle);

			// remember registration
			registrations.put(alias, registration);

			// remember registration for servlet bundle
			if (!registrationsByBundle.containsKey(servletBundle)) {
				registrationsByBundle.put(servletBundle, new HashSet<Registration>(1));
			}
			registrationsByBundle.get(servletBundle).add(registration);
		} finally {
			lock.unlock();
		}

		// initialize the servlet (outside the lock)
		try {
			registration.initServlet(config);
		} catch (final ServletException e) {
			// undo the registration process
			try {
				unregister(alias, false, servletBundle);
			} catch (final Exception ignored) {
				// ignore
			}
			throw e;
		}

	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.common.lifecycle.IShutdownParticipant#shutdown()
	 */
	@Override
	public void shutdown() throws Exception {
		destory();
	}

	/**
	 * Implementation of InternalApplication#unregister
	 * 
	 * @param callingBundle
	 */
	public void unregister(final String alias, final boolean destroy, final Bundle callingBundle) {
		checkActive();
		if (null == alias) {
			throw new IllegalArgumentException("alias must not be null");
		}

		// check not registered
		if (!registrations.containsKey(alias)) {
			// TODO: the spec requires this ... but do we need this? 
			throw new IllegalArgumentException("alias '" + alias + "' not registered");
		}

		// synchronize on registration lock
		final Lock lock = registrationLock;
		lock.lock();
		try {
			// get the registration
			final Registration registration = registrations.remove(alias);

			// check we have one
			if (null == registration) {
				// TODO: the spec requires this ... but do we need this? 
				throw new IllegalArgumentException("alias '" + alias + "' not registered");
			}

			// verify the caller is the registerer
			if (!registration.getBundle().equals(callingBundle)) {
				// the spec requires this 
				throw new IllegalArgumentException("alias '" + alias + "' was not registered by bundle '" + callingBundle.getSymbolicName() + "' (" + callingBundle + ")");
			}

			// remove the registration
			registrations.remove(alias);

			// remove the registration for bundle
			final Set<Registration> bundleRegistrations = registrationsByBundle.get(registration.getBundle());
			if (null != bundleRegistrations) {
				bundleRegistrations.remove(registration);
			}

			// destroy registration
			if (destroy) {
				registration.destroy();
			}

			// close
			registration.close();
		} finally {
			lock.unlock();
		}
	}

}
