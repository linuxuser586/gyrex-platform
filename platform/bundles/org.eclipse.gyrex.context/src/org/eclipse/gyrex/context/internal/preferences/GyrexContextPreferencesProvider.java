/**
 * Copyright (c) 2009 AGETO Service GmbH and others.
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

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.preferences.IRuntimeContextPreferences;
import org.eclipse.gyrex.context.provider.RuntimeContextObjectProvider;

/**
 * Provider for {@link IRuntimeContextPreferences}.
 */
public class GyrexContextPreferencesProvider extends RuntimeContextObjectProvider {

	private static final Class[] TYPES = new Class[] { IRuntimeContextPreferences.class };

	@Override
	public Object getObject(final Class type, final IRuntimeContext context) {
		if (IRuntimeContextPreferences.class.equals(type)) {
			return new GyrexContextPreferencesImpl(context);
		}
		return null;
	}

	@Override
	public Class[] getObjectTypes() {
		return TYPES;
	}

	@Override
	public void ungetObject(final Object object, final IRuntimeContext context) {
		if (object instanceof GyrexContextPreferencesImpl) {
			((GyrexContextPreferencesImpl) object).dispose();
		}
	}

}
