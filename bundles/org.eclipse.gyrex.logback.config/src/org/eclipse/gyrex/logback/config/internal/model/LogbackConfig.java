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

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.eclipse.gyrex.boot.internal.logback.LogbackConfigurator;

import ch.qos.logback.classic.jul.LevelChangePropagator;

/**
 * Logback configuration which is persisted to the cloud preferences.
 */
public class LogbackConfig {

	static void writeProperty(final XMLStreamWriter writer, final String name, final String value) throws XMLStreamException {
		writer.writeEmptyElement("property");
		writer.writeAttribute("name", name);
		writer.writeAttribute("value", value);
	}

	private List<Appender> appenders;
	private List<Logger> loggers;

	private boolean shortenStackTraces;

	private String addExceptionPattern(final String pattern) {
		if (isShortenStackTraces()) {
			return pattern + "%rootException{6}";
		} else {
			return pattern + "%rootException";
		}
	}

	public List<Appender> getAppenders() {
		if (null == appenders) {
			appenders = new ArrayList<Appender>();
		}
		return appenders;
	}

	public List<Logger> getLoggers() {
		if (null == loggers) {
			loggers = new ArrayList<Logger>();
		}
		return loggers;
	}

	private String getLongPattern() {
		return addExceptionPattern("%date{ISO8601} %30.30(%thread.%property{HOSTNAME})] %-5level %logger{36} %mdc{gyrex.contextPath, '[CTX:', '] '}%mdc{gyrex.applicationId, '[APP:', '] '}%mdc{gyrex.jobId, '[JOB:', '] '}- %msg%n");
	}

	private String getShortPattern() {
		return addExceptionPattern(LogbackConfigurator.DEFAULT_PATTERN);
	}

	public boolean isShortenStackTraces() {
		return shortenStackTraces;
	}

	public void setAppenders(final List<Appender> appenders) {
		this.appenders = appenders;
	}

	public void setLoggers(final List<Logger> loggers) {
		this.loggers = loggers;
	}

	public void setShortenStackTraces(final boolean shortenStackTraces) {
		this.shortenStackTraces = shortenStackTraces;
	}

	/**
	 * Serializes the Logback configuration to the specified XML writer.
	 * 
	 * @param writer
	 * @throws XMLStreamException
	 */
	public void toXml(final XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartDocument();

		writer.writeStartElement("configuration");
		writer.writeAttribute("scan", "true");
		writer.writeAttribute("scanPeriod", "2 minutes");

		writeCommonProperties(writer);
		writeJulLevelChangePropagator(writer);

		for (final Appender appender : appenders) {
			appender.toXml(writer);
		}
		for (final Logger logger : loggers) {
			writeLogger(writer, logger);
		}

		writer.writeEndElement();

		writer.writeEndDocument();
	}

	private void writeCommonProperties(final XMLStreamWriter writer) throws XMLStreamException {
		writer.writeComment("common properties");
		writeProperty(writer, "BASE_PATH", "${gyrex.instance.area.logs:-logs}");
		writeProperty(writer, "PATTERN_SHORT", getShortPattern());
		writeProperty(writer, "PATTERN_LONG", getLongPattern());
	}

	private void writeJulLevelChangePropagator(final XMLStreamWriter writer) throws XMLStreamException {
		writer.writeComment("propagate log level changes to JUL");
		writer.writeStartElement("contextListener");
		writer.writeAttribute("class", LevelChangePropagator.class.getName());
		{
			writer.writeStartElement("resetJUL");
			writer.writeCData("true");
			writer.writeEndElement();
		}
		writer.writeEndElement();
	}

	private void writeLogger(final XMLStreamWriter writer, final Logger logger) throws XMLStreamException {
		// TODO Auto-generated method stub

	}

}
