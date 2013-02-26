/*******************************************************************************
 * Copyright (c) 2013 AGETO Service GmbH and others.
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

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.ObjectUtils;

/**
 * Wrapper around AtomicReference to detected to frequent state changes.
 */
public class AtomicReferenceWithFlappingDetection<V> {

	static class StateChange<V> {
		final V reference;
		final long timestamp;

		/**
		 * Creates a new instance.
		 */
		public StateChange(final V reference, final long timestamp) {
			this.reference = reference;
			this.timestamp = timestamp;
		}
	}

	private final int historyCapacity;
	private final Deque<StateChange<V>> lastStates = new ConcurrentLinkedDeque<>();
	private final AtomicReference<V> reference;

	public AtomicReferenceWithFlappingDetection(final int historyCapacity) {
		this.historyCapacity = Math.max(historyCapacity, 1);
		reference = new AtomicReference<>();
	}

	public final boolean compareAndSet(final V expect, final V update) {
		if (!reference.compareAndSet(expect, update))
			return false;

		recordChange(expect, update);
		return true;
	}

	public final V get() {
		return reference.get();
	}

	public final V getAndSet(final V newValue) {
		final V old = reference.getAndSet(newValue);
		recordChange(old, newValue);
		return old;
	}

	public long getLastStateChangeTimestamp() {
		if (lastStates.isEmpty())
			return 0L;
		return lastStates.getFirst().timestamp;
	}

	public boolean isFlapping(final long since, final int allowedChanges) {
		if (allowedChanges > historyCapacity)
			throw new IllegalArgumentException("number of allowed changes must be below history capacity");

		int happendChanges = 0;
		for (final StateChange<V> change : lastStates) {
			if (change.timestamp >= since) {
				if (++happendChanges > allowedChanges)
					return true;
			}
		}
		return happendChanges > allowedChanges;
	}

	private void recordChange(final V oldValue, final V newValue) {
		// only record actual changes
		if (ObjectUtils.equals(oldValue, newValue))
			return;

		// insert most recent change in the beginning
		lastStates.addFirst(new StateChange<>(newValue, System.currentTimeMillis()));

		// shrink to capacity limit
		while (lastStates.size() > historyCapacity) {
			lastStates.removeLast();
		}
	}

	public final void set(final V newValue) {
		getAndSet(newValue);
	}
}
