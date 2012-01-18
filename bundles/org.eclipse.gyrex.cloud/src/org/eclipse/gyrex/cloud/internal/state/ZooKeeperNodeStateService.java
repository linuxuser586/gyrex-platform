/*******************************************************************************
 * Copyright (c) 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.internal.state;

import java.util.Collections;
import java.util.List;

import org.eclipse.gyrex.cloud.services.state.INodeState;
import org.eclipse.gyrex.cloud.services.state.query.INodeStateInfo;
import org.eclipse.gyrex.cloud.services.state.query.INodeStateQueryService;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import org.apache.commons.lang.StringUtils;

/**
 * {@link INodeStateQueryService} implementation for publishing and reading node
 * state from ZooKeeper.
 * <p>
 * Using ZooKeeper has the benefit of automatic clean-up based on EPHEMERAL
 * nodes.
 * </p>
 */
public class ZooKeeperNodeStateService extends ServiceTracker<INodeState, INodeState> implements INodeStateQueryService {

	private final ZooKeeperNodeStatePublisher statePublisher;

	/**
	 * Creates a new instance.
	 * 
	 * @param context
	 */
	public ZooKeeperNodeStateService(final BundleContext context, final String myNodeId) {
		super(context, INodeState.class, null);
		statePublisher = new ZooKeeperNodeStatePublisher(myNodeId);
	}

	@Override
	public INodeState addingService(final ServiceReference<INodeState> reference) {
		// get service
		final INodeState service = super.getService();
		if (null == service) {
			return service;
		}

		// publish if pid is available
		final String pid = (String) reference.getProperty(Constants.SERVICE_PID);
		if (StringUtils.isNotBlank(pid)) {
			statePublisher.publish(pid, reference);
		}

		return service;
	}

	/* (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTracker#close()
	 */
	@Override
	public void close() {
		try {
			// close
			super.close();
		} finally {
			// shutdown publisher
			statePublisher.shutdown();
		}
	}

	@Override
	public List<INodeStateInfo> findByNodeId(final String nodeId) {
		return Collections.unmodifiableList(statePublisher.findByNodeId(nodeId));
	}

	@Override
	public List<INodeStateInfo> findByServicePid(final String servicePid) {
		return Collections.unmodifiableList(statePublisher.findByServicePid(servicePid));
	}

	@Override
	public void modifiedService(final ServiceReference<INodeState> reference, final INodeState service) {
		// just publish if pid is available
		// TODO: support PID changes
		final String pid = (String) reference.getProperty(Constants.SERVICE_PID);
		if (StringUtils.isNotBlank(pid)) {
			statePublisher.publish(pid, reference);
		}
	}

	@Override
	public void removedService(final ServiceReference<INodeState> reference, final INodeState service) {
		// unget service
		super.removedService(reference, service);

		// remove any published state
		final String pid = (String) reference.getProperty(Constants.SERVICE_PID);
		if (StringUtils.isNotBlank(pid)) {
			statePublisher.remove(pid);
		}
	}

}
