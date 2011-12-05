/*******************************************************************************
 * Copyright (c) 2008, 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.derby.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.server.Platform;

import org.eclipse.core.runtime.IPath;

import org.osgi.framework.BundleContext;

public class DerbyActivator extends BaseBundleActivator {

	/** the plug-in id */
	public static final String PLUGIN_ID = "org.eclipse.gyrex.persistence.derby";

	private static final String DERBY_EMBEDDED_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

	/**
	 * Creates a new instance.
	 */
	public DerbyActivator() {
		super(PLUGIN_ID);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		// set the derby system home to the workspace location
		// (but only if it's not set externally)
		AccessController.doPrivileged(new PrivilegedAction<Object>() {
			@Override
			public Object run() {
				if (null == System.getProperty("derby.system.home")) {
					final IPath systemBase = Platform.getInstanceLocation().append("derby");
					if (null == systemBase) {
						throw new IllegalStateException("The framework must have file system support to use the Derby persistence type.");
					}
					System.setProperty("derby.system.home", systemBase.toOSString());
				}
				return null;
			}
		});

		// initialize database
		try {
			getBundle().loadClass(DERBY_EMBEDDED_DRIVER).newInstance();
		} catch (final Exception e) {
			throw new IllegalStateException("Could not load Derby database driver: " + e.getMessage());
		}
	}
}
