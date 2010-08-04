/**
 * Copyright (c) 2009, 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.context.internal.preferences;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.gyrex.context.preferences.IRuntimeContextPreferences;
import org.eclipse.gyrex.context.provider.RuntimeContextObjectProvider;

/**
 * Provider for {@link IRuntimeContextPreferences}.
 */
public class GyrexContextPreferencesProvider extends RuntimeContextObjectProvider {

	public GyrexContextPreferencesProvider() {
		final Map<Class<?>, Class<?>> config = new HashMap<Class<?>, Class<?>>();
		config.put(IRuntimeContextPreferences.class, GyrexContextPreferencesImpl.class);
		configureObjectTypes(config);
	}
}
