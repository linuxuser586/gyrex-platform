/**
 * Copyright (c) 2009 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.http.registry;

import org.eclipse.gyrex.http.application.Application;

/**
 * A customizer to allow further customization of {@link Application HTTP
 * applications} created by the HTTP Application Registry.
 * <p>
 * The customizer is invoked to participate in the lifecycle and request
 * handling of HTTP applications created by the registry.
 * </p>
 */
public abstract class ApplicationCustomizer {

	/**
	 * Called when an application instance has been destroyed.
	 * <p>
	 * Default implementation does nothing. Subclasses may overwrite.
	 * </p>
	 * 
	 * @param application
	 *            the application instance
	 */
	public void onDestroy(final Application application) {
		// empty
	}

	/**
	 * Called when an application instance has been initialized.
	 * <p>
	 * Default implementation does nothing. Subclasses may overwrite.
	 * </p>
	 * 
	 * @param application
	 *            the application instance
	 */
	public void onInit(final Application application) {
		// empty
	}

}
