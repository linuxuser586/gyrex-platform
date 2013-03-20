/*******************************************************************************
 * Copyright (c) 2009, 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.context.definitions.IRuntimeContextDefinitionManager;
import org.eclipse.gyrex.context.internal.manager.ContextManagerImpl;
import org.eclipse.gyrex.context.internal.provider.ObjectProviderRegistry;
import org.eclipse.gyrex.context.internal.registry.ContextRegistryImpl;
import org.eclipse.gyrex.context.manager.IRuntimeContextManager;
import org.eclipse.gyrex.context.registry.IRuntimeContextRegistry;

import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.osgi.util.NLS;

import org.osgi.framework.BundleContext;

public class ContextActivator extends BaseBundleActivator {

	/** SYMBOLIC_NAME */
	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.context";

	private static final AtomicReference<ContextActivator> instanceRef = new AtomicReference<ContextActivator>();

	public static ContextActivator getInstance() {
		final ContextActivator contextActivator = instanceRef.get();
		if (null == contextActivator)
			throw new IllegalStateException(NLS.bind("The Gyrex contextual runtime bundle {0} is inactive.", SYMBOLIC_NAME));
		return contextActivator;
	}

	private volatile ContextRegistryImpl contextRegistry;
	private volatile ObjectProviderRegistry objectProviderRegistry;

	public ContextActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instanceRef.set(this);

		// start the context registry
		contextRegistry = new ContextRegistryImpl();
		getServiceHelper().registerService(new String[] { IRuntimeContextRegistry.class.getName(), IRuntimeContextDefinitionManager.class.getName() }, contextRegistry, "Eclipse.org Gyrex", "Eclipse Gyrex Contextual Runtime Registry & Definition Manager", null, null);

		// start the context manager
		final ContextManagerImpl contextManager = new ContextManagerImpl(contextRegistry);
		getServiceHelper().registerService(IRuntimeContextManager.class.getName(), contextManager, "Eclipse.org Gyrex", "Eclipse Gyrex Contextual Runtime Manager", null, null);
		addShutdownParticipant(contextManager);
	}

	@Override
	protected synchronized void doStop(final BundleContext context) throws Exception {
		instanceRef.set(null);

		if (contextRegistry != null) {
			contextRegistry.close();
			contextRegistry = null;
		}

		if (objectProviderRegistry != null) {
			objectProviderRegistry.close();
			objectProviderRegistry = null;
		}
	}

	public ContextRegistryImpl getContextRegistryImpl() {
		final ContextRegistryImpl registry = contextRegistry;
		if (registry == null)
			throw createBundleInactiveException();
		return registry;
	}

	@Override
	protected Class getDebugOptions() {
		return ContextDebug.class;
	}

	public ObjectProviderRegistry getObjectProviderRegistry() {
		ObjectProviderRegistry registry = objectProviderRegistry;
		if (registry == null) {
			synchronized (this) {
				if (objectProviderRegistry != null)
					return objectProviderRegistry;

				if (!isActive())
					throw createBundleInactiveException();

				// start the object provider registry
				registry = objectProviderRegistry = new ObjectProviderRegistry();
				registry.start(getBundle().getBundleContext());
			}
		}
		return registry;
	}

	public IPreferencesService getPreferencesService() {
		return getServiceHelper().trackService(IPreferencesService.class).getService();
	}
}
