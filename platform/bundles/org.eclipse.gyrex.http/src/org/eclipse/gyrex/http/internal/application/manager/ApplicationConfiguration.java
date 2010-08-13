/*******************************************************************************
 * Copyright (c) 2008, 2009 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.internal.application.manager;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The application configuration manages application wide attributes and
 * parameters.
 */
public class ApplicationConfiguration {

	private final Map<String, String> properties;
	private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<String, Object>(1);

	/**
	 * Creates a new instance.
	 * 
	 * @param properties
	 */
	public ApplicationConfiguration(final Map<String, String> properties) {
		this.properties = properties;
	}

	public Object getAttribute(final String name) {
		return attributes.get(name);
	}

	public Enumeration getAttributeNames() {
		return Collections.enumeration(Arrays.asList(attributes.keySet().toArray()));
	}

	public URL getDefaultMountPoint() {
		// TODO read default mount point from preferences?
		return null;
	}

	public String getInitParameter(final String name) {
		return properties.get(name);
	}

	public Enumeration getInitParameterNames() {
		return Collections.enumeration(properties.keySet());
	}

	public void removeAttribte(final String name) {
		attributes.remove(name);
	}

	public void setAttribute(final String name, final Object object) {
		attributes.put(name.intern(), object);
	}

}
