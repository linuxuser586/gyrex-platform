/*******************************************************************************
 * Copyright (c) 2008, 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.internal.application.defaultapp;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.provider.ApplicationProvider;

import org.eclipse.core.runtime.CoreException;

/**
 * Provider for the {@link DefaultApplication}
 */
public class DefaultApplicationProvider extends ApplicationProvider {

	public static final String ID = "org.eclipse.gyrex.http.internal.apps.default";

	public DefaultApplicationProvider() {
		super(ID);
	}

	@Override
	public Application createApplication(final String applicationId, final IRuntimeContext context) throws CoreException {
		return new DefaultApplication(applicationId, context);
	}

}
