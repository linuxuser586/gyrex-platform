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
package org.eclipse.gyrex.jobs.internal.externalprocess;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.gyrex.jobs.IJobContext;
import org.eclipse.gyrex.jobs.provider.JobProvider;

import org.eclipse.core.runtime.jobs.Job;

import org.apache.commons.lang.StringUtils;

public class ExternalProcessJobProvider extends JobProvider {

	public static final String JOB_TYPE_ID = "org.eclipse.gyrex.jobs.system.externalprocess";

	public ExternalProcessJobProvider() {
		super(Arrays.asList(JOB_TYPE_ID));
	}

	private ExternalProcessJob create(final IJobContext context) {
		final ExternalProcessJobParameter parameter = ExternalProcessJobParameter.fromParameter(context.getParameter(), true);

		if (StringUtils.isBlank(parameter.getCommand()))
			throw new IllegalArgumentException("command must not be blank");

		final List<String> commandLine = new ArrayList<>();
		commandLine.add(parameter.getCommand());
		if (parameter.getArguments() != null) {
			commandLine.addAll(parameter.getArguments());
		}

		final ExternalProcessJob job = new ExternalProcessJob(context.getJobId(), commandLine, null);
		if (parameter.getExpectedReturnCode() != null) {
			job.setExitValue(parameter.getExpectedReturnCode().intValue());
		}
		if (parameter.getClearEnvironment() != null) {
			job.setClearEnvironment(parameter.getClearEnvironment().booleanValue());
		}
		if (parameter.getEnvironment() != null) {
			job.setAdditionalEnvironment(parameter.getEnvironment());
		}
		if (StringUtils.isNotBlank(parameter.getWorkingDir())) {
			job.setWorkingDirectory(new File(parameter.getWorkingDir()));
		}
		return job;
	}

	@Override
	public Job createJob(final String typeId, final IJobContext context) throws Exception {
		switch (typeId) {
			case JOB_TYPE_ID:
				return create(context);
			default:
				return null;
		}
	}

}
