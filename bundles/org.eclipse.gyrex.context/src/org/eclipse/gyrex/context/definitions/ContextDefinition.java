/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context.definitions;

import org.eclipse.core.runtime.IPath;

import org.apache.commons.lang.StringUtils;

/**
 * A context definition.
 * <p>
 * Note, this class is part of an administration API which may evolve faster
 * than the general contextual runtime API. Please get in touch with the
 * development team through the prefered channels listed on <a
 * href="http://www.eclipse.org/gyrex">the Gyrex website</a> to stay up-to-date
 * of possible changes.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ContextDefinition {

	private final IPath path;
	private String name;

	/**
	 * Creates a new instance.
	 * 
	 * @param path
	 *            the context path
	 */
	public ContextDefinition(final IPath path) {
		if (path == null)
			throw new IllegalArgumentException("path must not be null");
		this.path = path;
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
	 * Returns the path.
	 * 
	 * @return the path
	 */
	public IPath getPath() {
		return path;
	}

	/**
	 * Sets the name.
	 * 
	 * @param name
	 *            the name to set
	 */
	public void setName(final String name) {
		this.name = StringUtils.isNotBlank(name) ? name : null;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(path);
		if (null != name) {
			builder.append(" (").append(name).append(")");
		}
		return builder.toString();
	}

}
