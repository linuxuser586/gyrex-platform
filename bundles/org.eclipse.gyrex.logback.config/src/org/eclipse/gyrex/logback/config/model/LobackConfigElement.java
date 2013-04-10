/*******************************************************************************
 * Copyright (c) 2013 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.logback.config.model;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Base class for Logback configuration model objects.
 */
public abstract class LobackConfigElement {

	/**
	 * Serializes the Logback model object to the specified XML writer.
	 * <p>
	 * The XML is expected to be readable by Logback. As such, it depends
	 * heavily on Logback and may be bound to different evolution/compatibility
	 * rules.
	 * </p>
	 * 
	 * @param writer
	 *            the stream writer
	 * @throws XMLStreamException
	 */
	public abstract void toXml(final XMLStreamWriter writer) throws XMLStreamException;

}
