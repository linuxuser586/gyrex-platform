/**
 * Copyright (c) 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.monitoring.tests;

import static org.junit.Assert.fail;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.monitoring.metrics.ThroughputMetric;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ThroughputMetricTests {

	private ScheduledExecutorService scheduledExecutorService;

	@Before
	public void setUp() throws Exception {
		scheduledExecutorService = Executors.newScheduledThreadPool(50);
	}

	@After
	public void tearDown() throws Exception {
		scheduledExecutorService.shutdownNow();
	}

	@Test
	public void testGetRequestsStatsHitRatePerHour() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.eclipse.gyrex.monitoring.metrics.ThroughputMetric#getRequestsStatsHitRatePerMinute()}
	 * .
	 */
	@Test
	public void testGetRequestsStatsHitRatePerMinute() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for
	 * {@link org.eclipse.gyrex.monitoring.metrics.ThroughputMetric#getRequestsStatsHitRatePerSecond()}
	 * .
	 */
	@Test
	public void testGetRequestsStatsHitRatePerSecond() throws Exception {
		final ThroughputMetric metric = new ThroughputMetric("test");
		final Runnable requestSimulator = new Runnable() {
			@Override
			public void run() {
				System.out.printf("active: %d, hitrate: %d/s", metric.getRequestsActive(), metric.getRequestsStatsHitRatePerSecond());
				final long started = metric.requestStarted();
				try {
					Thread.sleep(100);
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				metric.requestFinished(1, System.currentTimeMillis() - started);
			}
		};
		scheduledExecutorService.scheduleAtFixedRate(requestSimulator, 0, 150, TimeUnit.MILLISECONDS);
		scheduledExecutorService.scheduleAtFixedRate(requestSimulator, 0, 150, TimeUnit.MILLISECONDS);
		scheduledExecutorService.scheduleAtFixedRate(requestSimulator, 0, 150, TimeUnit.MILLISECONDS);
		scheduledExecutorService.scheduleAtFixedRate(requestSimulator, 0, 150, TimeUnit.MILLISECONDS);
		scheduledExecutorService.scheduleAtFixedRate(requestSimulator, 0, 150, TimeUnit.MILLISECONDS);
		scheduledExecutorService.scheduleAtFixedRate(requestSimulator, 0, 150, TimeUnit.MILLISECONDS);
		scheduledExecutorService.scheduleAtFixedRate(requestSimulator, 0, 150, TimeUnit.MILLISECONDS);
		scheduledExecutorService.scheduleAtFixedRate(requestSimulator, 0, 150, TimeUnit.MILLISECONDS);
		Thread.sleep(30000);
		System.out.printf("active: %d, hitrate: %d/s", metric.getRequestsActive(), metric.getRequestsStatsHitRatePerSecond());
		System.out.println(metric.toString());
	}

}
