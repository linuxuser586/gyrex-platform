/*******************************************************************************
 * Copyright (c) 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.logback.config.internal;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;

import org.osgi.framework.BundleContext;

public class LogbackConfigActivator extends BaseBundleActivator {

	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.logback.config";
	private static LogbackConfigActivator instance;

	/**
	 * Returns the instance.
	 * 
	 * @return the instance
	 */
	public static LogbackConfigActivator getInstance() {
		return instance;
	}

	private AppenderProviderRegistry appenderProviderRegistry;

	/**
	 * Creates a new instance.
	 */
	public LogbackConfigActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instance = this;
	}

	@Override
	protected synchronized void doStop(final BundleContext context) throws Exception {
		instance = null;

		if (appenderProviderRegistry != null) {
			appenderProviderRegistry.close();
			appenderProviderRegistry = null;
		}
	}

	/**
	 * Returns the shared registry instance.
	 * <p>
	 * Note, the registry is lazily initialized. This call may block during
	 * initialization.
	 * </p>
	 * 
	 * @return the registry
	 */
	public AppenderProviderRegistry getAppenderProviderRegistry() {
		AppenderProviderRegistry registry = appenderProviderRegistry;
		if (null == registry) {
			synchronized (this) {
				if (appenderProviderRegistry != null)
					return appenderProviderRegistry;

				if (!isActive())
					throw createBundleInactiveException();

				// start the object provider registry
				registry = appenderProviderRegistry = new AppenderProviderRegistry(getBundle().getBundleContext());
				registry.open();
			}
		}

		return registry;
	}

	@Override
	protected Class getDebugOptions() {
		return LogbackConfigDebug.class;
	}
}
