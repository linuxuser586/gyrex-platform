/*******************************************************************************
 * Copyright (c) 2013 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.logback.config.spi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.gyrex.common.identifiers.IdHelper;
import org.eclipse.gyrex.logback.config.model.Appender;

import org.eclipse.core.runtime.PlatformObject;

import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;

/**
 * A provider for custom Logback Appenders.
 * <p>
 * Appender providers can be dynamically registered to Gyrex by registering
 * {@link AppenderProvider} instances as OSGi services (using
 * {@link #SERVICE_NAME}). Appender providers are considered core elements of
 * Gyrex. Security restrictions may be used to only allow a set of well known
 * (i.e. trusted) providers.
 * </p>
 * <p>
 * Appender providers do not represent a concrete appender. They will be used,
 * however, to create valid Logback appender configurations.
 * </p>
 * <p>
 * This class must be subclassed by clients that want to contribute an appender
 * provider to Gyrex. However, it is typically not referenced directly outside
 * Gyrex. It's also part of a service provider API that may evolve faster then
 * other APIs.
 * </p>
 */
public abstract class AppenderProvider extends PlatformObject {

	/** the OSGi service name */
	public static final String SERVICE_NAME = AppenderProvider.class.getName();

	/** provided appender type ids */
	private final List<String> providedTypeIds;

	/**
	 * Creates a new instance using the specified provider id.
	 * 
	 * @param providedTypeIds
	 *            the ids of the provided appenders (may not be
	 *            <code>null</code> or empty, will be
	 *            {@link IdHelper#isValidId(String) validated})
	 */
	protected AppenderProvider(final String... providedTypeIds) {
		if ((null == providedTypeIds) || (providedTypeIds.length == 0))
			throw new IllegalArgumentException("appender types must not be null or empty");

		// validate & save
		this.providedTypeIds = new ArrayList<String>(providedTypeIds.length);
		for (final String id : providedTypeIds) {
			if (!IdHelper.isValidId(id))
				throw new IllegalArgumentException(String.format("type id \"%s\" is invalid; valid chars are US-ASCII a-z / A-Z / 0-9 / '.' / '-' / '_'", id));
			else {
				this.providedTypeIds.add(id);
			}
		}
	}

	/**
	 * Creates and returns a new appender object of the specified type.
	 * 
	 * @param typeId
	 *            the appender type id
	 * @return the appender configuration
	 */
	public abstract Appender createAppender(String typeId) throws Exception;

	/**
	 * Returns a human readable name of the specified appender type.
	 * <p>
	 * The default implementation returns <code>null</code> for backwards
	 * compatibility which triggers a fallback of the UI the name is displayed
	 * in. Subclasses may override and return a specific name for the specified
	 * type id. If the type is unknown, <code>null</code> should be returned.
	 * </p>
	 * 
	 * @param typeId
	 *            the appender type identifier
	 * @return the type name (maybe <code>null</code>)
	 */
	public String getName(final String typeId) {
		return null;
	}

	/**
	 * Returns a list of provided appender type identifiers.
	 * 
	 * @return an unmodifiable collection of provided appender type identifiers
	 */
	public final Collection<String> getProvidedTypeIds() {
		return Collections.unmodifiableCollection(providedTypeIds);
	}

	/**
	 * Reads and returns an appender configuration from a preference node.
	 * 
	 * @param typeId
	 *            the appender type id
	 * @param node
	 *            the preference node
	 * @return the appender configuration
	 */
	public abstract Appender loadAppender(String typeId, final Preferences node) throws Exception;

	/**
	 * Returns a string containing a concise, human-readable description of the
	 * provider.
	 * 
	 * @return a string representation of the provider
	 */
	@Override
	public final String toString() {
		return getClass().getSimpleName() + " [" + StringUtils.join(providedTypeIds, ',') + "]";
	}

	/**
	 * Writes the specified appender configuration into the given preference
	 * node.
	 * <p>
	 * Note, implementors should not call {@link Preferences#flush()} on the
	 * given node in order to avoid redundant work. The node will be flushed by
	 * the framework when appropriate.
	 * </p>
	 * 
	 * @param appender
	 *            the appender to write
	 * @param node
	 *            the node to write the appender configuration to
	 */
	public abstract void writeAppender(Appender appender, Preferences node) throws Exception;

}
