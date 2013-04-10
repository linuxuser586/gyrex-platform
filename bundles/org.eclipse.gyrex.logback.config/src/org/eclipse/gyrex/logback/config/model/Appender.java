/**
 * Copyright (c) 2012 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.logback.config.model;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.logback.config.spi.AppenderProvider;

import org.apache.commons.lang.StringUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.sift.MDCBasedDiscriminator;
import ch.qos.logback.classic.sift.SiftingAppender;

/**
 * Base class for Logback Appender elements in the Logback configuration.
 */
public abstract class Appender extends LobackConfigElement {

	private final String typeId;

	private String name;
	private String pattern;
	private Level threshold;

	private String siftingMdcPropertyName;
	private String siftingMdcPropertyDefaultValue;

	/**
	 * Creates a new instance.
	 */
	public Appender(final String typeId) {
		if (!IdHelper.isValidId(typeId))
			throw new IllegalArgumentException("invalid type id: " + typeId);
		this.typeId = typeId;
	}

	/**
	 * Indicates if the appender can be wrapped into a sifting appender.
	 * 
	 * @return <code>true</code> if it can be wrapped, <code>false</code>
	 *         otherwise
	 */
	public boolean canSift() {
		return false;
	}

	/**
	 * Returns the appender class name.
	 * 
	 * @return
	 */
	protected abstract String getAppenderClassName();

	/**
	 * Returns the name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the pattern.
	 * 
	 * @return the pattern
	 */
	public String getPattern() {
		return pattern;
	}

	/**
	 * Returns the default value if no MDC property is set with the name
	 * returned by {@link #getSiftingMdcPropertyName()}.
	 * 
	 * @return a default value
	 */
	public String getSiftingMdcPropertyDefaultValue() {
		return siftingMdcPropertyDefaultValue;
	}

	/**
	 * Returns the name of the property that should be used for determining the
	 * log output if {@link #isSeparateLogOutputsPerMdcProperty() sifting} is
	 * enabled.
	 * 
	 * @return the name of the property (may be <code>null</code>)
	 */
	public String getSiftingMdcPropertyName() {
		return siftingMdcPropertyName;
	}

	/**
	 * Returns the threshold.
	 * 
	 * @return the threshold
	 */
	public Level getThreshold() {
		return threshold;
	}

	/**
	 * Returns the appender type id.
	 * <p>
	 * The id will be used to identify the {@link AppenderProvider} responsible
	 * for reading and writing the appender.
	 * </p>
	 * 
	 * @return the type id
	 */
	public final String getTypeId() {
		return typeId;
	}

	/**
	 * Indicates if log output should be written to different targets (eg.
	 * files) based on an MDC property value.
	 * 
	 * @return <code>true</code> if separate log outputs should be created,
	 *         <code>false</code> otherwise
	 */
	public boolean isSeparateLogOutputsPerMdcProperty() {
		return (null != getSiftingMdcPropertyName()) && (null != getSiftingMdcPropertyDefaultValue());
	}

	/**
	 * Indicates if the appender prefers a short pattern.
	 * 
	 * @return <code>true</code> if the short pattern is prefered,
	 *         <code>false</code> otherwise
	 */
	protected boolean preferShortPattern() {
		return false;
	}

	/**
	 * Sets the name.
	 * 
	 * @param name
	 *            the name to set
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Sets the pattern.
	 * 
	 * @param pattern
	 *            the pattern to set
	 */
	public void setPattern(final String pattern) {
		this.pattern = pattern;
	}

	/**
	 * Sets the siftingMdcPropertyDefaultValue.
	 * 
	 * @param siftingMdcPropertyDefaultValue
	 *            the siftingMdcPropertyDefaultValue to set
	 */
	public void setSiftingMdcPropertyDefaultValue(final String siftingMdcPropertyDefaultValue) {
		this.siftingMdcPropertyDefaultValue = siftingMdcPropertyDefaultValue;
	}

	/**
	 * Sets the siftingMdcPropertyName.
	 * 
	 * @param siftingMdcPropertyName
	 *            the siftingMdcPropertyName to set
	 */
	public void setSiftingMdcPropertyName(final String siftingMdcPropertyName) {
		this.siftingMdcPropertyName = siftingMdcPropertyName;
	}

	/**
	 * Sets the threshold.
	 * 
	 * @param threshold
	 *            the threshold to set
	 */
	public void setThreshold(final Level threshold) {
		this.threshold = threshold;
	}

	@Override
	public void toXml(final XMLStreamWriter writer) throws XMLStreamException {
		final boolean wrapIntoSiftingAppender = canSift() && isSeparateLogOutputsPerMdcProperty();

		writer.writeStartElement("appender");
		writer.writeAttribute("name", getName());
		if (wrapIntoSiftingAppender) {
			writer.writeAttribute("class", SiftingAppender.class.getName());
		} else {
			writer.writeAttribute("class", getAppenderClassName());
		}

		final Level threshold = getThreshold();
		if (null != threshold) {
			writer.writeStartElement("filter");
			writer.writeAttribute("class", ThresholdFilter.class.getName());
			writer.writeStartElement("level");
			writer.writeCharacters(threshold.toString());
			writer.writeEndElement();
			writer.writeEndElement();
		}

		if (wrapIntoSiftingAppender) {
			// start discriminator and wrap regular appender into <sift><appender>
			writer.writeStartElement("discriminator");
			writer.writeAttribute("class", MDCBasedDiscriminator.class.getName());
			writer.writeStartElement("key");
			writer.writeCharacters(getSiftingMdcPropertyName());
			writer.writeEndElement();
			writer.writeStartElement("defaultValue");
			writer.writeCharacters(getSiftingMdcPropertyDefaultValue());
			writer.writeEndElement();
			writer.writeEndElement();
			writer.writeStartElement("sift");
			writer.writeStartElement("appender");
			writer.writeAttribute("name", String.format("%s-${%s}", getName(), getSiftingMdcPropertyName()));
		}

		writeAppenderContent(writer);
		writeEncoder(writer);

		if (wrapIntoSiftingAppender) {
			// finish <sift><appender>
			writer.writeEndElement();
			writer.writeEndElement();
		}
		writer.writeEndElement();

	}

	protected void writeAppenderContent(final XMLStreamWriter writer) throws XMLStreamException {
		// empty
	}

	private void writeEncoder(final XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement("encoder");
		writer.writeStartElement("pattern");
		String text = getPattern();
		if (StringUtils.isBlank(text)) {
			text = preferShortPattern() ? "${PATTERN_SHORT}" : "${PATTERN_LONG}";
		} else if (StringUtils.containsIgnoreCase(text, "PATTERN_LONG")) {
			text = "${PATTERN_LONG}";
		} else if (StringUtils.containsIgnoreCase(text, "PATTERN_SHORT")) {
			text = "${PATTERN_SHORT}";
		}
		writer.writeCharacters(text);
		writer.writeEndElement();
		writer.writeEndElement();
	}
}
