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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

public class ExternalProcessJobParameter {

	public static final class ArgumentsParameterKeyComparator implements Comparator<String> {
		@Override
		public int compare(final String o1, final String o2) {
			// expect numbers as keys for proper ordering
			// (un-parsable keys appear last)
			final int i1 = NumberUtils.toInt(o1, Integer.MAX_VALUE);
			final int i2 = NumberUtils.toInt(o2, Integer.MAX_VALUE);
			return i1 - i2;
		}
	}

	public static final String PARAM_PREFIX_ARG = "arg:";
	public static final String PARAM_PREFIX_ENV = "env:";
	public static final String PARAM_WORKING_DIR = "workingDir";
	public static final String PARAM_EXPECTED_RETURN_CODE = "expectedReturnCode";
	public static final String PARAM_CLEAR_ENVIRONMENT = "clearEnvironment";
	public static final String PARAM_COMMAND = "command";

	public static final String ENV_VALUE_INHERIT = "!INHERIT!";

	public static ExternalProcessJobParameter fromParameter(final Map<String, String> params, final boolean resolveInheritedEnvironmentVariables) {
		final ExternalProcessJobParameter p = new ExternalProcessJobParameter();

		// collect arguments into a sorting map
		final SortedMap<String, String> arguments = new TreeMap<>(new ArgumentsParameterKeyComparator());

		// walk through all possible parameter
		final Map<String, String> environment = new HashMap<>();
		for (final Entry<String, String> e : params.entrySet()) {
			String value = e.getValue();
			switch (e.getKey()) {
				case PARAM_COMMAND:
					p.setCommand(value);
					break;
				case PARAM_CLEAR_ENVIRONMENT:
					p.setClearEnvironment(BooleanUtils.toBooleanObject(value));
					break;
				case PARAM_EXPECTED_RETURN_CODE:
					p.setExpectedReturnCode(NumberUtils.toInt(value));
					break;
				case PARAM_WORKING_DIR:
					p.setWorkingDir(value);
					break;

				default:
					if (e.getKey().startsWith(PARAM_PREFIX_ENV)) {
						final String name = e.getKey().substring(PARAM_PREFIX_ENV.length());
						if (resolveInheritedEnvironmentVariables && StringUtils.equals(ENV_VALUE_INHERIT, value)) {
							value = System.getenv(name);
						}
						environment.put(name, value);
					} else if (e.getKey().startsWith(PARAM_PREFIX_ARG)) {
						arguments.put(e.getKey().substring(PARAM_PREFIX_ARG.length()), value);
					}
					break;
			}
		}

		if (!arguments.isEmpty()) {
			p.setArguments(new ArrayList<>(arguments.values()));
		}

		if (!environment.isEmpty()) {
			p.setEnvironment(environment);
		}
		return p;
	}

	private String command = null;
	private Boolean clearEnvironment = null;
	private Integer expectedReturnCode = null;
	private String workingDir = null;
	private Map<String, String> environment;
	private List<String> arguments;

	public List<String> getArguments() {
		return arguments;
	}

	public Boolean getClearEnvironment() {
		return clearEnvironment;
	}

	public String getCommand() {
		return command;
	}

	public Map<String, String> getEnvironment() {
		return environment;
	}

	public Integer getExpectedReturnCode() {
		return expectedReturnCode;
	}

	public String getWorkingDir() {
		return workingDir;
	}

	public void setArguments(final List<String> arguments) {
		this.arguments = arguments;
	}

	public void setClearEnvironment(final Boolean clearEnvironment) {
		this.clearEnvironment = clearEnvironment;
	}

	public void setCommand(final String command) {
		this.command = command;
	}

	public void setEnvironment(final Map<String, String> environment) {
		this.environment = environment;
	}

	public void setExpectedReturnCode(final Integer expectedReturnCode) {
		this.expectedReturnCode = expectedReturnCode;
	}

	public void setWorkingDir(final String workingDir) {
		this.workingDir = workingDir;
	}

	public Map<String, String> toParameter() {
		final Map<String, String> params = new LinkedHashMap<>();

		if (StringUtils.isNotBlank(command)) {
			params.put(PARAM_COMMAND, command);
		}

		if (arguments != null) {
			for (int i = 0; i < arguments.size(); i++) {
				params.put(PARAM_PREFIX_ARG + i, String.valueOf(arguments.get(i)));
			}
		}

		if (environment != null) {
			for (final Entry<String, String> e : environment.entrySet()) {
				params.put(PARAM_PREFIX_ENV + e.getKey(), e.getValue());
			}
		}

		if (clearEnvironment != null) {
			params.put(PARAM_CLEAR_ENVIRONMENT, clearEnvironment.toString());
		}

		if (expectedReturnCode != null) {
			params.put(PARAM_EXPECTED_RETURN_CODE, expectedReturnCode.toString());
		}

		if (StringUtils.isNotBlank(workingDir)) {
			params.put(PARAM_WORKING_DIR, workingDir);
		}

		return params;
	}

}
