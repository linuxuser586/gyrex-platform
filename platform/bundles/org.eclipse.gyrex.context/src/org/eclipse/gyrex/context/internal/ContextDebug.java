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
package org.eclipse.gyrex.context.internal;

import org.eclipse.gyrex.common.debug.BundleDebugOptions;

/**
 *The debug options
 */
public class ContextDebug extends BundleDebugOptions {

	public static boolean debug;

	public static boolean preferencesLookup;
	public static boolean preferencesPut;

}
