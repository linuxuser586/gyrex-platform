/*******************************************************************************
 * Copyright (c) 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.p2.internal.commands;

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.p2.internal.P2Debug;
import org.eclipse.gyrex.p2.internal.installer.PackageScanner;

import org.eclipse.core.runtime.jobs.Job;

public final class UpdateCmd extends Command {

	public UpdateCmd() {
		super("updates the local node");
	}

	@Override
	protected void doExecute() throws Exception {
		final PackageScanner packageScanner = PackageScanner.getInstance();
		if (packageScanner.getState() == Job.RUNNING) {
			ci.println("update already in progess");
			return;
		} else {
			if (!packageScanner.cancel()) {
				ci.println("update already in progess; unable to cancel");
				return;
			}
		}

		// enable debug logging
		final boolean wasDebugging = P2Debug.nodeInstallation;
		P2Debug.nodeInstallation = true;

		// execute
		packageScanner.schedule();
		packageScanner.join();

		// reset logging
		P2Debug.nodeInstallation = wasDebugging;
		ci.println("update finished");
	}
}