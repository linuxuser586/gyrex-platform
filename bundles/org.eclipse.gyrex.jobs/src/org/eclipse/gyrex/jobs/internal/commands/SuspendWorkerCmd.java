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
package org.eclipse.gyrex.jobs.internal.commands;

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.jobs.internal.worker.WorkerEngine;

/**
 * Suspends processing of jobs by workers
 */
public class SuspendWorkerCmd extends Command {

	/**
	 * Creates a new instance.
	 */
	public SuspendWorkerCmd() {
		super("suspends processing of jobs by workers");
	}

	@Override
	protected void doExecute() throws Exception {
		WorkerEngine.suspend();
		printf("Suspended processing of jobs.");
	}

}
