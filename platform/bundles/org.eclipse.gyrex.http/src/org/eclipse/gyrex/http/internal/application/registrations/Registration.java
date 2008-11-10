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

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;

/**
 * Base class for a registration
 */
public abstract class Registration {

	private final String alias;
	private RegistrationsManager manager;
	private Bundle bundle;
	private final AtomicInteger referenceCount;
	private final AtomicBoolean stopped;

	/**
	 * Creates a new instance.
	 */
	public Registration(final String alias, final RegistrationsManager manager, final Bundle bundle) {
		this.alias = alias;
		this.manager = manager;
		this.bundle = bundle;
		referenceCount = new AtomicInteger(0);
		stopped = new AtomicBoolean(false);
	}

	/**
	 * Called by {@link RegistrationsManager#unregister(String, boolean)} to
	 * close the registration, i.e. release all resources.
	 */
	public final void close() {
		stopped.set(true);
		manager = null;
		bundle = null;
		doClose();
	}

	/**
	 * Called by {@link RegistrationsManager#unregister(String, boolean)} to
	 * destroy the registration.
	 */
	public final void destroy() {
		// set stopped
		stopped.set(true);

		// wait till all usages stopped (but not more than 30 seconds)
		final int maxWait = 30000;
		final long start = System.currentTimeMillis();
		while ((referenceCount.intValue() > 0) && (System.currentTimeMillis() - start < maxWait)) {
			try {
				Thread.sleep(1000);
			} catch (final InterruptedException e) {
				// restore interrupted state
				Thread.currentThread().interrupt();
			}
		}

		// destroy
		doDestroy();
	}

	protected abstract void doClose();

	protected abstract void doDestroy();

	protected abstract boolean doHandleRequest(final HttpServletRequest req, final HttpServletResponse resp, final String alias) throws ServletException, IOException;

	/**
	 * Returns the alias.
	 * 
	 * @return the alias
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * Returns the bundle which is responsible for the registration.
	 * 
	 * @return the bundle
	 */
	public Bundle getBundle() {
		return bundle;
	}

	/**
	 * Processes a request
	 * 
	 * @param req
	 * @param resp
	 * @param alias2
	 * @return
	 */
	public final boolean handleRequest(final HttpServletRequest req, final HttpServletResponse resp, final String alias) throws ServletException, IOException {
		if (stopped.get()) {
			return false;
		}

		referenceCount.incrementAndGet();
		try {
			return doHandleRequest(req, resp, alias);
		} finally {
			referenceCount.decrementAndGet();
		}
	}

	/**
	 * Unregisters a registration.
	 * <p>
	 * This method just calls
	 * {@link RegistrationsManager#unregister(String, boolean, Bundle))}.
	 * </p>
	 * 
	 * @param destroy
	 *            <code>true</code> if the registration backing resource may
	 *            explicitly be destroyed, i.e. it's safe to invoke methods on
	 *            it; <code>false</code> otherwise (i.e. automatic removal
	 *            during shutdown)
	 */
	void unregister(final boolean destroy) {
		if (null != manager) {
			manager.unregister(alias, destroy, getBundle());
		}
	}

}
