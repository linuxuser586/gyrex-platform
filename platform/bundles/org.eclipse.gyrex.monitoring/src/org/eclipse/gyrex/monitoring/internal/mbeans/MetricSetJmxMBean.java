/*******************************************************************************
 * Copyright (c) 2010 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.monitoring.internal.mbeans;

import java.io.IOException;

import org.eclipse.gyrex.monitoring.metrics.MetricSet;

/**
 * MBean for a {@link MetricSet}
 */
public interface MetricSetJmxMBean {

	String getId() throws IOException;
}
