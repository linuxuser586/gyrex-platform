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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanConstructorInfoSupport;
import javax.management.openmbean.OpenMBeanInfoSupport;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.eclipse.gyrex.monitoring.metrics.BaseMetric;
import org.eclipse.gyrex.monitoring.metrics.MetricAttribute;
import org.eclipse.gyrex.monitoring.metrics.MetricSet;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import org.apache.commons.lang.StringUtils;

/**
 * {@link MetricSetJmxMBean} implementation.
 */
public class MetricSetMBean implements DynamicMBean {

	private static final String PROPERTIES = "properties";
	private static final String DESCRIPTION = "description";
	private static final String ID = "id";

	private final MetricSet metricSet;
	private final ServiceReference<MetricSet> reference;

	private MBeanInfo beanInfo;
	private TabularDataSupport properties;
	private CompositeType propertyType;
	private TabularType propertyTableType;
	private Map<String, CompositeType> metricTypesByAttributeName;
	private Map<String, BaseMetric> metricByAttributeName;

	/**
	 * Creates a new instance.
	 * 
	 * @param reference
	 */
	public MetricSetMBean(final MetricSet metricSet, final ServiceReference<MetricSet> reference) {
		this.metricSet = metricSet;
		this.reference = reference;
		initialize();
	}

	@Override
	public Object getAttribute(final String attributeName) throws AttributeNotFoundException, MBeanException, ReflectionException {
		if (ID.equals(attributeName)) {
			return metricSet.getId();
		} else if (DESCRIPTION.equals(attributeName)) {
			return metricSet.getDescription();
		} else if (PROPERTIES.equals(attributeName)) {
			return properties;
		} else if (metricTypesByAttributeName.containsKey(attributeName)) {
			final CompositeType type = metricTypesByAttributeName.get(attributeName);
			final BaseMetric metric = metricByAttributeName.get(attributeName);
			if ((type != null) && (metric != null)) {
				final Map<String, ?> rawValues = metric.getAttributeValues();
				final String[] metricAttributeNames = type.keySet().toArray(new String[0]);
				final Object[] metricValues = new Object[metricAttributeNames.length];
				for (int i = 0; i < metricValues.length; i++) {
					metricValues[i] = String.valueOf(rawValues.get(metricAttributeNames[i]));
				}
				try {
					return new CompositeDataSupport(type, metricAttributeNames, metricValues);
				} catch (final OpenDataException e) {
					throw new MBeanException(e);
				}
			}
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
		return beanInfo;
	}

	private CompositeType getType(final BaseMetric metric) throws OpenDataException {
		// get attributes
		final List<MetricAttribute> attributes = metric.getAttributes();

		// collect names, descriptions and types
		final List<String> names = new ArrayList<String>(attributes.size());
		final List<String> descriptions = new ArrayList<String>(attributes.size());
		final List<OpenType> types = new ArrayList<OpenType>(attributes.size());
		for (final MetricAttribute attribute : attributes) {
			// at this type we only support types that can be converted to strings
			final Class type = attribute.getType();
			if (isConvertibleToString(type)) {
				names.add(attribute.getName());
				descriptions.add(attribute.getDescription());
				types.add(SimpleType.STRING);
			}
		}

		return new CompositeType(metric.getClass().getSimpleName(), "Metric of type " + metric.getClass().getName(), names.toArray(new String[names.size()]), descriptions.toArray(new String[descriptions.size()]), types.toArray(new OpenType[types.size()]));
	}

	private void initialize() {
		final List<OpenMBeanAttributeInfoSupport> attributes = new ArrayList<OpenMBeanAttributeInfoSupport>();
		final OpenMBeanConstructorInfoSupport[] constructors = new OpenMBeanConstructorInfoSupport[0];
		final OpenMBeanOperationInfoSupport[] operations = new OpenMBeanOperationInfoSupport[0];
		final MBeanNotificationInfo[] notifications = new MBeanNotificationInfo[0];

		// common attribute
		attributes.add(new OpenMBeanAttributeInfoSupport(ID, "MetricSet Id", SimpleType.STRING, true, false, false));
		attributes.add(new OpenMBeanAttributeInfoSupport(DESCRIPTION, "MetricSet Description", SimpleType.STRING, true, false, false));

		// service property attributes
		try {
			final String[] propertyTypeNames = new String[] { "key", "value" };
			propertyType = new CompositeType("property", "A property with name and value.", propertyTypeNames, new String[] { "Name", "Value" }, new OpenType[] { SimpleType.STRING, SimpleType.STRING });
			propertyTableType = new TabularType(PROPERTIES, "A lst of properties.", propertyType, new String[] { "key" });
			attributes.add(new OpenMBeanAttributeInfoSupport(PROPERTIES, "MetricSet Properties", propertyTableType, true, false, false));

			// pre-build service properties
			properties = new TabularDataSupport(propertyTableType);
			final String[] propertyKeys = reference.getPropertyKeys();
			for (final String serviceProperty : propertyKeys) {
				if (serviceProperty.startsWith("gyrex.") || serviceProperty.equals(Constants.SERVICE_DESCRIPTION) || serviceProperty.equals(Constants.SERVICE_VENDOR)) {
					final Object value = reference.getProperty(serviceProperty);
					if (value == null) {
						continue;
					}
					if (isConvertibleToString(value.getClass())) {
						final Object[] values = { serviceProperty, String.valueOf(value) };
						properties.put(new CompositeDataSupport(propertyType, propertyTypeNames, values));
					}
				}
			}
		} catch (final OpenDataException e) {
			attributes.add(new OpenMBeanAttributeInfoSupport("propertiesError", "Exception occured while determining properties. " + e.toString(), SimpleType.STRING, true, false, false));
		}

		// metrics
		final List<BaseMetric> metrics = metricSet.getMetrics();
		metricTypesByAttributeName = new HashMap<String, CompositeType>(metrics.size());
		metricByAttributeName = new HashMap<String, BaseMetric>(metrics.size());
		for (final BaseMetric metric : metrics) {
			final String attributeName = StringUtils.removeStart(metric.getId(), metricSet.getId() + ".");
			try {
				final CompositeType type = getType(metric);
				if (type != null) {
					metricTypesByAttributeName.put(attributeName, type);
					metricByAttributeName.put(attributeName, metric);
					attributes.add(new OpenMBeanAttributeInfoSupport(attributeName, metric.getId(), type, true, false, false));
				}
			} catch (final OpenDataException e) {
				attributes.add(new OpenMBeanAttributeInfoSupport(attributeName + "Error", "Exception occured while determining properties. " + e.toString(), SimpleType.STRING, true, false, false));
			}
		}

		// build the info
		beanInfo = new OpenMBeanInfoSupport(this.getClass().getName(), metricSet.getDescription(), attributes.toArray(new OpenMBeanAttributeInfoSupport[attributes.size()]), constructors, operations, notifications);
	}

	@Override
	public Object invoke(final String actionName, final Object[] params, final String[] signature) throws MBeanException, ReflectionException {
		// not supported
		return null;
	}

	private boolean isConvertibleToString(final Class type) {
		return (type == String.class) || type.isPrimitive() || (type == Boolean.class) || Number.class.isAssignableFrom(type);
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
