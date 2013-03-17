/*******************************************************************************
 * Copyright (c) 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.p2.internal.packages;

import org.apache.commons.lang.StringUtils;

public enum InstallState {
	ROLLOUT, REVOKE, NONE;

	public static InstallState fromString(final String value) {
		if (StringUtils.isBlank(value))
			return NONE;
		switch (value) {
			case "rollout":
				return ROLLOUT;
			case "revoke":
				return REVOKE;
			default:
				return NONE;
		}
	}

	public static String toString(final InstallState installState) {
		if (installState == null)
			return null;

		switch (installState) {
			case ROLLOUT:
				return "rollout";
			case REVOKE:
				return "revoke";
			case NONE:
			default:
				return null;
		}
	}
}