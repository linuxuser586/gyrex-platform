/*******************************************************************************
 * Copyright (c) 2008, 2009 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.model.common;

import java.text.MessageFormat;

import org.eclipse.gyrex.context.IRuntimeContext;

/**
 * This class may be used to obtain any model implementation.
 * <p>
 * This class is not intended to be subclassed or instantiated. It provides
 * static methods to streamline the model access.
 * </p>
 */
public final class ModelUtil {

	/**
	 * Returns a contributed model manager implementation of the specified type
	 * for a given context.
	 * <p>
	 * This implementation asks the specified context for an adapter of the
	 * specified manager type. This will ensure that always the correct manager
	 * for the specified context will be used. If no manager is available, an
	 * attempt will be made to load contributed managers from bundles which
	 * haven't been started yet.
	 * </p>
	 * <p>
	 * An {@link IllegalStateException} will be thrown if no manager
	 * implementation is available. The reason is simplicity and convenience.
	 * Any callers can expect that they get the correct manager at any time. If
	 * they don't get the model manager they want, it basically means that the
	 * system is in an unusable state anyway (because of upgrades or missing
	 * dependencies) and the current operation should be aborted an re-tried
	 * later.
	 * </p>
	 * <p>
	 * Callers should not hold onto the returned manager for a longer time. The
	 * context is allowed to be reconfigured at runtime. Additionally, bundles
	 * contributing model managers are allowed to come and got at any time in a
	 * dynamic system.
	 * </p>
	 * 
	 * @param managerType
	 *            the model manager type (may not be <code>null</code>)
	 * @param context
	 *            the context for model manager lookup (may not be
	 *            <code>null</code>)
	 * @return the model manager implementation
	 * @throws IllegalArgumentException
	 *             if any input is <code>null</code> or invalid
	 * @throws IllegalStateException
	 *             if no suitable manager implementation is currently available
	 */
	public static <M extends IModelManager> M getManager(final Class<M> managerType, final IRuntimeContext context) throws IllegalArgumentException, IllegalStateException {
		if (null == managerType) {
			throw new IllegalArgumentException("manager type must not be null");
		}
		if (null == context) {
			throw new IllegalArgumentException("context must not be null");
		}

		// we simply ask the context for an adapter
		final M manager = context.get(managerType);

		// at this point check if a manager was found
		if (null == manager) {
			throw new IllegalStateException(MessageFormat.format("No model manager implementation available for type ''{0}'' in context ''{1}''", managerType.getName(), context.getContextPath()));
		}

		return manager;
	}

	/**
	 * Hidden constructor.
	 */
	private ModelUtil() {
		//empty
	}
}
