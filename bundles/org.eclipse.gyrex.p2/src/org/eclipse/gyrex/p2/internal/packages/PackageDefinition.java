/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.p2.internal.packages;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
public final class PackageDefinition {

	private String id;
	private String nodeFilter;
	private Set<InstallableUnitReference> componentsToInstall;

	public void addComponentToInstall(final InstallableUnitReference component) {
		if (null == componentsToInstall) {
			componentsToInstall = new HashSet<InstallableUnitReference>(3);
		}
		if (!componentsToInstall.contains(component)) {
			componentsToInstall.add(component);
		}
	}

	/**
	 * Returns the componentsToInstall.
	 * 
	 * @return the componentsToInstall
	 */
	public Collection<InstallableUnitReference> getComponentsToInstall() {
		if (null != componentsToInstall) {
			return Collections.unmodifiableCollection(componentsToInstall);
		}
		return Collections.emptySet();
	}

	/**
	 * Returns the id.
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the nodeFilter.
	 * 
	 * @return the nodeFilter
	 */
	public String getNodeFilter() {
		return nodeFilter;
	}

	public void removeComponentToInstall(final InstallableUnitReference component) {
		if (null != componentsToInstall) {
			componentsToInstall.remove(component);
		}
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
		builder.append("componentsToInstall", componentsToInstall);
		builder.append("nodeFilter", nodeFilter);
		return builder.toString();
	}
}
