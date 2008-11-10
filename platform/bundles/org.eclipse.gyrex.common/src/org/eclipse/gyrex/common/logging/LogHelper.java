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
package org.eclipse.cloudfree.common.logging;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;


import org.eclipse.cloudfree.common.context.ContextUtil;
import org.eclipse.cloudfree.common.context.IContext;
import org.eclipse.cloudfree.common.internal.fixme.RuntimeLogAccess;
import org.eclipse.cloudfree.common.runtime.BaseBundleActivator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The bundle specific logger.
 * <p>
 * The CloudFree Platform takes logging seriously. It strictly separates logging
 * from debugging. Typically, logging may not be limited to technical message
 * logging about the system which usually targets developers or system
 * administrators. Logging is also suitable for logging application logic
 * specific messages targeted at a difference audience (eg. application users).
 * Therefore, the CloudFree platform comes with a different approach to logging.
 * </p>
 * <p>
 * Under the covers, the CloudFree Platform provides integrations into different
 * logging implementations.
 * </p>
 * <p>
 * This class may be instantiated directly by clients. However, the use through
 * {@link BaseBundleActivator} is encouraged.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
public final class BundleLog {

	/** default tag if no tags are provided */
	protected static final LogTag UNCLASSIFIED = new LogTag() {

		private static final String STRING_UNCLASSIFIED = "unclassified";

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (null == obj) {
				return false;
			}
			if (!LogTag.class.isAssignableFrom(obj.getClass())) {
				return false;
			}
			return STRING_UNCLASSIFIED.equals(obj.toString());
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return 17 * STRING_UNCLASSIFIED.hashCode();
		}

