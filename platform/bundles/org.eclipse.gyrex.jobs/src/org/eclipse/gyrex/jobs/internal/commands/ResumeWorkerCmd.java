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
package org.eclipse.gyrex.jobs.internal.commands;

import org.eclipse.gyrex.common.console.Command;
import org.eclipse.gyrex.jobs.internal.worker.WorkerEngine;

/**
 * Resumes processing of jobs by workers
 */
public class ResumeWorkerCmd extends Command {

	/**
	 * Creates a new instance.
	 */
	public ResumeWorkerCmd() {
		super("resumes processing of jobs by workers");
	}

	@Override
	protected void doExecute() throws Exception {
		WorkerEngine.resume();
	}

}
