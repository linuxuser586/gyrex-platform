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
package org.eclipse.gyrex.logback.config.internal.model;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.StringUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.sift.MDCBasedDiscriminator;
import ch.qos.logback.classic.sift.SiftingAppender;

public abstract class Appender {

	private String name;
	private String pattern;
	private Level threshold;

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
		return "";
	}

	/**
	 * Returns the name of the property that should be used for determining the
	 * log output if {@link #isSeparateLogOutputsPerMdcProperty() sifting} is
	 * enabled.
	 * 
	 * @return the name of the property (may be <code>null</code>)
	 */
	public String getSiftingMdcPropertyName() {
		return "gyrex.contextPath";
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
	 * Indicates if log output should be written to different targets (eg.
	 * files) based on an MDC property value.
	 * 
	 * @return <code>true</code> if separate log outputs should be created,
	 *         <code>false</code> otherwise
	 */
	public boolean isSeparateLogOutputsPerMdcProperty() {
		return false; /*false by default */
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
	 * Sets the threshold.
	 * 
	 * @param threshold
	 *            the threshold to set
	 */
	public void setThreshold(final Level threshold) {
		this.threshold = threshold;
	}

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
			text = "${PATTERN_LONG}";
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
