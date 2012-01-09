/*******************************************************************************
 * Copyright (c) 2008, 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.model.common.internal;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;

import org.osgi.framework.BundleContext;

/**
 * The core model plug-in.
 */
public class ModelActivator extends BaseBundleActivator {

	/** SYMBOLIC_NAME */
	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.model.common";

	/** the shared instance */
	private static final AtomicReference<ModelActivator> sharedInstance = new AtomicReference<ModelActivator>();

	/**
	 * Returns the shared instance.
	 * <p>
	 * A <code>{@link IllegalStateException}</code> will be thrown if the bundle
	 * has not been started.
	 * </p>
	 * 
	 * @return the shared instance
	 * @throws IllegalStateException
	 *             if the bundle has not been started
	 */
	public static ModelActivator getInstance() {
		final ModelActivator activator = sharedInstance.get();
		if (null == activator) {
			throw new IllegalStateException(NLS.bind("Bundle {0} has not been started.", SYMBOLIC_NAME));
		}

		return activator;
	}

	static public void printChildren(final IStatus status, final PrintStream output) {
		final IStatus[] children = status.getChildren();
		if ((children == null) || (children.length == 0)) {
			return;
		}
		for (int i = 0; i < children.length; i++) {
			output.println("Contains: " + children[i].getMessage()); //$NON-NLS-1$
			output.flush(); // call to synchronize output
			final Throwable exception = children[i].getException();
			if (exception != null) {
				exception.printStackTrace(output);
			}
			printChildren(children[i], output);
		}
	}

	static public void printChildren(final IStatus status, final PrintWriter output) {
		final IStatus[] children = status.getChildren();
		if ((children == null) || (children.length == 0)) {
			return;
		}
		for (int i = 0; i < children.length; i++) {
			output.println("Contains: " + children[i].getMessage()); //$NON-NLS-1$
			output.flush(); // call to synchronize output
			final Throwable exception = children[i].getException();
			if (exception != null) {
				exception.printStackTrace(output);
			}
			printChildren(children[i], output);
		}
	}

	/**
	 * Creates a new instance.
	 * <p>
	 * Note, this is called by the OSGi platform. <b>Clients should never call
	 * this method.</b>
	 * </p>
	 */
	public ModelActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		sharedInstance.set(this);
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		sharedInstance.set(null);
	}

}
