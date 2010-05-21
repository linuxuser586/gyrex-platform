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
 * Tags for classifying log messages based on the log importance.
 */
public enum LogImportance implements LogTag {

	/**
	 * a blocking entry (typically used to report problems which are preventing
	 * the system from functioning and must be addressed before continuing)
	 * <p>
	 * Examples:
	 * <ul>
	 * <li>a system service is missing a required dependency and cannot start</li>
	 * </p>
	 */
	BLOCKER,

	/**
	 * a critical entry (typically used to report problems which effect system
	 * functionality)
	 * <p>
	 * Examples:
	 * <ul>
	 * <li>a resource limit threshold is reached</li>
	 * <li>a service is missing a required dependency and cannot start</li>
	 * </p>
	 */
	CRITICAL,

	/**
	 * an error entry (typically used to report problems which could not be
	 * handle gracefully but which do not affect the overall system
	 * functionality)
	 * <p>
	 * Examples:
	 * <ul>
	 * <li>an operation did not finish successfully (the operation may be
	 * repeated with different input)</li>
	 * <li>a service was still unavailable after a few retries and can be
	 * considered down (the operation may be repeated at later times)</li>
	 * </p>
	 */
	ERROR,

	/**
	 * a warning entry (typically used to report conditions which may result
	 * into errors later but are handled gracefully for the time being)
	 * <p>
	 * Examples:
	 * <ul>
	 * <li>a service is not available (but the application will retry)</li>
	 * <li>a resource limit threshold is approached</li>
	 * </p>
	 */
	WARNING,

	/**
	 * an informational entry (typically used to report progress on certain
	 * activities or events within the system)
	 * <p>
	 * Examples:
	 * <ul>
	 * <li>a background operation finished</li>
	 * <li>a service is available (again)</li>
	 * </p>
	 */
	INFO

}
