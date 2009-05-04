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
package org.eclipse.gyrex.context.internal.configuration;

import org.eclipse.core.runtime.IPath;

/**
 * The configuration of a particular context.
 */
public class ContextConfiguration {

	private final IPath contextPath;

	/**
	 * Creates a new instance.
	 */
	public ContextConfiguration(final IPath contextPath) {
		this.contextPath = contextPath;
	}

	/**
	 * Returns the contextPath.
	 * 
	 * @return the contextPath
	 */
	public IPath getContextPath() {
		return contextPath;
	}

}
