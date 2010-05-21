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
package org.eclipse.gyrex.common.logging;

/**
 * Tags for classifying log messages based on the target audience.
 */
public enum LogAudience implements LogTag {

	/** intended for developers of the system */
	DEVELOPER,

	/** intended for users of the system */
	USER,

	/** intended for system administrators */
	ADMIN,
}
