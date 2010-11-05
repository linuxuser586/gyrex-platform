/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.server.internal.opsmode;

/**
 * The platform operation mode.
 * <p>
 * Gyrex uses the concept of operation modes to behave differently in a secure
 * production environment and in a relaxed development environment.
 * </p>
 * <p>
 * For example, in a production environment it is not desired to confront web
 * site visitors with cryptic error messages and exception stack traces which
 * may even contain sensitive data. In contrast, at development time is
 * sometimes much more convenient to output additional debugging information.
 * Additionally, it's sometimes better to use different default values in
 * production than for development.
 * </p>
 * <p>
 * Note, although Gyrex is a dynamic platform, the operation mode is static
 * information. It is not anticipated that the platform changes its operation
 * mode. Once set it should be <strong>assumed for lifetime</strong>. A new
 * installation has to be made to rebuild a system using a different operation
 * mode. Security and a clean environment are some of the reasons for such a
 * strict decision.
 * </p>
 */
public enum OperationMode {

	/**
	 * <strong>DEVELOPMENT</strong> operation mode
	 */
	DEVELOPMENT,

	/** <strong>PRODUCTION</strong> operation mode */
	PRODUCTION;

	/**
	 * Returns the {@link OperationMode} for a string.
	 * 
	 * @param mode
	 *            the mode (can be lower case)
	 * @return the operation mode (may be <code>null</code> if <code>mode</code>
	 *         was <code>null</code>)
	 * @throws IllegalArgumentException
	 *             if the specified mode is neither {@link #DEVELOPMENT} nor
	 *             {@link #PRODUCTION}
	 */
	public static OperationMode fromString(final String mode) throws IllegalArgumentException {
		if (null == mode) {
			return null;
		}
		try {
			return Enum.valueOf(OperationMode.class, mode.toUpperCase());
		} catch (final IllegalArgumentException e) {
			throw new IllegalArgumentException("The operation mode '" + mode + "' is invalid.");
		}
	}

}