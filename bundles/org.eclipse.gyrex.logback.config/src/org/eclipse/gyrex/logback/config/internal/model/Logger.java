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

import ch.qos.logback.classic.Level;

public class Logger {

	private String name;
	private Level level;
	private List<String> appenderReferences;
	private boolean inheritOtherAppenders = true;

	/**
	 * Returns the appenders.
	 * 
	 * @return the appenders
	 */
	public List<String> getAppenderReferences() {
		if (null == appenderReferences) {
			appenderReferences = new ArrayList<String>();
		}
		return appenderReferences;
	}

	/**
	 * Returns the level.
	 * 
	 * @return the level
	 */
	public Level getLevel() {
		return level;
	}

	/**
	 * Returns the name.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the inheritOtherAppenders.
	 * 
	 * @return the inheritOtherAppenders
	 */
	public boolean isInheritOtherAppenders() {
		return inheritOtherAppenders;
	}

	/**
	 * Sets the appenders.
	 * 
	 * @param appenders
	 *            the appenders to set
	 */
	public void setAppenderReferences(final List<String> appenders) {
		appenderReferences = appenders;
	}

	/**
	 * Sets the inheritOtherAppenders.
	 * 
	 * @param inheritOtherAppenders
	 *            the inheritOtherAppenders to set
	 */
	public void setInheritOtherAppenders(final boolean inheritOtherAppenders) {
		this.inheritOtherAppenders = inheritOtherAppenders;
	}

	/**
	 * Sets the level.
	 * 
	 * @param level
	 *            the level to set
	 */
	public void setLevel(final Level level) {
		this.level = level;
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

	public void toXml(final XMLStreamWriter writer) throws XMLStreamException {
		final List<String> appenderRefs = getAppenderReferences();
		if (appenderRefs.isEmpty()) {
			writer.writeEmptyElement("logger");
		} else {
			writer.writeStartElement("logger");
		}
		writer.writeAttribute("name", getName());
		writer.writeAttribute("level", getLevel().toString());
		if (!isInheritOtherAppenders()) {
			writer.writeAttribute("additivity", Boolean.FALSE.toString());
		}
		for (final String appenderRef : appenderRefs) {
			writer.writeEmptyElement("appender-ref");
			writer.writeAttribute("ref", appenderRef);
		}
		if (!appenderRefs.isEmpty()) {
			writer.writeEndElement();
		}
	}

}
