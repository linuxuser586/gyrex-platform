/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.jetty.internal.app;

import javax.servlet.Servlet;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.http.application.context.IApplicationContext;

import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkUtil;

/**
 * Optimized ServletHolder for servlets coming from {@link IApplicationContext}
 * registrations
 */
@SuppressWarnings("restriction")
public class ApplicationRegisteredServletHolder extends ServletHolder implements BundleListener {

	public ApplicationRegisteredServletHolder() {
		super();
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param servlet
	 */
	public ApplicationRegisteredServletHolder(final Class servlet) {
		super(servlet);
		setName(servlet.getName() + "-" + super.hashCode());
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param servlet
	 */
	public ApplicationRegisteredServletHolder(final Servlet servlet) {
		super(servlet);
		setName(servlet.getClass().getName() + "-" + super.hashCode());
	}

	@Override
	public void bundleChanged(final BundleEvent event) {
		if (!isStarted()) {
			return;
		}
		if (event.getType() == Bundle.STOPPING) {
			try {
				// remove registration
				getApplicationServletHandler().removeServlet(this);
				// stop
				doStop();
			} catch (final Exception e) {
				// ignore
				Log.ignore(e);
			} finally {
				try {
				} catch (final Exception e) {
					Log.ignore(e);
				}
			}
		}
	}

	@Override
	public void doStart() throws Exception {
		// super start
		super.doStart();

		// hook bundle listener
		final Bundle bundle = FrameworkUtil.getBundle(getServlet().getClass());
		if (null != bundle) {
			final BundleContext bundleContext = bundle.getBundleContext();
			if (null != bundleContext) {
				bundleContext.addBundleListener(new BundleListener() {
					@Override
					public void bundleChanged(final BundleEvent event) {
						if (event.getType() == Bundle.STOPPING) {
							try {
								final BundleContext bundleContext = event.getBundle().getBundleContext();
								if (null != bundleContext) {
									bundleContext.removeBundleListener(this);
								}
							} catch (final Exception e) {
								// ignore
								Log.ignore(e);
							}
						}
					}
				});
			}
		}
	}

	@Override
	public void doStop() throws Exception {
		// remove listener
		final Bundle bundle = FrameworkUtil.getBundle(getServlet().getClass());
		if (null != bundle) {
			final BundleContext bundleContext = bundle.getBundleContext();
			if (null != bundleContext) {
				bundleContext.removeBundleListener(this);
			}
		}

		// stop
		super.doStop();
	}

	private ApplicationServletHandler getApplicationServletHandler() {
		return (ApplicationServletHandler) getServletHandler();
	}

	@Override
	public synchronized Servlet newInstance() throws InstantiationException, IllegalAccessException {
		final Class<? extends Servlet> heldClass = getHeldClass();
		if (null == heldClass) {
			throw new InstantiationException("no held class: " + getClassName());
		}
		final IRuntimeContext context = getApplicationServletHandler().applicationContextHandler.application.getContext();
		try {
			return context.getInjector().make(heldClass);
		} catch (final InjectionException e) {
			final InstantiationException instantiationException = new InstantiationException("error injecting class: " + e.getMessage());
			instantiationException.initCause(e);
			throw instantiationException;
		}
	}

}
