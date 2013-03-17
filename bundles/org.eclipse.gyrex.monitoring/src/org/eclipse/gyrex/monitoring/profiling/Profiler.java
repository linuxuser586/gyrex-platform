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
package org.eclipse.gyrex.monitoring.profiling;

/**
 * A manager for accessing {@link Transaction transactions}.
 * <p>
 * Warning: This is part of a new API that has not been finalized yet. Please
 * get in touch with the Gyrex developments if you intend to use it and this
 * warning is still present.
 * </p>
 */
public final class Profiler {

	private static final ThreadLocal<Transaction> currentThreadTransaction = new ThreadLocal<Transaction>();

	/**
	 * Returns the transaction associated with the current thread.
	 * 
	 * @return the transaction for the current thread (maybe <code>null</code>
	 *         if none is available).
	 */
	public static Transaction getTransaction() {
		return currentThreadTransaction.get();
	}

	/**
	 * Sets the transaction for the current thread.
	 * 
	 * @param transaction
	 *            the transaction to set
	 */
	public static void setTransaction(final Transaction transaction) {
		currentThreadTransaction.set(transaction);
	}

	private Profiler() {
		// empty;
	}

}
