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

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

@XmlEnum
public enum Level {
	@XmlEnumValue(value = "DEBUG")
	DEBUG, @XmlEnumValue(value = "INFO")
	INFO, @XmlEnumValue(value = "WARN")
	WARN, @XmlEnumValue(value = "ERROR")
	ERROR, @XmlEnumValue(value = "ALL")
	ALL, @XmlEnumValue(value = "OFF")
	OFF,
}