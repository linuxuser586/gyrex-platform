/*******************************************************************************
 * Copyright (c) 2013 <enter-company-name-here> and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.monitoring.profiling;

/**
 * A manager for accessing {@link Transaction transactions}.
 */
public class Profiler {

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
