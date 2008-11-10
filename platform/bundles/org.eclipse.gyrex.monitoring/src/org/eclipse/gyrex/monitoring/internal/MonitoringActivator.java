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
package org.eclipse.cloudfree.monitoring.internal;

import org.eclipse.cloudfree.common.runtime.BaseBundleActivator;

public class MonitoringActivator extends BaseBundleActivator {

	/** plug-in id */
	public static final String PLUGIN_ID = "org.eclipse.cloudfree.monitoring";

	/**
	 * Creates a new instance.
	 */
	public MonitoringActivator() {
		super(PLUGIN_ID);
	}
}
