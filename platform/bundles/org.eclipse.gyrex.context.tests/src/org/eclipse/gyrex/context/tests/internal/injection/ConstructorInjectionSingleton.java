/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context.tests.internal.injection;

import static junit.framework.Assert.assertNotNull;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.di.annotations.DynamicService;

@Singleton
public class ConstructorInjectionSingleton {
	final ISampleService service;
	final IRuntimeContext context;

	@Inject
	public ConstructorInjectionSingleton(final IRuntimeContext context, @DynamicService final ISampleService service) {
		assertNotNull("no context", context);
		assertNotNull("no osgi service", service);
		this.context = context;
		this.service = service;
	}

	@Override
	public String toString() {
		return "ConstructorInjectionSingleton{ " + context + ", " + service + " }";
	}
}