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
package org.eclipse.cloudfree.common.internal.context;

import org.eclipse.cloudfree.common.context.IContext;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.PlatformObject;

/**
 * Internal context implementation.
 */
public class CloudFreeContextImpl extends PlatformObject implements IContext {

	private final IPath contextPath;

	/**
	 * Creates a new instance.
	 * 
	 * @param contextPath
	 */
	public CloudFreeContextImpl(final IPath contextPath) {
		if (null == contextPath) {
			throw new IllegalArgumentException("context path may not be null");
		}
		this.contextPath = contextPath;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.common.context.IContext#getContextPath()
	 */
	@Override
	public IPath getContextPath() {
		return contextPath;
	}

}
