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

import ch.qos.logback.core.joran.spi.ConsoleTarget;

/**
 * A simple appender which writes to the console.
 */
public class ConsoleAppender extends Appender {

	ConsoleTarget target;

	/**
	 * Creates a new instance.
	 */
	public ConsoleAppender() {
		super("console");
		setName("console");
	}

	@Override
	public String getAppenderClassName() {
		return ch.qos.logback.core.ConsoleAppender.class.getName();
	}

	/**
	 * Returns the target.
	 * 
	 * @return the target
	 */
	public ConsoleTarget getTarget() {
		return target;
	}

	@Override
	protected boolean preferShortPattern() {
		return true;
	}

	/**
	 * Sets the target.
	 * 
	 * @param target
	 *            the target to set
	 */
	public void setTarget(final ConsoleTarget target) {
		this.target = target;
	}

	@Override
	protected void writeAppenderContent(final XMLStreamWriter writer) throws XMLStreamException {
		final ConsoleTarget consoleTarget = getTarget();

		// only write console target SystemErr; SystemOut is default
		if (consoleTarget == ConsoleTarget.SystemErr) {
			writer.writeStartElement("target");
			writer.writeCharacters(ConsoleTarget.SystemErr.getName());
			writer.writeEndElement();
		}
	}

}
