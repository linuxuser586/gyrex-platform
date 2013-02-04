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
 *     Mike Tschierschke - improvements due working on https://bugs.eclipse.org/bugs/show_bug.cgi?id=344467
 *******************************************************************************/
package org.eclipse.gyrex.jobs.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.gyrex.cloud.services.locking.ILockService;
import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.jobs.IJob;
import org.eclipse.gyrex.jobs.IJobContext;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import org.apache.commons.lang.StringUtils;

/**
 * A job provider base class which provides {@link Job} instances to Gyrex.
 * <p>
 * Job providers can be dynamically registered to Gyrex by registering
 * {@link JobProvider} instances as OSGi services (using {@link #SERVICE_NAME}).
 * Job providers are considered core elements of Gyrex. Security restrictions
 * may be used to only allow a set of well known (i.e. trusted) providers.
 * </p>
 * <p>
 * Job providers do not represent a concrete job. They will be used, however, to
 * create concrete job instances.
 * </p>
 * <p>
 * This class must be subclassed by clients that want to contribute a job
 * provider to Gyrex. However, it is typically not referenced directly outside
 * Gyrex.
 * </p>
 * 
 * @see IJob
 * @see IJobContext
 */
public abstract class JobProvider extends PlatformObject {

	/** the OSGi service name */
	public static final String SERVICE_NAME = JobProvider.class.getName();

	/** provided job type ids */
	private final List<String> providedTypeIds;

	/**
	 * Creates a new instance using the specified provider id.
	 * 
	 * @param providedTypeIds
	 *            the ids of the provided jobs (may not be <code>null</code> or
	 *            empty, will be {@link IdHelper#isValidId(String) validated})
	 */
	protected JobProvider(final Collection<String> providedTypeIds) {
		if ((null == providedTypeIds) || (providedTypeIds.isEmpty())) {
			throw new IllegalArgumentException("job types must not be null or empty");
		}

		// validate & save
		this.providedTypeIds = new ArrayList<String>(providedTypeIds.size());
		for (final String id : providedTypeIds) {
			if (!IdHelper.isValidId(id)) {
				throw new IllegalArgumentException(String.format("type id \"%s\" is invalid; valid chars are US-ASCII a-z / A-Z / 0-9 / '.' / '-' / '_'", id));
			} else {
				this.providedTypeIds.add(id);
			}
		}
	}

	/**
	 * Creates a new Job.
	 * <p>
	 * Note, the job will be scheduled by the framework. Therefore,
	 * implementations must not schedule it but just return the created job.
	 * </p>
	 * <p>
	 * Implementors are free to create new job instances or re-use shared job
	 * instances. However, care must be taken for any threading issues along the
	 * road as jobs <em>may</em> run concurrently on the same machine.
	 * {@link ISchedulingRule}s and the {@link ILockService lock service} may be
	 * used to coordinate concurrent execution of jobs on the same machine and
	 * within the cloud.
	 * </p>
	 * 
	 * @param typeId
	 *            the job type to crate
	 * @param context
	 *            the job context
	 * @return the job (maybe <code>null</code> if called for an unknown type)
	 * @throws Exception
	 *             if an error occurred creating the job
	 */
	public abstract Job createJob(String typeId, IJobContext context) throws Exception;

	/**
	 * Returns a list of provided job type identifiers.
	 * 
	 * @return an unmodifiable collection of provided job type identifiers
	 */
	public final Collection<String> getProvidedTypeIds() {
		return Collections.unmodifiableCollection(providedTypeIds);
	}

	/**
	 * Returns a string containing a concise, human-readable description of the
	 * provider.
	 * 
	 * @return a string representation of the provider
	 */
	@Override
	public final String toString() {
		return getClass().getSimpleName() + " [" + StringUtils.join(providedTypeIds, ',') + "]";
	}
}
