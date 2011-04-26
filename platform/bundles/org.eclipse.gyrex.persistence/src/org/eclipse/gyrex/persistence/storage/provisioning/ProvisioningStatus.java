/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.persistence.storage.provisioning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * The provisioning status describes if a repository is capable of storing
 * content of a specific type.
 * <p>
 * A {@link #getSeverity()} indicates if there are provisioning operations
 * pending which need to be executed in order to store content in a repository.
 * </p>
 * <p>
 * This class is typically only instantiated by clients that contribute a
 * repository implementation to Gyrex. As such it is considered part of a
 * service provider API which may evolve faster than the general API. Please get
 * in touch with the development team through the prefered channels listed on <a
 * href="http://www.eclipse.org/gyrex">the Gyrex website</a> to stay up-to-date
 * of possible changes.
 * </p>
 */
public final class ProvisioningStatus extends Status {

	private static final IStatus[] NO_CHILDREN = new IStatus[0];

	private List<ProvisioningOperationDescriptor> pendingOperations;

	/**
	 * Creates a new provisioning status.
	 * 
	 * @param severity
	 *            the severity; one of <code>OK</code>, <code>ERROR</code>,
	 *            <code>INFO</code>, <code>WARNING</code>, or
	 *            <code>CANCEL</code>
	 * @param pluginId
	 *            the unique identifier of the relevant plug-in
	 * @param code
	 *            the plug-in-specific status code, or <code>OK</code>
	 * @param pendingOperations
	 *            the pending provisioning operations, or <code>null</code> if
	 *            none
	 * @param message
	 *            a human-readable message, localized to the current locale
	 * @param exception
	 *            a low-level exception, or <code>null</code> if not applicable
	 */
	public ProvisioningStatus(final int severity, final String pluginId, final int code, final List<ProvisioningOperationDescriptor> pendingOperations, final String message, final Throwable exception) {
		super(severity, pluginId, code, message, exception);
		setPendingOperations(pendingOperations);
	}

	@Override
	public final IStatus[] getChildren() {
		return NO_CHILDREN;
	}

	/**
	 * Returns a list of provisioning operations that must be executed in order
	 * to enable a repository to tore the specified content type.
	 * 
	 * @return an ordered, unmodifiable list of provisioning operations (maybe
	 *         empty if no further provisioning is necessary)
	 */
	public List<ProvisioningOperationDescriptor> getPendingOperations() {
		return pendingOperations;
	}

	/**
	 * Returns the severity. The severities are as follows (in descending
	 * order):
	 * <ul>
	 * <li><code>CANCEL</code> - cancelation occurred</li>
	 * <li><code>ERROR</code> - a serious error (most severe, the repository is
	 * not capable of storing the content, the pending operations must be
	 * executed first)</li>
	 * <li><code>WARNING</code> - a warning (less severe, there are pending
	 * provisioning operations but they are not required to store the content)</li>
	 * <li><code>INFO</code> - an informational ("fyi") message (least severe,
	 * the repository is capable of storing the content)</li>
	 * <li><code>OK</code> - everything is just fine</li>
	 * </ul>
	 * 
	 * @return the severity: one of <code>OK</code>, <code>ERROR</code>,
	 *         <code>INFO</code>, <code>WARNING</code>, or <code>CANCEL</code>
	 * @see #matches(int)
	 */
	@Override
	public final int getSeverity() {
		return super.getSeverity();
	}

	@Override
	public final boolean isMultiStatus() {
		return false;
	}

	/**
	 * Sets the pending provisioning operations.
	 * 
	 * @param pendingOperations
	 *            the pending provisioning operations
	 */
	protected void setPendingOperations(final List<ProvisioningOperationDescriptor> pendingOperations) {
		if (null != pendingOperations) {
			this.pendingOperations = Collections.unmodifiableList(new ArrayList<ProvisioningOperationDescriptor>(pendingOperations));
		} else {
			this.pendingOperations = Collections.emptyList();
		}
	}
}
