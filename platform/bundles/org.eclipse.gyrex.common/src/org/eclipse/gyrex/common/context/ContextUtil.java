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
package org.eclipse.gyrex.common.context;

import org.eclipse.core.runtime.IPath;
import org.eclipse.gyrex.common.internal.context.GyrexContextImpl;

/**
 * Provides access to Gyrex contexts.
 * <p>
 * Access to Gyrex contexts is provided through this central class.
 * Internally, it uses a pluggable strategy for resolving the path to a context.
 * </p>
 * <p>
 * Note, using security it is possible to limit access to certain contexts to a
 * trusted group.
 * </p>
 */
public class ContextUtil {

	/**
	 * Returns the Gyrex context with the specified path.
	 * <p>
	 * Note, security may be used to verify that the caller is allowed to access
	 * the specified context.
	 * </p>
	 * 
	 * @param contextPath
	 *            the context path
	 * @return
	 */
	public static IContext get(final IPath contextPath) {
		// TODO: implement pluggable strategy
		// TODO: implement security
		return new GyrexContextImpl(contextPath);
	}

}
