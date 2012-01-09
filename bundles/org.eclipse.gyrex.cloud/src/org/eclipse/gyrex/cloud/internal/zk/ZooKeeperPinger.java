/*******************************************************************************
 * Copyright (c) 2011 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal.zk;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.gyrex.cloud.internal.CloudActivator;
import org.eclipse.gyrex.cloud.internal.CloudDebug;
import org.eclipse.gyrex.monitoring.diagnostics.IStatusConstants;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.osgi.framework.ServiceRegistration;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple runnable to ping ZooKeeper
 */
public final class ZooKeeperPinger implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperPinger.class);
	private static final String STATUS_PID = CloudActivator.SYMBOLIC_NAME + ".status.zookeeper.connection";
	private static final Callable<IStatus> checkConnection = new Callable<IStatus>() {

		@Override
		public IStatus call() throws Exception {
			final ZooKeeperGate zk = ZooKeeperGate.get();
			final IPath pingPath = zk.createPath(IZooKeeperLayout.PATH_GYREX_ROOT.append("ping"), CreateMode.EPHEMERAL_SEQUENTIAL, DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(System.currentTimeMillis()));
			zk.deletePath(pingPath);
			return null;
		}
	};
	private ServiceRegistration serviceRegistration;

	@Override
	public void run() {
		final ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			if (CloudDebug.zooKeeperGateLifecycle) {
				LOG.debug("Checking ZooKeeper connection...");
			}

			// execute in separate thread with timeout
			executor.submit(checkConnection).get(30, TimeUnit.SECONDS);

			if (CloudDebug.zooKeeperGateLifecycle) {
				LOG.debug("ZooKeeper connection OK");
			}

			// ok
			setStatus(null);
		} catch (final Exception e) {
			LOG.error("The ZooKeeper connection is in trouble. {}", ExceptionUtils.getRootCauseMessage(e), e);
			setStatus(new Status(IStatus.ERROR, CloudActivator.SYMBOLIC_NAME, String.format("Unable to ping ZooKeeper. %s", ExceptionUtils.getRootCauseMessage(e)), e));
		} finally {
			executor.shutdownNow();
		}
	}

	private synchronized void setStatus(final IStatus status) {
		try {
			if (null != serviceRegistration) {
				serviceRegistration.unregister();
				serviceRegistration = null;
			}
			if ((null != status) && !status.isOK()) {
				serviceRegistration = CloudActivator.getInstance().getServiceHelper().registerService(IStatusConstants.SERVICE_NAME, status, "Eclipse Gyrex", "ZooKeeper Connection Status", STATUS_PID, null);
			}
		} catch (final IllegalStateException e) {
			LOG.warn("Unable to update ZooKeeper connection status. {}", e.getMessage());
		}
	}
}