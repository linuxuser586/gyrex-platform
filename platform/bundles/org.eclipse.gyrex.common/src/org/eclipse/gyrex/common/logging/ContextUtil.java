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
package org.eclipse.gyrex.common.logging;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.gyrex.common.context.IContext;
import org.eclipse.gyrex.common.internal.fixme.AdapterManagerAccess;

/**
 * This class provides static utility methods when working with contexts.
 */
/*package*/final class ContextUtil {

	/**
	 * Returns the {@link IContext} from an adaptable object.
	 * <p>
	 * First, the adaptable is checked if it implements the {@link IContext}
	 * interface directly. If not but the adaptable object implements the
	 * {@link IAdaptable} interface then it's asked for returning an
	 * {@link IContext}.
	 * </p>
	 * 
	 * @param adaptable
	 *            the adaptable (may be <code>null</code>)
	 * @return the found context or <code>null</code> if the adaptable was
	 *         <code>null</code> or no {@link IContext} is available
	 */
	public static IContext getContext(final Object adaptable) {
		if (null == adaptable) {
			return null;
		}
		if (IContext.class.isAssignableFrom(adaptable.getClass())) {
			return (IContext) adaptable;
		}

		if (IAdaptable.class.isAssignableFrom(adaptable.getClass())) {
			IContext context = (IContext) ((IAdaptable) adaptable).getAdapter(IContext.class);
			if (null == context) {
				context = (IContext) AdapterManagerAccess.getAdapterManager().loadAdapter(adaptable, IContext.class.getName());
			}
			return context;
		}

		// give up
		return null;
	}

	/**
	 * No need to instantiate this class
	 */
	private ContextUtil() {
		// empty
	}
}
