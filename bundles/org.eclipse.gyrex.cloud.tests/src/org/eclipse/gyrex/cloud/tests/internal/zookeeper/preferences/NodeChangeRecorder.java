/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.cloud.tests.internal.zookeeper.preferences;

import static junit.framework.Assert.assertNull;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;

/**
 *
 */
public final class NodeChangeRecorder implements INodeChangeListener {

	private final BlockingDeque<NodeChangeEvent> added = new LinkedBlockingDeque<NodeChangeEvent>();
	private final BlockingDeque<NodeChangeEvent> removed = new LinkedBlockingDeque<NodeChangeEvent>();

	@Override
	public void added(final NodeChangeEvent event) {
		added.add(event);
	}

	public void assertEmpty() {
		assertEmpty("unexpected node event");
	}

	public void assertEmpty(final String message) {
		assertNull(message + " ADDED", peekAdded());
		assertNull(message + " REMOVED", peekRemoved());
	}

	public NodeChangeEvent peekAdded() {
		return added.peek();
	}

	public NodeChangeEvent peekRemoved() {
		return removed.peek();
	}

	public NodeChangeEvent pollAdded() {
		return added.poll();
	}

	public NodeChangeEvent pollAdded(final int timeout) throws InterruptedException {
		return added.poll(timeout, TimeUnit.MILLISECONDS);
	}

	public NodeChangeEvent pollRemoved() {
		return removed.poll();
	}

	public NodeChangeEvent pollRemoved(final int timeout) throws InterruptedException {
		return removed.poll(timeout, TimeUnit.MILLISECONDS);
	}

	@Override
	public void removed(final NodeChangeEvent event) {
		removed.add(event);
	}

	public void reset() {
		added.clear();
		removed.clear();
	}
}