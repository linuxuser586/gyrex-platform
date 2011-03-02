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
package org.eclipse.gyrex.jobs.provider;

import java.text.MessageFormat;

import org.eclipse.gyrex.common.identifiers.IdHelper;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.jobs.Job;

import org.apache.commons.lang.StringUtils;

/**
 * A job provider base class which provides {@link Job} instances to Gyrex. *
 * <p>
 * job providers can be dynamically registered to Gyrex by registering
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
 */
public abstract class JobProvider extends PlatformObject {

	/** the OSGi service name */
	public static final String SERVICE_NAME = JobProvider.class.getName();

	/** job provider providerId */
	private final String[] providerIds;

	/**
	 * Creates a new instance using the specified provider id.
	 * 
	 * @param id
	 *            the job provider id (may not be <code>null</code>, will be
	 *            {@link IdHelper#isValidId(String) validated})
	 */
	protected JobProvider(final String[] ids) {
		if ((null == ids) || (ids.length == 0)) {
			throw new IllegalArgumentException("job provider ids must not be null or empty");
		}
		providerIds = new String[ids.length];
		for (int i = 0; i < ids.length; i++) {
			final String id = ids[i];
			if (!IdHelper.isValidId(id)) {
				throw new IllegalArgumentException(MessageFormat.format("job provider id \"{0}\" is invalid; valid chars are US-ASCII a-z / A-Z / 0-9 / '.' / '-' / '_'", id));
			}
			providerIds[i] = id;
		}
	}

	/**
	 * Returns the job provider identifiers.
	 * 
	 * @return the job provider identifiers
	 */
	public final String[] getProviderIds() {
		return providerIds;
	}

	/**
	 * Returns a string containing a concise, human-readable description of the
	 * provider.
	 * 
	 * @return a string representation of the provider
	 */
	@Override
	public final String toString() {
		return getClass().getSimpleName() + " [" + StringUtils.join(providerIds, ',') + "]";
	}

}
