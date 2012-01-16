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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 */
@XmlRootElement(name = "contextListener")
public class LevelChangePropagator extends ContextListener {

	@XmlElement(name = "resetJUL")
	public Boolean resetJul;

	/**
	 * Creates a new instance.
	 */
	public LevelChangePropagator() {
		clazz = ch.qos.logback.classic.jul.LevelChangePropagator.class.getName();
		resetJul = Boolean.TRUE;
	}

}
