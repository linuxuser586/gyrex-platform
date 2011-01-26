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
package org.eclipse.gyrex.http.jetty.admin;

import org.eclipse.gyrex.common.identifiers.IdHelper;

import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Description of a web channel
 */
public class ChannelDescriptor {

	private String id;
	private boolean secure;
	private String certificateId;
	private String secureChannelId;
	private int port;
	private String nodeFilter;

	/**
	 * Returns the certificateId.
	 * 
	 * @return the certificateId
	 */
	public String getCertificateId() {
		return certificateId;
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

	/**
	 * Returns the port.
	 * 
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Returns the secureChannelId.
	 * 
	 * @return the secureChannelId
	 */
	public String getSecureChannelId() {
		return secureChannelId;
	}

	/**
	 * Returns the secure.
	 * 
	 * @return the secure
	 */
	public boolean isSecure() {
		return secure;
	}

	/**
	 * Sets the certificateId.
	 * 
	 * @param certificateId
	 *            the certificateId to set
	 */
	public void setCertificateId(final String certificateId) {
		if (!IdHelper.isValidId(id)) {
			throw new IllegalArgumentException("invalid id");
		}
		this.certificateId = certificateId;
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
		if (nodeFilter != null) {
			try {
				FrameworkUtil.createFilter(nodeFilter);
			} catch (final InvalidSyntaxException e) {
				throw new IllegalArgumentException("Invalid node filter. Please use LDAP syntax. " + e.getMessage(), e);
			}
		}
		this.nodeFilter = nodeFilter;
	}

	/**
	 * Sets the port.
	 * 
	 * @param port
	 *            the port to set
	 */
	public void setPort(final int port) {
		if ((port <= 0) || (port > 65535)) {
			throw new IllegalArgumentException("invalid port: " + port);
		}
		this.port = port;
	}

	/**
	 * Sets the secure.
	 * 
	 * @param secure
	 *            the secure to set
	 */
	public void setSecure(final boolean secure) {
		this.secure = secure;
	}

	/**
	 * Sets the secureChannelId.
	 * 
	 * @param secureChannelId
	 *            the secureChannelId to set
	 */
	public void setSecureChannelId(final String secureChannelId) {
		if (!IdHelper.isValidId(id)) {
			throw new IllegalArgumentException("invalid id");
		}
		this.secureChannelId = secureChannelId;
	}

	@Override
	public String toString() {
		final ToStringBuilder builder = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
		builder.append("id", id);
		builder.append("port", port);
		builder.append("secure", secure);
		builder.append("certificateId", certificateId);
		builder.append("secureChannelId", secureChannelId);
		return builder.toString();
	}
}
