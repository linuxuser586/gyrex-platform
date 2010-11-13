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
package org.eclipse.gyrex.http.jetty.internal.app;

import javax.servlet.Servlet;

import org.eclipse.gyrex.context.IRuntimeContext;

import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * ServletHolder with injection capabilities based on {@link IRuntimeContext}.
 */
@SuppressWarnings("restriction")
public class ApplicationServletHolder extends ServletHolder {

	public ApplicationServletHolder() {
		super();
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param servlet
	 */
	public ApplicationServletHolder(final Class<? extends Servlet> servlet) {
		super(servlet);
		setName(servlet.getName() + "-" + super.hashCode());
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param servlet
	 */
	public ApplicationServletHolder(final Servlet servlet) {
		super(servlet);
		setName(servlet.getClass().getName() + "-" + super.hashCode());
	}

	private ApplicationServletHandler getApplicationServletHandler() {
		return (ApplicationServletHandler) getServletHandler();
	}

	@Override
	public synchronized Servlet newInstance() throws InstantiationException, IllegalAccessException {
		final Class<? extends Servlet> heldClass = getHeldClass();
		if (null == heldClass) {
			throw new InstantiationException("no held class: " + getClassName());
		}
		final IRuntimeContext context = getApplicationServletHandler().getApplicationHandler().getApplication().getContext();
		try {
			return context.getInjector().make(heldClass);
		} catch (final InjectionException e) {
			final InstantiationException instantiationException = new InstantiationException("error injecting class: " + e.getMessage());
			instantiationException.initCause(e);
			throw instantiationException;
		}
	}

}
