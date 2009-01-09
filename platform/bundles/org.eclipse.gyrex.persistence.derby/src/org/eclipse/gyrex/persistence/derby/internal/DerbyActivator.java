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
package org.eclipse.cloudfree.persistence.derby.internal;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;


import org.eclipse.cloudfree.common.runtime.BaseBundleActivator;
import org.eclipse.cloudfree.persistence.storage.provider.RepositoryProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

public class DerbyActivator extends BaseBundleActivator {

	/** the plug-in id */
	public static final String PLUGIN_ID = "org.eclipse.cloudfree.persistence.derby";

	private static final String DERBY_EMBEDDED_DRIVER = "org.apache.derby.jdbc.EmbeddedDriver";

	/**
	 * Creates a new instance.
	 */
	public DerbyActivator() {
		super(PLUGIN_ID);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cloudfree.common.runtime.BaseBundleActivator#doStart(org.osgi.framework.BundleContext)
	 */
	@Override
	protected void doStart(final BundleContext context) throws Exception {
		// set the derby system home to the persistent bundle location
		final File systemBase = context.getDataFile("derbysystembase");
		if (null == systemBase) {
			throw new IllegalStateException("The framework must have file system support to use the Derby persistence type.");
		}
		System.setProperty("derby.system.home", systemBase.getPath());

		// initialize database
		try {
			getClass().getClassLoader().loadClass(DERBY_EMBEDDED_DRIVER).newInstance();
		} catch (final Exception e) {
			throw new IllegalStateException("Could not load Derby database driver: " + e.getMessage());
		}

		// register repository type
		final Dictionary<String, Object> serviceProperties = new Hashtable<String, Object>(2);
		serviceProperties.put(Constants.SERVICE_VENDOR, "CloudFree Platform");
		serviceProperties.put(Constants.SERVICE_DESCRIPTION, "Embedded Derby JDBC Database Repository Type");
		context.registerService(RepositoryProvider.class.getName(), new DerbyRepositoryType(), serviceProperties);
	}
}
