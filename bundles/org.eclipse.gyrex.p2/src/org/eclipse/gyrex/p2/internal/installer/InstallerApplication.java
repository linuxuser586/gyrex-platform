/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.p2.internal.installer;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import org.eclipse.gyrex.p2.internal.P2Debug;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application which scans for new packages and installs them on the local node.
 */
public class InstallerApplication implements IApplication {

	private static final Logger LOG = LoggerFactory.getLogger(InstallerApplication.class);

	private static final AtomicReference<PackageScanner> jobRef = new AtomicReference<PackageScanner>();
	private final AtomicReference<IApplicationContext> contextRef = new AtomicReference<IApplicationContext>();

	@Override
	public Object start(final IApplicationContext context) throws Exception {
		final PackageScanner job = PackageScanner.getInstance();
		if (!jobRef.compareAndSet(null, job)) {
			throw new IllegalStateException("installer application already started");
		}

		// schedule job
		if (P2Debug.nodeInstallation) {
			LOG.debug("Scheduling package scanner to check for new packages now.");
		}
		job.schedule();

		// signal running
		context.applicationRunning();

		// remember context and return async
		contextRef.set(context);
		return IApplicationContext.EXIT_ASYNC_RESULT;
	}

	@Override
	public void stop() {
		final PackageScanner job = jobRef.getAndSet(null);
		if (job == null) {
			return;
		}

		// cancel job
		job.cancel();

		// wait for finish
		try {
			job.join();
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		// set result
		final IApplicationContext context = contextRef.getAndSet(null);
		if (null != context) {
			context.setResult(EXIT_OK, this);
		}
	}

}
