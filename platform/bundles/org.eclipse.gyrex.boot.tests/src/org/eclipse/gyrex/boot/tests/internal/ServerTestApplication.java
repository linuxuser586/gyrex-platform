/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.boot.tests.internal;

import org.eclipse.equinox.app.IApplication;

import org.eclipse.gyrex.boot.internal.app.ServerApplication;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.pde.internal.junit.runtime.RemotePluginTestRunner;

@SuppressWarnings("restriction")
public class ServerTestApplication extends ServerApplication implements IApplication {

	@Override
	protected void onServerStarted(final String[] arguments) {
		// schedule test executions
		new Job("PDE JUnit Test Runner") {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				try {
					RemotePluginTestRunner.main(arguments);
					ServerApplication.signalShutdown(null);
				} catch (final Exception e) {
					ServerApplication.signalShutdown(e);
				}
				return Status.OK_STATUS;
			}
		}.schedule();
	}

}
