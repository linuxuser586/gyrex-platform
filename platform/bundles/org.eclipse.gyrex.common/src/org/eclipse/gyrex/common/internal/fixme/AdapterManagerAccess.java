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
package org.eclipse.cloudfree.common.internal.fixme;

import org.eclipse.core.internal.runtime.AdapterManager;
import org.eclipse.core.runtime.IAdapterManager;

/**
 * This class provides access to the {@link IAdapterManager}.
 * <p>
 * See also <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=161389">bug 161389</a>
 * </p>
 * 
 * @todo remove once bug 161389 got implemented
 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=161389
 */
@SuppressWarnings("restriction")
public class AdapterManagerAccess {

	/**
	 * Returns the {@link IAdapterManager} to use.
	 * 
	 * @return the {@link IAdapterManager}
	 */
	public static IAdapterManager getAdapterManager() {
		return AdapterManager.getDefault();
	}
}
