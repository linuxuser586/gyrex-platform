/*******************************************************************************
 * Copyright (c) 2012 AGETO and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.junit;

import org.eclipse.gyrex.junit.internal.GyrexStarter;
import org.eclipse.gyrex.junit.internal.JUnitActivator;

import org.junit.ClassRule;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link TestRule} which bootstraps a Gyrex server for running tests and take
 * it down afterwards.
 * <p>
 * To avoid to frequent restarts, this rule is best used in test suites
 * combining several tests together. It is also recommended to use the
 * {@link ClassRule} annotation.
 * </p>
 * <p>
 * This rule can be nested. However, only the moste outer one, which started a
 * server, will ever attempt to shutdown it.
 * </p>
 */
public class GyrexServerResource extends ExternalResource {

	private static final Logger LOG = LoggerFactory.getLogger(GyrexServerResource.class);

	private boolean hasBeenStarted;

	@Override
	protected void after() {
		if (hasBeenStarted) {
			getStarter().requestShutdown();
		}
	}

	@Override
	protected void before() throws Throwable {
		hasBeenStarted = getStarter().ensureStartedAndOnline();
		if (!hasBeenStarted) {
			LOG.debug("Gyrex server already started by outer rule!");
		}
	}

	private GyrexStarter getStarter() {
		return JUnitActivator.getInstance().getGyrexStarter();
	}

}
