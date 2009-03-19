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
package org.eclipse.gyrex.common.status;


import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.gyrex.common.runtime.BaseBundleActivator;

/**
 * A utility for working with {@link IStatus} objects.
 * <p>
 * This class may be instantiated directly by clients. However, the use through
 * {@link BaseBundleActivator} is encouraged.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public final class BundleStatusUtil {

	/** the symbolic name */
	private final String symbolicName;

	/**
	 * Creates a new instance.
	 * 
	 * @param symbolicName
	 *            the owner's bundle symbolic name.
	 * @noreference This constructor is not intended to be referenced by
	 *              clients.
	 */
	public BundleStatusUtil(final String symbolicName) {
		this.symbolicName = symbolicName;
	}

	/**
	 * Creates a new status object with severity {@link IStatus#ERROR}.
	 * <p>
	 * The status plug-in id will be set with the {@link #getSymbolicName()
	 * owner's bundle symbolic name}.
	 * </p>
	 * 
	 * @param code
	 *            the plug-in-specific status code, or <code>OK</code>
	 * @param message
	 *            a human-readable message, localized to the current locale
	 * @param exception
	 *            a low-level exception, or <code>null</code> if not applicable
	 * @return the created status object
	 */
	public IStatus createError(final int code, final String message, final Throwable exception) {
		return new Status(IStatus.ERROR, symbolicName, code, message, exception);
	}

	/**
	 * Creates a new status object.
	 * <p>
	 * The status plug-in id will be set with the {@link #getSymbolicName()
	 * owner's bundle symbolic name}.
	 * </p>
	 * 
	 * @param severity
	 *            the severity; one of <code>OK</code>, <code>ERROR</code>,
	 *            <code>INFO</code>, <code>WARNING</code>, or
	 *            <code>CANCEL</code>
	 * @param code
	 *            the plug-in-specific status code, or <code>OK</code>
	 * @param message
	 *            a human-readable message, localized to the current locale
	 * @param exception
	 *            a low-level exception, or <code>null</code> if not applicable
	 * @return the created status object
	 */
	public IStatus createStatus(final int severity, final int code, final String message, final Throwable exception) {
		return new Status(severity, symbolicName, code, message, exception);
	}

	/**
	 * Creates a new status object with severity {@link IStatus#WARNING}.
	 * <p>
	 * The status plug-in id will be set with the {@link #getSymbolicName()
	 * owner's bundle symbolic name}.
	 * </p>
	 * <p>
	 * The status exception will be <code>null</code>.
	 * </p>
	 * 
	 * @param code
	 *            the plug-in-specific status code, or <code>OK</code>
	 * @param message
	 *            a human-readable message, localized to the current locale
	 * @return the created status object
	 */
	public IStatus createWarning(final int code, final String message) {
		return new Status(IStatus.ERROR, symbolicName, code, message, null);
	}

	/**
	 * Returns the symbolicName.
	 * 
	 * @return the symbolicName
	 */
	public String getSymbolicName() {
		return symbolicName;
	}
}
