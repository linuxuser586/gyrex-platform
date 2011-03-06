/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.p2.repositories;

import java.net.URI;

import org.eclipse.gyrex.common.identifiers.IdHelper;

import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * A p2 repository definition.
 * <p>
 * A repository definition is used to define a combined metadata and artifact
 * repository for underlying p2 operations.
 * </p>
 */
public final class RepositoryDefinition {

	private String id;
	private URI location;
	private String nodeFilter;

	/**
	 * Returns the id.
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the location.
	 * 
	 * @return the location
	 */
	public URI getLocation() {
		return location;
	}

	/**
	 * Returns the nodeFilter.
	 * 
	 * @return the nodeFilter
	 */
	public String getNodeFilter() {
		return nodeFilter;
	}

	/**
	 * Sets the id.
	 * 
	 * @param id
	 *            the id to set
	 */
	public void setId(final String id) {
		if (!IdHelper.isValidId(id)) {
			throw new IllegalArgumentException("invalid id");
		}
		this.id = id;
	}

	/**
	 * Sets the location.
	 * 
	 * @param location
	 *            the location to set
	 */
	public void setLocation(final URI location) {
		this.location = location;
	}

	/**
	 * Sets the node filter (LDAP syntax).
	 * 
	 * @param nodeFilter
	 *            the nodeFilter to set
	 * @throws IllegalArgumentException
	 */
	public void setNodeFilter(final String nodeFilter) throws IllegalArgumentException {
		if (StringUtils.isNotBlank(nodeFilter)) {
			try {
				FrameworkUtil.createFilter(nodeFilter);
			} catch (final InvalidSyntaxException e) {
				throw new IllegalArgumentException("Invalid node filter. Please use LDAP syntax. " + e.getMessage(), e);
			}
		}
		this.nodeFilter = StringUtils.trimToNull(nodeFilter);
	}

	@Override
	public String toString() {
		final ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
		builder.append("id", id);
		builder.append("location", location);
		builder.append("nodeFilter", nodeFilter);
		return builder.toString();
	}
}
