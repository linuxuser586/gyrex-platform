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
package org.eclipse.gyrex.preferences.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate;
import org.eclipse.gyrex.cloud.internal.zk.ZooKeeperGate.IConnectionMonitor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import org.osgi.service.prefs.BackingStoreException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class ZooKeeperPreferenceConnector implements IConnectionMonitor {

	private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperPreferenceConnector.class);

	private final ZooKeeperBasedPreferences rootNode;
	private final AtomicReference<Job> connectJobRef = new AtomicReference<Job>();

	/**
	 * Creates a new instance.
	 */
	ZooKeeperPreferenceConnector(final ZooKeeperBasedPreferences rootNode) {
		this.rootNode = rootNode;
	}

	@Override
	public void connected(final ZooKeeperGate gate) {
		final Job job = new Job("Activating preferences hierarchy.") {
			private static final int MAX_CONNECT_DELAY = 240000;
			private volatile int delay = 1000;

			private int nextDelay() {
				return delay = delay < MAX_CONNECT_DELAY ? delay * 2 : MAX_CONNECT_DELAY;
			}

			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				try {
					// activate
					LOG.info("Synchronizing preferences hierarchy for node {}.", rootNode.absolutePath());
					rootNode.syncTree();

					// clear job references
					connectJobRef.compareAndSet(this, null);

					// done
					return Status.OK_STATUS;
				} catch (final BackingStoreException e) {
					// get back-off delay
					final int nextDelay = nextDelay();

					// log error
					LOG.warn("Unable to activate cloud preferences. Will retry in {} seconds. {}", new Object[] { nextDelay, ExceptionUtils.getRootCauseMessage(e), e });

					// re-try later
					schedule(nextDelay);

					// indicate not successful
					return Status.CANCEL_STATUS;
				}
			}
		};
		job.setSystem(true);
		if (connectJobRef.compareAndSet(null, job)) {
			job.schedule();
		}
	}

	@Override
	public void disconnected(final ZooKeeperGate gate) {
		// cancel activation job
		final Job job = connectJobRef.getAndSet(null);
		if (job != null) {
			job.cancel();
		}

		// set dis-connected
		if (rootNode != null) {
			try {
				rootNode.disconnectTree();
			} catch (final Exception e) {
				// ignore (maybe already disconnected)
			}
		}

		LOG.info("De-activated ZooKeeper preferences.");
	}
}