		/**
		 * Returns <code>"unclassified"</code>.
		 * 
		 * @return <code>"unclassified"</code>
		 */
		@Override
		public String toString() {
			return STRING_UNCLASSIFIED;
		}

	};

	/** the log service tracker */
	private final AtomicReference<ServiceTracker> logServiceTracker = new AtomicReference<ServiceTracker>();

	/** the plug-in id */
	private final String symbolicName;

	/**
	 * Creates a new instance.
	 * 
	 * @param symbolicName
	 *            the owner's bundle symbolic name.
	 */
	public BundleLog(final String symbolicName) {
		this.symbolicName = symbolicName;
	}

	/**
	 * Configures the bundle logger to the specified bundle context
	 * <p>
	 * Note, this method is typically called by the framework during bundle
	 * activation. Clients must not call it directly.
	 * </p>
	 * 
	 * @param context
	 *            the bundle context
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public void configure(final BundleContext context) {
		if (null == context) {
			throw new IllegalArgumentException("context must not be null");
		}

		if (!getSymbolicName().equals(context.getBundle().getSymbolicName())) {
			throw new IllegalArgumentException(NLS.bind("context \"{0}\" does not match declared bundle symbolic name \"{1}\"", context.getBundle().getSymbolicName(), getSymbolicName()));
		}

		// de-configure if necessary
		deconfigure(context);

		// set and service tracker
		if (logServiceTracker.compareAndSet(null, new ServiceTracker(context, "org.osgi.service.log.LogService", null))) {
			logServiceTracker.get().open();
		}
	}

	/**
	 * De-configures the bundle logger from any bundle specific context logging.
	 * <p>
	 * Note, this method is typically called by the framework during bundle
	 * shutdown. Clients must not call it directly.
	 * </p>
	 * 
	 * @param context
	 *            the bundle context
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public synchronized void deconfigure(final BundleContext context) {
		if (null == context) {
			throw new IllegalArgumentException("context must not be null");
		}

		// unset and close service tracker
		final ServiceTracker serviceTracker = logServiceTracker.getAndSet(null);
		if (null != serviceTracker) {
			serviceTracker.close();
		}
	}

	private LogImportance findLogImportanceTag(final Iterable<LogTag> allTags) {
		for (final LogTag logTag : allTags) {
			if (logTag.getClass().equals(LogImportance.class)) {
				return (LogImportance) logTag; // found
			}
		}
		return null; // not found
	}

	private int findSeverity(final LogTag[] tags) {
		final LogImportance logImportance = findLogImportanceTag(sanitizeTags(tags, null));
		if (null == logImportance) {
			return IStatus.INFO;
		}
		switch (logImportance) {
			case BLOCKER:
			case CRITICAL:
			case ERROR:
				return IStatus.ERROR;

			case WARNING:
				return IStatus.WARNING;

			case INFO:
			default:
				return IStatus.INFO;
		}
	}

	private IPath getContextPath(final IContext context) {
		if (null == context) {
			return Path.EMPTY;
		}
		return context.getContextPath();
	}

	/**
	 * Returns the matching {@link LogImportance} for a status object.
	 * 
	 * @param status
	 *            the status
	 * @return the log importance
	 */
	private LogImportance getImportance(final IStatus status) {
		switch (status.getSeverity()) {
			case IStatus.ERROR:
				return LogImportance.ERROR;
			case IStatus.WARNING:
				return LogImportance.WARNING;

			case IStatus.INFO:
			case IStatus.OK:
			case IStatus.CANCEL:
			default:
				return (LogImportance.INFO);
		}
	}

	/**
	 * Returns the plug-in id.
	 * 
	 * @return the plug-in id
	 */
	protected String getSymbolicName() {
		return symbolicName;
	}

	private void internalLog(final IStatus status, final IContext context, final Set<LogTag> allTags) {
		// TODO: implement logging
		final MultiStatus multiStatus = new MultiStatus(status.getPlugin(), status.getCode(), status.getMessage(), status.getException());
		multiStatus.add(new Status(status.getSeverity(), status.getPlugin(), "Context: " + getContextPath(context)));
		RuntimeLogAccess.log(multiStatus);
	}

	/**
	 * Logs a status using the specified context and tags.
	 * <p>
	 * For convenience, the status severity will be mapped automatically to one
	 * of the {@link LogImportance} tags if possible.
	 * </p>
	 * 
	 * @param status
	 *            the status to log (may not be <code>null</code>)
	 * @param context
	 *            the context a status is associated with (if <code>null</code>
	 *            a default log context is used)
	 * @param tags
	 *            the log tags (if <code>null</code> a set of default tags are
	 *            used)
	 */
	public void log(final IStatus status, final Object context, final LogTag... tags) {
		if (null == status) {
			throw new IllegalArgumentException("status must not be null");
		}

		internalLog(status, ContextUtil.getContext(context), sanitizeTags(tags, status));
	}

	/**
	 * Logs a message using the specified context and tags.
	 * <p>
	 * Note, localization support for log message is currently not provided at
	 * log time. The client is responsible for providing the localized text
	 * based on the log context and target audience.
	 * </p>
	 * 
	 * @param message
	 *            the message to log (may not be <code>null</code>)
	 * @param context
	 *            the context a status is associated with (if <code>null</code>
	 *            a default log context is used)
	 * @param tags
	 *            the log tags (if <code>null</code> a set of default tags are
	 *            used)
	 */
	public void log(final String message, final Object context, final LogTag... tags) {
		final IStatus status = new Status(findSeverity(tags), getSymbolicName(), message);
		internalLog(status, ContextUtil.getContext(context), sanitizeTags(tags, status));
	}

	/**
	 * Logs a message using the specified context, tags and cause.
	 * <p>
	 * Note, localization support for log message is currently not provided at
	 * log time. The client is responsible for providing the localized text
	 * based on the log context and target audience.
	 * </p>
	 * 
	 * @param message
	 *            the message to log (may not be <code>null</code>)
	 * @param cause
	 *            the message cause
	 * @param context
	 *            the context a status is associated with (if <code>null</code>
	 *            a default log context is used)
	 * @param tags
	 *            the log tags (if <code>null</code> a set of default tags are
	 *            used)
	 */
	public void log(final String message, final Throwable cause, final Object context, final LogTag... tags) {
		final IStatus status = new Status(findSeverity(tags), getSymbolicName(), message, cause);
		internalLog(status, ContextUtil.getContext(context), sanitizeTags(tags, status));
	}

	private Set<LogTag> sanitizeTags(final LogTag[] tags, final IStatus status) {
		// remove duplicate tags
		final Set<LogTag> allTags = new HashSet<LogTag>(null != tags ? tags.length + 1 : 2);
		if (null != tags) {
			allTags.addAll(Arrays.asList(tags));
		} else {
			allTags.add(UNCLASSIFIED);
		}

		// add LogImportance for status if none is present 
		if ((null != status) && (null == findLogImportanceTag(allTags))) {
			allTags.add(getImportance(status));
		}

		// done
		return allTags;
	}
}
