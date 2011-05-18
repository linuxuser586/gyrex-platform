/*******************************************************************************
 * Copyright (c) 2010, 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.jetty.internal.app;

import javax.servlet.Filter;

import org.eclipse.gyrex.context.IRuntimeContext;

import org.eclipse.jetty.servlet.FilterHolder;

/**
 * FilterHolder with injection capabilities based on {@link IRuntimeContext}.
 */
public class ApplicationFilterHolder extends FilterHolder {

	public ApplicationFilterHolder() {
		super();
	}

//	/**
//	 * Creates a new instance.
//	 *
//	 * @param filter
//	 */
//	public ApplicationFilterHolder(final Class<? extends Filter> filter) {
//		super(filter);
//		setName(filter.getName() + "-" + super.hashCode());
//	}

	/**
	 * Creates a new instance.
	 * 
	 * @param filter
	 */
	public ApplicationFilterHolder(final Filter filter) {
		super(filter);
		setName(filter.getClass().getName() + "-" + super.hashCode());
	}

//	private ApplicationServletHandler getApplicationServletHandler() {
//		return (ApplicationServletHandler) getServletHandler();
//	}
//
//	@Override
//	public synchronized Filter newInstance() throws InstantiationException, IllegalAccessException {
//		final Class<? extends Filter> heldClass = getHeldClass();
//		if (null == heldClass) {
//			throw new InstantiationException("no held class: " + getClassName());
//		}
//		final IRuntimeContext context = getApplicationServletHandler().getApplicationHandler().getApplication().getContext();
//		try {
//			return context.getInjector().make(heldClass);
//		} catch (final InjectionException e) {
//			final InstantiationException instantiationException = new InstantiationException("error injecting class: " + e.getMessage());
//			instantiationException.initCause(e);
//			throw instantiationException;
//		}
//	}

}
