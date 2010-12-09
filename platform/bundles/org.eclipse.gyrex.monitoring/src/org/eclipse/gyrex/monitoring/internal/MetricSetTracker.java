/*******************************************************************************
 * Copyright (c) 2010 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.monitoring.internal;

import java.lang.management.ManagementFactory;
import java.util.Hashtable;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.eclipse.gyrex.monitoring.internal.mbeans.MetricSetJmx;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import org.apache.commons.lang.StringUtils;

/**
 * Tracker for {@link MetricSet}.
 */
public class MetricSetTracker extends ServiceTracker<MetricSet, MetricSet> {

	public MetricSetTracker(final BundleContext context) {
		super(context, MetricSet.class, null);
	}

	@Override
	public MetricSet addingService(final ServiceReference<MetricSet> reference) {
		final MetricSet metricSet = super.addingService(reference);
		if (metricSet == null) {
			return null;
		}

		try {
			final MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
			final ObjectName objectName = getObjectName(reference, metricSet);
			beanServer.registerMBean(new MetricSetJmx(metricSet), objectName);
		} catch (final InstanceAlreadyExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final MBeanRegistrationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final NotCompliantMBeanException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final MalformedObjectNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return metricSet;
	}

	private ObjectName getObjectName(final ServiceReference<MetricSet> reference, final MetricSet metricSet) throws MalformedObjectNameException {
		final String symbolicName = reference.getBundle().getSymbolicName();
		final Hashtable<String, String> properties = new Hashtable<String, String>(2);
		properties.put("type", "MetricSet");
		properties.put("name", '"' + StringUtils.removeStart(metricSet.getId(), symbolicName) + '"');
		final ObjectName objectName = new ObjectName(symbolicName, properties);
		return objectName;
	}

	@Override
	public void removedService(final ServiceReference<MetricSet> reference, final MetricSet metricSet) {
		try {
			final MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
			final ObjectName objectName = getObjectName(reference, metricSet);
			beanServer.unregisterMBean(objectName);
		} catch (final MalformedObjectNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final MBeanRegistrationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final InstanceNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// unget service
		super.removedService(reference, metricSet);
	}
}
