/*******************************************************************************
 * Copyright (c) 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.externalprocess;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.jobs.internal.JobsActivator;
import org.eclipse.gyrex.jobs.internal.externalprocess.AsyncLoggingInputStreamReader.Level;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalProcessJob extends Job {

	private static final Logger DEFAULT_LOG = LoggerFactory.getLogger(ExternalProcessJob.class);

	private final String jobId;
	private final List<String> command;
	private File workingDirectory;
	private final Logger log;
	private boolean clearEnvironment;
	private Map<String, String> additionalEnvironment;
	private int exitValue = 0;

	public ExternalProcessJob(final String jobId, final List<String> command, final Logger log) {
		super(jobId);
		this.jobId = jobId;
		this.command = command;
		this.log = null != log ? log : DEFAULT_LOG;
		setPriority(LONG);

		if (!IdHelper.isValidId(jobId))
			throw new IllegalArgumentException("Invalid job id: " + jobId);

		if ((null == command) || command.isEmpty())
			throw new IllegalArgumentException("Command must not be null or empty!");
	}

	public int getExitValue() {
		return exitValue;
	}

	@Override
	protected IStatus run(final IProgressMonitor monitor) {
		final ProcessBuilder builder = new ProcessBuilder();

		log.debug("Command: {}", command);
		builder.command(command);

		if (workingDirectory != null) {
			log.debug("Using working directory: {}", workingDirectory);
			builder.directory(workingDirectory);
		}

		if (clearEnvironment) {
			builder.environment().clear();
			log.debug("Cleared environment!");
		} else {
			// remove all Gyrex specific settings for security reasons
			final Iterator<Entry<String, String>> entries = builder.environment().entrySet().iterator();
			while (entries.hasNext()) {
				final Map.Entry<java.lang.String, java.lang.String> e = entries.next();
				if (StringUtils.startsWithIgnoreCase(e.getKey(), "gyrex")) {
					log.debug("Removing Gyrex specific environment variable: {}", e.getKey());
					entries.remove();
				}
			}
		}

		setEnvironmentVariable(builder, "WORKSPACE", Platform.getInstanceLocation().toOSString());
		setEnvironmentVariable(builder, "JOB_ID", jobId);

		if (additionalEnvironment != null) {
			for (final Entry<String, String> e : additionalEnvironment.entrySet()) {
				log.debug("Additional environment variable: {} = {}", e.getKey(), e.getValue());
				builder.environment().put(e.getKey(), e.getValue());
			}
		}

		AsyncLoggingInputStreamReader inputStreamReader = null, errorStreamReader = null;
		try {
			final Process p = builder.start();
			inputStreamReader = new AsyncLoggingInputStreamReader(jobId + "[OUT Reader]", p.getInputStream(), log, Level.INFO);
			errorStreamReader = new AsyncLoggingInputStreamReader(jobId + "[ERR Reader]", p.getErrorStream(), log, Level.ERROR);

			final int result = p.waitFor();
			if (result != exitValue)
				return new Status(IStatus.ERROR, JobsActivator.SYMBOLIC_NAME, "Process finished with unexpected exit value: " + result);

		} catch (final InterruptedException e) {
			log.warn("Interrupted while waiting for the process to finish.", e);
			Thread.currentThread().interrupt();
			return Status.CANCEL_STATUS;
		} catch (final Exception | AssertionError | LinkageError e) {
			log.error("Error starting process. {} ", e.getMessage(), e);
			return new Status(IStatus.ERROR, JobsActivator.SYMBOLIC_NAME, "Error starting process: " + e.getMessage(), e);
		} finally {
			if (inputStreamReader != null) {
				inputStreamReader.close();
			}
			if (errorStreamReader != null) {
				errorStreamReader.close();
			}
		}

		if (StringUtils.isNotBlank(inputStreamReader.getLastLine()))
			return new Status(IStatus.OK, JobsActivator.SYMBOLIC_NAME, inputStreamReader.getLastLine());

		return Status.OK_STATUS;
	}

	public void setAdditionalEnvironment(final Map<String, String> additionalEnvironment) {
		this.additionalEnvironment = additionalEnvironment;
	}

	public void setClearEnvironment(final boolean clearEnvironment) {
		this.clearEnvironment = clearEnvironment;
	}

	private void setEnvironmentVariable(final ProcessBuilder builder, final String key, final String value) {
		log.debug("Setting environment variable: {} = {}", key, value);
		builder.environment().put(key, value);
	}

	public void setExitValue(final int exitValue) {
		this.exitValue = exitValue;
	}

	public void setWorkingDirectory(final File workingDirectory) {
		this.workingDirectory = workingDirectory;
	}
}
