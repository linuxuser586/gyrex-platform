/*******************************************************************************
 * Copyright (c) 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Andreas Mihm - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.http.staticresource;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.http.application.Application;
import org.eclipse.gyrex.http.application.context.IResourceProvider;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.Path;
import org.eclipse.jetty.servlet.ErrorPageErrorHandler;

import org.osgi.framework.Bundle;

/**
 * a gyrex application, which serves static content (html, js, css). This
 * application shall be used for creating html5 frontends in gyrex
 * <p>
 * the actual application instance is created as a declarative service
 * declaration in a bundle, which holds the static content. The required service
 * property resource.path needs to contain the folder inside the bundle e.g.
 * "/web", which holds the static content.
 * <p>
 * There is also another option available in Gyrex Dev Mode Only (see
 * {@link Platform}) of serving static content just from any other directory by
 * setting an environment varaiable containing the fully qualified directory
 * path and set the name of this environment variable as the DS property
 * resource.devModeDocRootEnvVar. The goal of this option is to make the html
 * development process easier.
 */
public class StaticResourceApplication extends Application {

	private final Bundle bundle;
	private final String bundleResourcePath;
	private final String devModeDocRootEnvVar;

	/**
	 * Creates a new instance.
	 * 
	 * @param applicationId
	 * @param context
	 * @param bundle
	 * @param bundleResourcePath
	 *            the bundle resource path which holds the static content
	 * @param devModeDocRootEnvVar
	 *            the name of the environment variable, which holds the path to
	 *            static content (only used in dev mode {@link Platform}) this
	 *            setting overrides a bundleresource path
	 */
	public StaticResourceApplication(final String applicationId, final IRuntimeContext context, final Bundle bundle, final String bundleResourcePath, final String devModeDocRootEnvVar) {
		super(applicationId, context);
		this.bundle = bundle;
		this.bundleResourcePath = bundleResourcePath;
		this.devModeDocRootEnvVar = devModeDocRootEnvVar;
	}

	@Override
	protected void doInit() throws IllegalStateException, Exception {

		// registering jetty resources
		getApplicationContext().registerResources("/", "", new IResourceProvider() {

			@Override
			public URL getResource(final String path) throws MalformedURLException {
				// if gyrex platform runs in dev mode and the service definition contains a ENV variable which is present in the environment and contains a directory, then this path is registered with jetty 
				if (Platform.inDevelopmentMode() && (devModeDocRootEnvVar != null) && (System.getenv(devModeDocRootEnvVar) != null)) {
					final File file = new File(System.getenv(devModeDocRootEnvVar), path);
					if (file.exists())
						return file.toURI().toURL();
					return null;
				}
				// else the configured folder inside the bundle is used for serving the static resources 
				return bundle.getEntry(new Path(bundleResourcePath).append(path).toString());
			}

			@Override
			public Set<String> getResourcePaths(final String path) {
				// this method is not called through jetty integration in gyrex 
				return null;
			}
		});
	}

	@Override
	public Object getAdapter(final Class adapter) {
		if (adapter == org.eclipse.jetty.server.handler.ErrorHandler.class) {
			final ErrorPageErrorHandler errorHandler = new ErrorPageErrorHandler();
			errorHandler.addErrorPage(404, "/error/404.html");
			errorHandler.addErrorPage(ErrorPageErrorHandler.GLOBAL_ERROR_PAGE, "/error/error.html");
			return errorHandler;
		}
		return super.getAdapter(adapter);
	}
}
