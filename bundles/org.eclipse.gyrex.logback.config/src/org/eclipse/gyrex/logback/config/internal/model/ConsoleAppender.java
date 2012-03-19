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

/**
 * A simple appender which writes to the console.
 */
public class ConsoleAppender extends Appender {

	/**
	 * Creates a new instance.
	 */
	public ConsoleAppender() {
		setName("console");
	}

	@Override
	public String getAppenderClassName() {
		return ch.qos.logback.core.ConsoleAppender.class.getName();
	}
}
