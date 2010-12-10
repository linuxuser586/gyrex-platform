/*******************************************************************************
 * Copyright (c) 2010 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.monitoring.internal.mbeans;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.ReflectionException;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanConstructorInfoSupport;
import javax.management.openmbean.OpenMBeanInfoSupport;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.SimpleType;

import org.eclipse.gyrex.monitoring.metrics.MetricSet;

import org.osgi.framework.ServiceReference;

/**
 * {@link MetricSetJmxMBean} implementation.
 */
public class MetricSetMBean implements DynamicMBean {
	private static final String ID = "Id";

	private final MetricSet metricSet;
	private final ServiceReference<MetricSet> reference;

	/**
	 * Creates a new instance.
	 * 
	 * @param reference
	 */
	public MetricSetMBean(final MetricSet metricSet, final ServiceReference<MetricSet> reference) {
		this.metricSet = metricSet;
		this.reference = reference;
	}

	@Override
	public Object getAttribute(final String attributeName) throws AttributeNotFoundException, MBeanException, ReflectionException {
		if (ID.equals(attributeName)) {
			return metricSet.getId();
		}

		throw new AttributeNotFoundException(String.format("attribute %s not found", attributeName));
	}

	@Override
	public AttributeList getAttributes(final String[] attributes) {
		final AttributeList attributeList = new AttributeList();
		for (final String attributeName : attributes) {
			try {
				attributeList.add(new Attribute(attributeName, getAttribute(attributeName)));
			} catch (final AttributeNotFoundException e) {
				// ignore
			} catch (final MBeanException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (final ReflectionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return attributeList;
	}

	public String getId() {
		return metricSet.getId();
	}

	@Override
	public MBeanInfo getMBeanInfo() {
		final OpenMBeanAttributeInfoSupport[] attributes = new OpenMBeanAttributeInfoSupport[1];
		final OpenMBeanConstructorInfoSupport[] constructors = new OpenMBeanConstructorInfoSupport[0];
		final OpenMBeanOperationInfoSupport[] operations = new OpenMBeanOperationInfoSupport[0];
		final MBeanNotificationInfo[] notifications = new MBeanNotificationInfo[0];

		// just one attribute
		attributes[0] = new OpenMBeanAttributeInfoSupport(ID, "MetricSet Id", SimpleType.STRING, true, false, false);

		// no arg constructor
//		constructors[0] = new OpenMBeanConstructorInfoSupport("MetricSetMXBean", "Constructs a MetricSetMXBean instance.", new OpenMBeanParameterInfoSupport[0]);

		// build the info
		return new OpenMBeanInfoSupport(this.getClass().getName(), metricSet.getDescription(), attributes, constructors, operations, notifications);
	}

	@Override
	public Object invoke(final String actionName, final Object[] params, final String[] signature) throws MBeanException, ReflectionException {
		// not supported
		return null;
	}

	@Override
	public void setAttribute(final Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
		// not supported
	}

	@Override
	public AttributeList setAttributes(final AttributeList attributes) {
		// not supported
		return null;
	}

}
