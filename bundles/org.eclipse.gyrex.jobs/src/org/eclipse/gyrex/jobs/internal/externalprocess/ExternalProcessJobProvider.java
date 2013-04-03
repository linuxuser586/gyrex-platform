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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.gyrex.jobs.IJobContext;
import org.eclipse.gyrex.jobs.provider.JobProvider;

import org.eclipse.core.runtime.jobs.Job;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

public class ExternalProcessJobProvider extends JobProvider {

	public static final String PARAM_PREFIX_ARG = "arg:";
	public static final String PARAM_PREFIX_ENV = "env:";
	public static final String PARAM_WORKING_DIR = "workingDir";
	public static final String PARAM_EXPECTED_RETURN_CODE = "expectedReturnCode";
	public static final String PARAM_CLEAR_ENVIRONMENT = "clearEnvironment";
	public static final String PARAM_COMMAND = "command";

	public static final String ENV_VALUE_INHERIT = "!INHERIT!";

	public static final String JOB_TYPE_ID = "org.eclipse.gyrex.jobs.system.externalprocess";

	public ExternalProcessJobProvider() {
		super(Arrays.asList(JOB_TYPE_ID));
	}

	private ExternalProcessJob create(final IJobContext context) {
		String command = null;
		Boolean clearEnvironment = null;
		Integer expectedReturnCode = null;
		String workingDir = null;
		final Map<String, String> environment = new HashMap<>();
		final SortedMap<String, String> arguments = new TreeMap<>(new Comparator<String>() {
			@Override
			public int compare(final String o1, final String o2) {
				// expect numbers as keys for proper ordering
				// (un-parsable keys appear last)
				final int i1 = NumberUtils.toInt(o1, Integer.MAX_VALUE);
				final int i2 = NumberUtils.toInt(o2, Integer.MAX_VALUE);
				return i1 - i2;
			}
		});
		for (final Entry<String, String> e : context.getParameter().entrySet()) {
			String value = e.getValue();
			switch (e.getKey()) {
				case PARAM_COMMAND:
					command = value;
					break;
				case PARAM_CLEAR_ENVIRONMENT:
					clearEnvironment = BooleanUtils.toBooleanObject(value);
					break;
				case PARAM_EXPECTED_RETURN_CODE:
					expectedReturnCode = NumberUtils.toInt(value);
					break;
				case PARAM_WORKING_DIR:
					workingDir = value;
					break;

				default:
					if (e.getKey().startsWith(PARAM_PREFIX_ENV)) {
						final String name = e.getKey().substring(PARAM_PREFIX_ENV.length());
						if (StringUtils.equals(ENV_VALUE_INHERIT, value)) {
							value = System.getenv(name);
						}
						environment.put(name, value);
					} else if (e.getKey().startsWith(PARAM_PREFIX_ARG)) {
						arguments.put(e.getKey().substring(PARAM_PREFIX_ARG.length()), value);
					}
					break;
			}
		}

		if (StringUtils.isBlank(command))
			throw new IllegalArgumentException("command must not be blank");

		final List<String> commandLine = new ArrayList<>();
		commandLine.add(command);
		commandLine.addAll(arguments.values());

		final ExternalProcessJob job = new ExternalProcessJob(context.getJobId(), commandLine, null);
		if (expectedReturnCode != null) {
			job.setExitValue(expectedReturnCode.intValue());
		}
		if (clearEnvironment != null) {
			job.setClearEnvironment(clearEnvironment.booleanValue());
		}
		if (!environment.isEmpty()) {
			job.setAdditionalEnvironment(environment);
		}
		if (StringUtils.isNotBlank(workingDir)) {
			job.setWorkingDirectory(new File(workingDir));
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
