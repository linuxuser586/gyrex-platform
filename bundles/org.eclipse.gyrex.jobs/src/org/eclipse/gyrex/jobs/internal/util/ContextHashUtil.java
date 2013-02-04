/*******************************************************************************
 * Copyright (c) 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.internal.util;

import java.io.UnsupportedEncodingException;

import org.eclipse.gyrex.context.IRuntimeContext;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;

/**
 * A utility for generating consistent context hashes for a context.
 */
public class ContextHashUtil {

	private static final String SEPARATOR = "_";

	public static String getExternalId(final String internalId) {
		final int i = internalId.indexOf(SEPARATOR);
		if (i < 0)
			return internalId;
		return internalId.substring(i + 1);
	}

	private static String getInternalIdPrefix(final IRuntimeContext context) {
		try {
			return DigestUtils.shaHex(context.getContextPath().toString().getBytes(CharEncoding.UTF_8)) + SEPARATOR;
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException("Please use a JVM that supports UTF-8.");
		}
	}

	private final String internalIdPrefix;

	/**
	 * Creates a new instance.
	 */
	public ContextHashUtil(final IRuntimeContext context) {
		internalIdPrefix = getInternalIdPrefix(context);
	}

	/**
	 * @return <code>true</code> if the specified internal id starts with the
	 *         same hash used by this util
	 */
	public boolean isInternalId(final String internalId) {
		return (internalId != null) && internalId.startsWith(internalIdPrefix);
	}

	public String toExternalId(final String internalId) {
		return StringUtils.removeStart(internalId, internalIdPrefix);
	}

	public String toInternalId(final String id) {
		return internalIdPrefix.concat(id);
	}

	@Override
	public String toString() {
		return internalIdPrefix;
	}
}
