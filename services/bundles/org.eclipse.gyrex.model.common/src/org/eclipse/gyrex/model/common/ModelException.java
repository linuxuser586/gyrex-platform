/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.model.common;

import java.io.PrintStream;
import java.io.PrintWriter;

import org.eclipse.gyrex.model.common.internal.ModelActivator;

import org.eclipse.core.runtime.IStatus;

/**
 * Base class for Gyrex model exceptions.
 * <p>
 * Model operations will typically throw this exception in case of problems
 * occurred in the model. A status can be further queried for the problem
 * details and severity.
 * </p>
 * <p>
 * This class may be instantiated or subclassed by model contributors.
 * </p>
 */
public class ModelException extends RuntimeException {

	/** serialVersionUID */
	private static final long serialVersionUID = 1L;

	private final IStatus status;

	/**
	 * Creates a new instance using the specified status.
	 * 
	 * @param status
	 *            the status
	 */
	public ModelException(final IStatus status) {
		super(status.getMessage(), status.getException());
		this.status = status;
	}

	/**
	 * Returns the status object.
	 * 
	 * @return the status
	 */
	public IStatus getStatus() {
		return status;
	}

	/**
	 * Prints a stack trace for this throwable to the standard error stream.
	 * <p>
	 * This will also print out any throwable found in children of the
	 * {@link #getStatus() status object}.
	 * </p>
	 */
	@Override
	public void printStackTrace() {
		printStackTrace(System.err);
	}

	/**
	 * Prints a stack trace for this throwable to the specified output stream.
	 * <p>
	 * This will also print out any throwable found in children of the
	 * {@link #getStatus() status object}.
	 * </p>
	 * 
	 * @param output
	 *            the stream to write to
	 */
	@Override
	public void printStackTrace(final PrintStream output) {
		synchronized (output) {
			super.printStackTrace(output);
			ModelActivator.printChildren(status, output);
		}
	}

	/**
	 * Prints a stack trace for this throwable to the specified output writer.
	 * <p>
	 * This will also print out any throwable found in children of the
	 * {@link #getStatus() status object}.
	 * </p>
	 * 
	 * @param writer
	 *            the writer to write to
	 */
	@Override
	public void printStackTrace(final PrintWriter writer) {
		synchronized (writer) {
			super.printStackTrace(writer);
			ModelActivator.printChildren(status, writer);
		}
	}

}
