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

/**
 *
 */
public class ConsoleAppender extends Appender {

	/**
	 * Creates a new instance.
	 */
	public ConsoleAppender() {
		name = "console";
		clazz = ch.qos.logback.core.ConsoleAppender.class.getName();
		final PatternEncoder p = new PatternEncoder();
		p.pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n";
		encoder = p;
	}

}
