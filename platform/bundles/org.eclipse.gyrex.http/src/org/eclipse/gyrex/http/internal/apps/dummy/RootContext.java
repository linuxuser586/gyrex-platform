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
package org.eclipse.gyrex.http.internal.apps.dummy;


import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.gyrex.common.context.IContext;

/**
 * 
 */
public class RootContext extends PlatformObject implements IContext {

	/* (non-Javadoc)
	 * @see org.eclipse.gyrex.common.context.IContext#getContextPath()
	 */
	@Override
	public IPath getContextPath() {
		return Path.ROOT;
	}

}
