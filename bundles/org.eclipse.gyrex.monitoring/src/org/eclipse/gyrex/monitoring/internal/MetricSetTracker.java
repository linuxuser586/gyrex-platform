/*******************************************************************************
 * Copyright (c) 2010, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.monitoring.internal;

import java.lang.management.ManagementFactory;
import java.util.Hashtable;
import java.util.Map.Entry;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.eclipse.gyrex.monitoring.internal.MetricSetTracker.MetricSetJmxRegistration;
import org.eclipse.gyrex.monitoring.internal.mbeans.MetricSetMBean;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracker for {@link MetricSet}.
 */
public class MetricSetTracker extends ServiceTracker<MetricSet, MetricSetJmxRegistration> {

	static class MetricSetJmxRegistration {
		final ObjectName objectName;
		final MetricSet metricSet;

		MetricSetJmxRegistration(final ObjectName objectName, final MetricSet metricSet) {
			this.objectName = objectName;
			this.metricSet = metricSet;
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(MetricSetTracker.class);

	public MetricSetTracker(final BundleContext context) {
		super(context, MetricSet.class, null);
	}

	@Override
	public MetricSetJmxRegistration addingService(final ServiceReference<MetricSet> reference) {
		// get service
		final MetricSet metricSet = context.getService(reference);
		if (metricSet == null) {
			return null;
		}

		try {
			final MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
			final ObjectName objectName = getObjectName(reference, metricSet);
			beanServer.registerMBean(new MetricSetMBean(metricSet, reference), objectName);
			return new MetricSetJmxRegistration(objectName, metricSet);
		} catch (final Exception e) {
			LOG.warn("Failed to expose metric {} using JMX. {}", metricSet, ExceptionUtils.getRootCauseMessage(e));
		}
		return null;
	}

	private ObjectName getObjectName(final ServiceReference<MetricSet> reference, final MetricSet metricSet) throws MalformedObjectNameException {
		// get symbolic name
		final String symbolicName = reference.getBundle().getSymbolicName();

		// set properties
		final String[] propertyKeys = reference.getPropertyKeys();
		final Hashtable<String, String> properties = new Hashtable<String, String>(propertyKeys.length + 2);

		// common properties first
		properties.put("type", "MetricSet");
		properties.put("name", StringUtils.removeStart(metricSet.getId(), symbolicName + "."));

		// we also remember the service id in order to allow multiple metrics instances with same id
		properties.put("service.id", String.valueOf(reference.getProperty(Constants.SERVICE_ID)));

		// check additional metric set properties
		for (final Entry<String, String> property : metricSet.getProperties().entrySet()) {
			// prefix each with "metric." to avoid name clash with common properties
			properties.put("metric.".concat(property.getKey()), property.getValue());
		}

		// create object name
		return new ObjectName(symbolicName, properties);
	}

	@Override
	public void removedService(final ServiceReference<MetricSet> reference, final MetricSetJmxRegistration metricSetJmxRegistration) {
		try {
			// unregister MBean
			final MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
			beanServer.unregisterMBean(metricSetJmxRegistration.objectName);
		} catch (final Exception e) {
			LOG.warn("Failed to unregister metric JMX registration {}. {}", metricSetJmxRegistration.objectName, ExceptionUtils.getRootCauseMessage(e));
		} finally {
			// unget service
			context.ungetService(reference);
		}
	}
}
