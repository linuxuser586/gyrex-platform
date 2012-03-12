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

import javax.xml.stream.XMLStreamWriter;

/**
 * Logback configuration which is persisted to the cloud preferences.
 */
public class LogbackConfig {

	private List<Appender> appenders;
	private List<Logger> loggers;

	/**
	 * Returns the appenders.
	 * 
	 * @return the appenders
	 */
	public List<Appender> getAppenders() {
		if (null == appenders) {
			appenders = new ArrayList<Appender>();
		}
		return appenders;
	}

	/**
	 * Returns the loggers.
	 * 
	 * @return the loggers
	 */
	public List<Logger> getLoggers() {
		if (null == loggers) {
			loggers = new ArrayList<Logger>();
		}
		return loggers;
	}

	/**
	 * Sets the appenders.
	 * 
	 * @param appenders
	 *            the appenders to set
	 */
	public void setAppenders(final List<Appender> appenders) {
		this.appenders = appenders;
	}

	/**
	 * Sets the loggers.
	 * 
	 * @param loggers
	 *            the loggers to set
	 */
	public void setLoggers(final List<Logger> loggers) {
		this.loggers = loggers;
	}

	/**
	 * @param xmlStreamWriter
	 */
	public void toXml(final XMLStreamWriter xmlStreamWriter) {
		// TODO Auto-generated method stub

	}

}
