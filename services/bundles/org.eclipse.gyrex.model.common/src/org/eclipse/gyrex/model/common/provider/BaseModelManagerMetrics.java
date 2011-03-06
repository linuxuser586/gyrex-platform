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
package org.eclipse.gyrex.model.common.provider;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.monitoring.metrics.BaseMetric;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;
import org.eclipse.gyrex.persistence.storage.Repository;

/**
 * Convenience base class for model manager metrics.
 * <p>
 * This class may be extended by clients to provide a convenient set of metrics
 * for model managers.
 * </p>
 * 
 * @see MetricSet
 */
public abstract class BaseModelManagerMetrics extends MetricSet {

	/**
	 * Convenience method to create a human-readable metrics description based
	 * on a simple manager implementation name (eg. a <code>"MyManager"</code>),
	 * a specified context and a repository.
	 * 
	 * @param managerImplementationName
	 *            the manager implementation name
	 * @param context
	 *            the context
	 * @param repository
	 *            the repository
	 * @return a human-readable metrics description
	 */
	protected static String createDescription(final String managerImplementationName, final IRuntimeContext context, final Repository repository) {
		return String.format("Metrics for %s in context %s backed by repository %s.", managerImplementationName, context.getContextPath(), repository.getRepositoryId());
	}

	/**
	 * Convenience method to create a modifiable map of metric properties based
	 * on a manager implementation class, a specified context and a repository.
	 * 
	 * @param managerImplementationName
	 *            the manager implementation name
	 * @param context
	 *            the context
	 * @param repository
	 *            the repository
	 * @return a human-readable metrics description
	 */
	protected static Map<String, String> createProperties(final Class<? extends BaseModelManager<?>> managerClass, final IRuntimeContext context, final Repository repository) {
		final HashMap<String, String> properties = new HashMap<String, String>(4);
		properties.put("manager.class", managerClass.getName());
		properties.put("context.path", context.getContextPath().toString());
		properties.put("repository.id", repository.getRepositoryId());
		properties.put("repository.provider.id", repository.getRepositoryProvider().getProviderId());
		return properties;
	}

	/**
	 * Creates a new metric set for the specified model manager class using the
	 * specified id and metrics.
	 * <p>
	 * This is a convenience method which uses
	 * {@link #createDescription(String, IRuntimeContext, Repository)} as well
	 * as {@link #createProperties(Class, IRuntimeContext, Repository)} to have
	 * a common practise for model manager metrics.
	 * </p>
	 * 
	 * @param id
	 *            the metrics id
	 * @param managerClass
	 *            the manager implementation class
	 * @param context
	 *            the context the manager operates in
	 * @param repository
	 *            the repository used by the manager
	 * @param metrics
	 *            the metrics which form this set
	 */
	protected BaseModelManagerMetrics(final String id, final Class<? extends BaseModelManager<?>> managerClass, final IRuntimeContext context, final Repository repository, final BaseMetric... metrics) {
		super(id, createDescription(managerClass.getSimpleName(), context, repository), createProperties(managerClass, context, repository), metrics);
	}

	/**
	 * Creates a new metric set using the specified id and metrics.
	 * <p>
	 * The metrics are stored in an immutable way and can be retrieved using
	 * {@link #getMetric(int, Class)}.
	 * </p>
	 * <p>
	 * The ids of the metrics may but don't need to be prefixed with the metric
	 * set id. In any case the will be interpreted within the scope of the
	 * metric set id.
	 * </p>
	 * <p>
	 * The specified properties may be used to further identify, classify,
	 * annotate or group the metric set.
	 * </p>
	 * 
	 * @param id
	 *            the metric id
	 * @param description
	 *            the metric description
	 * @param properties
	 *            the metric properties (each key
	 *            {@link IdHelper#isValidId(String) must be a valid identifier})
	 * @param metrics
	 *            the metrics which form this set
	 */
	protected BaseModelManagerMetrics(final String id, final String description, final Map<String, String> properties, final BaseMetric... metrics) {
		super(id, description, properties, metrics);
	}

}
