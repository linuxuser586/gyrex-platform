/**
 * Copyright (c) 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.log.frameworklogadapter.internal;

import org.eclipse.osgi.baseadaptor.HookConfigurator;
import org.eclipse.osgi.baseadaptor.HookRegistry;

/**
 * Adds the {@link FrameworkLogAdapterHook} to the {@link HookRegistry}.
 */
public class FrameworkLogAdapterConfigurator implements HookConfigurator {

	@Override
	public void addHooks(final HookRegistry hookRegistry) {
		hookRegistry.addAdaptorHook(FrameworkLogAdapterHook.getInstance());
	}

}
