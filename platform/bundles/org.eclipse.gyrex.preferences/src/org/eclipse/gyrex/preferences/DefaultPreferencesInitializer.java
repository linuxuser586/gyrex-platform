/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.preferences;

import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

/**
 * Abstract class used to aid in default preference value initialization.
 * <p>
 * This class is based on {@link AbstractPreferenceInitializer} and provides a
 * specialized implementation of
 * {@link AbstractPreferenceInitializer#initializeDefaultPreferences()} which
 * delegates to protected methods depending on the operation mode.
 * </p>
 * <p>
 * As an alternative to registering a subclass of this with the
 * <code>org.eclipse.core.runtime.preferences</code> extension point clients may
 * also simply register them as OSGi services using this class name as service
 * name. The Gyrex platform will ensure that
 * {@link #initializeDefaultPreferences()} is called on every new registered
 * services. This behavior might be desired for clients that do not wish to use
 * the extension registry. However, {@link #initializeDefaultPreferences()} is
 * called on every registered service. Thus, if multiple versions of your
 * bundles are active, care must be taken to not override the defaults of the
 * others.
 * </p>
 * 
 * @see AbstractPreferenceInitializer
 */
public abstract class DefaultPreferencesInitializer extends AbstractPreferenceInitializer {

	/** OSGi service name */
	public static final String SERVICE_NAME = DefaultPreferencesInitializer.class.getName();

	/**
	 * Default constructor for the class.
	 */
	public DefaultPreferencesInitializer() {
		super();
	}

	/**
	 * This method is called by {@link #initializeDefaultPreferences()} to
	 * initialize default preference values for
	 * {@link ConfigurationMode#DEVELOPMENT development mode}.
	 * <p>
	 * Clients should get the correct node for their bundle and then set the
	 * default values on it. For example:
	 * 
	 * <pre>
	 * public void initializeDefaultProductionPreferences() {
	 * 	Preferences node = new DefaultScope().getNode(&quot;my.bundle.id&quot;);
	 * 	node.put(key, value);
	 * }
	 * </pre>
	 * 
	 * </p>
	 * <p>
	 * <em>Note: Clients should only set default preference values for their
	 * own bundle.</em>
	 * </p>
	 * <p>
	 * <em>Note:</em> Clients should not call this method. It will be called
	 * automatically by the preference initializer when the appropriate default
	 * preference node is accessed.
	 * </p>
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected abstract void initializeDefaultDevelopmentPreferences();

	/**
	 * This method is called by the preference initializer to initialize default
	 * preference values.
	 * <p>
	 * This implementation checks the platform
	 * {@link PlatformConfiguration#getConfigurationMode() configuration mode}
	 * and delegates to {@link #initializeDefaultDevelopmentPreferences()} or
	 * {@link #initializeDefaultProductionPreferences()}.
	 * </p>
	 * <p>
	 * <em>Note:</em> Clients should not call this method. It will be called
	 * automatically by the preference initializer when the appropriate default
	 * preference node is accessed.
	 * </p>
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 */
	@Override
	public final void initializeDefaultPreferences() {
		if (Platform.inDevelopmentMode()) {
			initializeDefaultDevelopmentPreferences();
		} else {
			initializeDefaultProductionPreferences();
		}

	}

	/**
	 * This method is called by {@link #initializeDefaultPreferences()} to
	 * initialize default preference values for
	 * {@link ConfigurationMode#PRODUCTION production mode}.
	 * <p>
	 * Clients should get the correct node for their bundle and then set the
	 * default values on it. For example:
	 * 
	 * <pre>
	 * public void initializeDefaultProductionPreferences() {
	 * 	Preferences node = new DefaultScope().getNode(&quot;my.bundle.id&quot;);
	 * 	node.put(key, value);
	 * }
	 * </pre>
	 * 
	 * </p>
	 * <p>
	 * <em>Note: Clients should only set default preference values for their
	 * own bundle.</em>
	 * </p>
	 * <p>
	 * <em>Note:</em> Clients should not call this method. It will be called
	 * automatically by the preference initializer when the appropriate default
	 * preference node is accessed.
	 * </p>
	 * 
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected abstract void initializeDefaultProductionPreferences();

}
