/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context;

import org.eclipse.core.runtime.IPath;

/**
 * Interface with shared, public context constants.
 */
public interface IRuntimeContextConstants {

	/**
	 * optional OSGi service property specifying the {@link IPath#toString()
	 * string representation} of a {@link IRuntimeContext#getContextPath()
	 * context path} (value {@value #SERVICE_PROPERTY_CONTEXT_PATH})
	 */
	String SERVICE_PROPERTY_CONTEXT_PATH = "gyrex.context.path";
}
