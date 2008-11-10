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

/**
 * Metrics for monitoring the system.
 * <p>
 * The CloudFree Platform uses metrics to provide insights into the system. 
 * Metrics are extensible can be registered to the platform to allow clients
 * to contribute their own metrics. 
 * </p>
 * <p>
 * Metrics will be made available via JMX as Open MBeans.
 * </p>
 */
package org.eclipse.cloudfree.monitoring.metrics;

