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

import java.util.Collection;

import javax.inject.Inject;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.di.annotations.DynamicService;

public class DynamicFieldInjection {

	@Inject
	@DynamicService
	ISampleService service;

	@Inject
	IRuntimeContext context;

	@Inject
	@DynamicService
	Collection<ISampleService> services;

	public void assertInjected() {
		assertNotNull("no context", context);
		assertNotNull("no osgi service", service);
		assertNotNull("no osgi service collection", services);
	}

	@Override
	public String toString() {
		return "DynamicFieldInjection{ " + context + ", " + service + " }";
	}
}