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
 *     Mike Tschierschke - improvements due working on https://bugs.eclipse.org/bugs/show_bug.cgi?id=344467
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.worker;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.eclipse.gyrex.cloud.services.queue.IMessage;
import org.eclipse.gyrex.common.identifiers.IdHelper;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 * Serialization/deserialization helper for Job queue messages.
 */
public class JobInfo {

	private static final String PREFIX = "gyrex.jobinfo."; //$NON-NLS-1$
	private static final String ID = PREFIX + "jobid"; //$NON-NLS-1$
	private static final String TYPE_ID = PREFIX + "jobtype"; //$NON-NLS-1$
	private static final String CONTEXT_PATH = PREFIX + "jobcontext"; //$NON-NLS-1$
	private static final String VERSION = PREFIX + "version"; //$NON-NLS-1$
	private static final String VERSION_VALUE = "1"; //$NON-NLS-1$

	public static byte[] asMessage(final JobInfo info) throws IOException {
		final Properties properties = new Properties();

		// collect properties
		final Map<String, String> jobProperties = info.getJobProperties();
		for (final Entry<String, String> entry : jobProperties.entrySet()) {
			properties.put(entry.getKey(), entry.getValue());
		}

		// put version
		properties.put(VERSION, VERSION_VALUE);

		// put id
		properties.put(ID, info.getJobId());

		// put type
		properties.put(TYPE_ID, info.getJobTypeId());

		// put type
		properties.put(CONTEXT_PATH, info.getContextPath().toString());

		// create bytes
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		properties.store(out, null);
		return out.toByteArray();
	}

	public static JobInfo parse(final IMessage message) throws IOException {
		final Properties properties = new Properties();
		properties.load(new ByteArrayInputStream(message.getBody()));

		// check version (remove key from properties)
		final Object versionValue = properties.remove(VERSION);
		if (!VERSION_VALUE.equals(versionValue)) {
			throw new IOException(String.format("invalid record data: version mismatch (expected %d, found %s)", 1, String.valueOf(versionValue)));
		}

		// get id (remove key from properties as well)
		final Object jobIdValue = properties.remove(ID);
		if ((null == jobIdValue) || !(jobIdValue instanceof String) || !IdHelper.isValidId(((String) jobIdValue))) {
			throw new IOException(String.format("invalid record data: missing/invalid job id %s", String.valueOf(jobIdValue)));
		}

		// get type (remove key from properties as well)
		final Object jobTypeValue = properties.remove(TYPE_ID);
		if ((null == jobTypeValue) || !(jobTypeValue instanceof String) || !IdHelper.isValidId(((String) jobTypeValue))) {
			throw new IOException(String.format("invalid record data: missing/invalid job id %s", String.valueOf(jobTypeValue)));
		}

		// get path (remove key from properties as well)
		final Object contextPathValue = properties.remove(CONTEXT_PATH);
		if ((null == contextPathValue) || !(contextPathValue instanceof String) || !Path.EMPTY.isValidPath(((String) contextPathValue))) {
			throw new IOException(String.format("invalid record data: missing/invalid context path %s", String.valueOf(contextPathValue)));
		}

		// collect properties
		final Map<String, String> jobProperties = new HashMap<String, String>();
		for (final Iterator<?> stream = properties.keySet().iterator(); stream.hasNext();) {
			final String key = (String) stream.next();
			if (!key.startsWith(PREFIX)) {
				jobProperties.put(key, properties.getProperty(key));
			}
		}

		// create job info
		return new JobInfo((String) jobTypeValue, (String) jobIdValue, new Path((String) contextPathValue), jobProperties);
	}

	private final String jobId;
	private final String jobTypeId;
	private final Map<String, String> jobProperties;
	private final IPath contextPath;

	/**
	 * Creates a new instance.
	 * 
	 * @param jobId
	 * @param jobProperties
	 */
	public JobInfo(final String jobTypeId, final String jobId, final IPath contextPath, final Map<String, String> jobProperties) {
		this.jobId = jobId;
		this.jobTypeId = jobTypeId;
		this.contextPath = contextPath;
		this.jobProperties = jobProperties;
	}

	/**
	 * Returns the contextPath.
	 * 
	 * @return the contextPath
	 */
	public IPath getContextPath() {
		return contextPath;
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

	/**
	 * @return
	 */
	public String getJobTypeId() {
		return jobTypeId;
	}

}
