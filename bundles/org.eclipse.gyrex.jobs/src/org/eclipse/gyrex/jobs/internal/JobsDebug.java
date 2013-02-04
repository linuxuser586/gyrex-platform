/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal;

import org.eclipse.gyrex.common.debug.BundleDebugOptions;

/**
 * debug options
 */
public class JobsDebug extends BundleDebugOptions {

	public static boolean debug;
	public static boolean providerRegistry;
	public static boolean workerEngine;
	public static boolean schedulerEngine;

	public static boolean jobLocks;
	public static boolean cleanup;

}
