/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.worker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.eclipse.gyrex.cloud.services.queue.IMessage;
import org.eclipse.gyrex.common.identifiers.IdHelper;

import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 * Serialization/deserialization helper for Job queue messages.
 */
public class JobInfo {

	private static final String JOB_ID_KEY = "gyrex.jobinfo.jobid"; //$NON-NLS-1$
	private static final String VERSION_KEY = "gyrex.jobinfo.version"; //$NON-NLS-1$
	private static final String VERSION_VALUE = "1"; //$NON-NLS-1$

	public static byte[] asMessage(final JobInfo info) throws IOException {
		final Properties properties = new Properties();

		// collect properties
		final Map<String, String> jobProperties = info.getJobProperties();
		for (final Iterator stream = jobProperties.keySet().iterator(); stream.hasNext();) {
			final String key = (String) stream.next();
			properties.put(key, jobProperties.get(key));
		}

		// put version
		properties.put(VERSION_KEY, VERSION_VALUE);

		// put id
		properties.put(JOB_ID_KEY, info.getJobId());

		// create bytes
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		properties.store(out, null);
		return out.toByteArray();
	}

	public static JobInfo parse(final IMessage message) throws IOException {
		final Properties properties = new Properties();
		properties.load(new ByteArrayInputStream(message.getBody()));

		// check version (remove key from properties)
		final Object versionValue = properties.remove(VERSION_KEY);
		if (!VERSION_VALUE.equals(versionValue)) {
			throw new IOException(String.format("invalid record data: version mismatch (expected %d, found %s)", 1, String.valueOf(versionValue)));
		}

		// check id (remove key from properties as well)
		final Object jobIdValue = properties.remove(JOB_ID_KEY);
		if ((null == jobIdValue) || !(jobIdValue instanceof String) || !IdHelper.isValidId(((String) jobIdValue))) {
			throw new IOException(String.format("invalid record data: missing/invalid job id %s", String.valueOf(jobIdValue)));
		}

		// collect properties
		final Map<String, String> jobProperties = new HashMap<String, String>();
		for (final Iterator stream = properties.keySet().iterator(); stream.hasNext();) {
			final String key = (String) stream.next();
			jobProperties.put(key, properties.getProperty(key));
		}

		// create job info
		return new JobInfo((String) jobIdValue, jobProperties);
	}

	private final String jobId;
	private final Map<String, String> jobProperties;

	/**
	 * Creates a new instance.
	 * 
	 * @param jobId
	 * @param jobProperties
	 */
	public JobInfo(final String jobId, final Map<String, String> jobProperties) {
		this.jobId = jobId;
		this.jobProperties = jobProperties;
	}

	/**
	 * Returns the jobId.
	 * 
	 * @return the jobId
	 */
	public String getJobId() {
		return jobId;
	}

	/**
	 * Returns the jobProperties.
	 * 
	 * @return the jobProperties
	 */
	public Map<String, String> getJobProperties() {
		return jobProperties;
	}

}
