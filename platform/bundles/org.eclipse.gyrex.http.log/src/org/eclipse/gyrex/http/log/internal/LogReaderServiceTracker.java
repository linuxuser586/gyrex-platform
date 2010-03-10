/**
 * Copyright (c) 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.log.http.internal;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.eclipse.gyrex.log.http.internal.wildfire.FirePHPLogger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

class LogReaderServiceTracker extends ServiceTracker {

	private final AtomicInteger firePhpRefCount = new AtomicInteger();
	private FirePHPLogger firePHPLogger;

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 */
	public LogReaderServiceTracker(final BundleContext context) {
		super(context, ExtendedLogReaderService.class.getName(), null);
	}

	@Override
	public Object addingService(final ServiceReference reference) {
		// get service
		final ExtendedLogReaderService logReaderService = (ExtendedLogReaderService) super.addingService(reference);
		if (null == logReaderService) {
			return logReaderService;
		}

		if (null == firePHPLogger) {
			firePHPLogger = new FirePHPLogger();
		}
		firePhpRefCount.incrementAndGet();
		logReaderService.addLogListener(firePHPLogger);

		return logReaderService;
	}

	@Override
	public void removedService(final ServiceReference reference, final Object service) {
		final ExtendedLogReaderService logReaderService = (ExtendedLogReaderService) service;

		logReaderService.removeLogListener(firePHPLogger);
		if (firePhpRefCount.decrementAndGet() == 0) {
			firePHPLogger = null;
		}

		// unget service
		super.removedService(reference, service);
	}
}
