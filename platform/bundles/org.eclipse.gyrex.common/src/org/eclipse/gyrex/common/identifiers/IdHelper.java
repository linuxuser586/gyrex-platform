/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.common.identifiers;

/**
 * Helper utility for working with public identifiers used in Gyrex.
 * 
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class IdHelper {
	/**
	 * Indicates if the specified id is a valid Gyrex API id.
	 * <p>
	 * By definition, a all identifiers used within the Gyrex API must not be
	 * <code>null</code> or the empty string and may only contain the following
	 * printable ASCII characters.
	 * <ul>
	 * <li>lower- and uppercase letters <code>a..z</code> and <code>A..Z</code></li>
	 * <li>numbers <code>0..9</code></li>
	 * <li><code>'.'</code></li>
	 * <li><code>'-'</code></li>
	 * <li><code>'_'</code></li>
	 * </ul>
	 * </p>
	 * <p>
	 * This method should be used to validate identifiers within the Gyrex API.
	 * </p>
	 * 
	 * @param id
	 *            the id
	 * @return <code>true</code> if the id is valid, <code>false</code>
	 *         otherwise
	 */
	public static boolean isValidId(final String id) {
		if (null == id) {
			return false;
		}

		if (id.equals("")) {
			return false;
		}

		// verify chars
		for (int i = 0; i < id.length(); i++) {
			final char c = id.charAt(i);
			if (((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')) || ((c >= '0') && (c <= '9')) || (c == '.') || (c == '_') || (c == '-')) {
				continue;
			} else {
				return false;
			}
		}

		return true;
	}

	private IdHelper() {
		// empty
	}
}
