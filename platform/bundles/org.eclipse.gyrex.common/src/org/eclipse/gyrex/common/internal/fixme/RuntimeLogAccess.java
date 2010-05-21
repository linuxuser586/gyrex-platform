/*******************************************************************************
 * Copyright (c) 2008, 2009 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.common.internal.fixme;

import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.IStatus;

/**
 * This class provides access to the {@link RuntimeLog}.
 * 
 * @todo remove once there is a story for logging
 */
@SuppressWarnings("restriction")
public class RuntimeLogAccess {
	/**
	 * Notifies all listeners of the platform log.
	 */
	public static void log(final IStatus status) {
		RuntimeLog.log(status);
	}
}
