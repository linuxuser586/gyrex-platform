/*******************************************************************************
 * Copyright (c) 2013 AGETO and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.jobs.spi.storage;

import org.eclipse.gyrex.context.provider.RuntimeContextObjectProvider;
import org.eclipse.gyrex.jobs.schedules.ISchedule;

/**
 * A store for persisting job configuration data ({@link ISchedule} & Co.).
 * <p>
 * Implementations of this class must be made available as
 * {@link RuntimeContextObjectProvider context objects}. The system may be
 * configured with a standard implementation at the root context level. They can
 * be overridden at any context level node using the standard Gyrex context
 * configuration capabilities. Different storage implementations within one
 * context are not supported.
 * </p>
 * <p>
 * If no alternate storage implementation is configured in a context, a default
 * job configuration storage will be used which stores configuration data in
 * cloud preferences.
 * </p>
 * <p>
 * This interface must be implemented by clients that contribute a history store
 * implementation to Gyrex. As such it is considered part of a service provider
 * API which may evolve faster than the general API. Please get in touch with
 * the development team through the prefered channels listed on <a
 * href="http://www.eclipse.org/gyrex">the Gyrex website</a> to stay up-to-date
 * of possible changes.
 * </p>
 */
public interface IJobConfigurationStorage {

}
