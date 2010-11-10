/*******************************************************************************
 * Copyright (c) 2008, 2010 Gunnar Wagenknecht and others.
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
 * Tags for classifying log messages based on the log source.
 */
public enum LogSource implements LogTag {

	/** a platform message (eg., server infrastructure code) */
	PLATFORM,

	/** an application code related message (eg., business logic) */
	APPLICATION
}