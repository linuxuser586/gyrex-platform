/**
 * Copyright (c) 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.logback.config.internal.xml;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 *
 */
@XmlRootElement(name = "appender-ref")
public class AppenderRef extends ConfigElement {

	@XmlTransient
	private Appender appender;

	/**
	 * Creates a new instance.
	 */
	public AppenderRef() {
		// empty
	}

	/**
	 * Creates a new instance.
	 * 
	 * @param appender
	 */
	public AppenderRef(final Appender appender) {
		setAppender(appender);
	}

	@XmlAttribute(name = "ref", required = true)
	public String getRef() {
		if (null == appender) {
			return null;
		}
		return appender.name;
	}

	private void setAppender(final Appender appender) {
		this.appender = appender;
	}

}
