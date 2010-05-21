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
package org.eclipse.gyrex.common.runtime;

import java.text.MessageFormat;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.gyrex.common.debug.BundleDebugOptions;
import org.eclipse.gyrex.common.lifecycle.IShutdownParticipant;
import org.eclipse.gyrex.common.logging.BundleLogHelper;
import org.eclipse.gyrex.common.logging.LogAudience;
import org.eclipse.gyrex.common.logging.LogSource;
import org.eclipse.gyrex.common.services.BundleServiceHelper;
import org.eclipse.gyrex.common.status.BundleStatusUtil;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * The default bundle activator implementation.
 * <p>
 * A bundle activator is the entry into the OSGi world. This common base class
 * provides convenient access to helpers and implements lifecycle practises
 * followed in Gyrex.
 * </p>
 * <p>
 * This class should be subclassed by clients providing a bundle to the Gyrex.
 * However, its usage is not mandatory.
 * </p>
 * <p>
 * Clients subclassing this class must provide a parameterless public
 * constructor when specifying the class as their bundle activator in their
 * bundle manifest.
 * </p>
 * 
 * @see BundleActivator
 */
public abstract class BaseBundleActivator implements BundleActivator {

	/** the shutdown participants */
	private final ListenerList shutdownParticipants = new ListenerList(ListenerList.IDENTITY);

	/** the plug-in id */
	private final String symbolicName;

	/** the underlying bundle */
	private final AtomicReference<Bundle> bundle = new AtomicReference<Bundle>();

	/** the bundle version */
	private final AtomicReference<Version> bundleVersion = new AtomicReference<Version>();

	/** the plug-in log */
	private final AtomicReference<BundleLogHelper> log = new AtomicReference<BundleLogHelper>();

	/** the status util */
	private final AtomicReference<BundleStatusUtil> statusUtil = new AtomicReference<BundleStatusUtil>();

	/** the service helper */
	private final AtomicReference<BundleServiceHelper> serviceHelper = new AtomicReference<BundleServiceHelper>();

	/**
	 * Creates a new instance.
	 * <p>
	 * Note, due to OSGi requirements subclasses must provide a parameterless
	 * public constructor. This constructor exists to have a contract for the
	 * bundle symbolic available at construction time. The name specified here
	 * will be returned ba {@link #getSymbolicName()} and verified during
	 * {@link #start(BundleContext) bundle start}.
	 * </p>
	 * <p>
	 * Note, this constructor should only be called by subclasses when
	 * implementing the parameterless public constructor. It's invoked by the
	 * OSGi runtime.
	 * </p>
	 * 
	 * @param symbolicName
	 *            the symbolic name (also known as plug-in id)
	 */
	public BaseBundleActivator(final String symbolicName) {
		this.symbolicName = symbolicName;
	}

	/**
	 * Adds a shutdown participant. This method has no effect if the same
	 * participant (based on object identity) is already registered.
	 * 
	 * @param shutdownParticipant
	 *            the participant to add
	 */
	public final void addShutdownParticipant(final IShutdownParticipant shutdownParticipant) {
		shutdownParticipants.add(shutdownParticipant);
	}

	/**
	 * Checks if the bundle is active.
	 * 
	 * @throws IllegalStateException
	 *             if the bundle is inactive
	 */
	protected final void checkActive() throws IllegalStateException {
		if (null == bundle) {
			throw createBundleInactiveException();
		}
	}

	/**
	 * Creates an {@link IllegalStateException} which may be thrown when a
	 * bundle is no longer active.
	 * 
	 * @return an {@link IllegalStateException} to throw when a bundle is
	 *         inactive
	 */
	protected final IllegalStateException createBundleInactiveException() {
		return new IllegalStateException(MessageFormat.format("Bundle ''{0}'' is inactive.", getSymbolicName()));
	}

	/**
	 * Creates a new status using this plug-in's id and the specified message
	 * and cause.
	 * 
	 * @param message
	 * @param cause
	 * @return
	 */
	private IStatus createInternalStatus(final String message, final Throwable cause) {
		return new Status(IStatus.ERROR, getSymbolicName(), -1, message, cause);
	}

	/**
	 * Called when this bundle is started so the Framework can perform the
	 * bundle-specific activities necessary to start this bundle.
	 * <p>
	 * This method can be used to register services or to allocate any resources
	 * that this bundle needs. The default implementation does nothing. Thus,
	 * subclasses are not required to call super.
	 * </p>
	 * <p>
	 * This method must complete and return to its caller in a timely manner.
	 * It's advisable to delegate long running necessary activation work into a
	 * background job/thread.
	 * </p>
	 * 
	 * @param context
	 *            The execution context of the bundle being started.
	 * @throws java.lang.Exception
	 *             If this method throws an exception, this bundle is marked as
	 *             stopped and the Framework will remove this bundle's
	 *             listeners, unregister all services registered by this bundle,
	 *             and release all services used by this bundle.
	 */
	protected void doStart(final BundleContext context) throws Exception {
		// empty
	}

	/**
	 * Called when this bundle is stopped so the Framework can perform the
	 * bundle-specific activities necessary to stop the bundle.
	 * <p>
	 * In general, this method should undo the work that the
	 * <code>{@link #doStart(BundleContext)}</code> method started. There should
	 * be no active threads that were started by this bundle when this bundle
	 * returns. A stopped bundle must not call any Framework objects.
	 * </p>
	 * <p>
	 * This method must complete and return to its caller in a timely manner.
	 * </p>
	 * 
	 * @param context
	 *            The execution context of the bundle being stopped.
	 * @throws java.lang.Exception
	 *             If this method throws an exception, the bundle is still
	 *             marked as stopped, and the Framework will remove the bundle's
	 *             listeners, unregister all services registered by the bundle,
	 *             and release all services used by the bundle.
	 */
	protected void doStop(final BundleContext context) throws Exception {
		// empty
	}

	/**
	 * Returns the underlying OSGi bundle.
	 * 
	 * @return the underlying bundle (maybe <code>null</code> if inactive).
	 */
	public final Bundle getBundle() {
		return bundle.get();
	}

	/**
	 * Returns the version of the underlying OSGi bundle as specified in the
	 * bundle manifest header <code>{@value Constants#BUNDLE_VERSION}</code>.
	 * 
	 * @return the bundle version.
	 */
	public final Version getBundleVersion() {
		final Version bundleVersion = this.bundleVersion.get();
		if (null != bundleVersion) {
			return bundleVersion;
		}
		final Bundle bundle = this.bundle.get();
		if (null == bundle) {
			return Version.emptyVersion;
		}
		final String version = (String) bundle.getHeaders().get(Constants.BUNDLE_VERSION);
		if (null == version) {
			return Version.emptyVersion;
		}

		// remember
		this.bundleVersion.set(Version.parseVersion(version));

		// return
		return this.bundleVersion.get();
	}

	/**
	 * Returns a class with non-final public static boolean fields with debug
	 * options.
	 * <p>
	 * Subclasses may overwrite to to return their bundle specific debug options
	 * class which will be initialized during bundle activation. The default
	 * implementation returns <code>null</code>.
	 * </p>
	 * 
	 * @return the debug options
	 */
	protected Class getDebugOptions() {
		return null; // no debug options
	}

	/**
	 * Returns the plug-in specific log that should be used for all logging
	 * within the platform.
	 * 
	 * @return the bundle log
	 */
	public final BundleLogHelper getLog() {
		checkActive();
		if (null == log.get()) {
			log.compareAndSet(null, new BundleLogHelper(symbolicName));
		}
		return log.get();
	}

	/**
	 * Returns the bundle's service helper.
	 * 
	 * @return the bundle service helper
	 * @throws IllegalStateException
	 *             if the bundle is inactive
	 */
	public final BundleServiceHelper getServiceHelper() throws IllegalStateException {
		final BundleServiceHelper bundleServiceHelper = serviceHelper.get();
		if (null == bundleServiceHelper) {
			throw createBundleInactiveException();
		}
		return bundleServiceHelper;
	}

	/**
	 * Returns the statusUtil.
	 * 
	 * @return the statusUtil
	 */
	public final BundleStatusUtil getStatusUtil() {
		checkActive();
		if (null == statusUtil.get()) {
			statusUtil.set(new BundleStatusUtil(getSymbolicName()));
		}
		return statusUtil.get();
	}

	/**
	 * Returns the bundle symbolic name as specified at
	 * {@link #BaseBundleActivator(String) construction time} as plug-in id).
	 * 
	 * @return the bundle symbolic name
	 * @see Bundle#getSymbolicName()
	 * @see #BaseBundleActivator(String)
	 */
	public final String getSymbolicName() {
		return symbolicName;
	}

	/**
	 * Removes a shutdown participant. This method has no effect if the same
	 * participant (based on object identity) was not registered.
	 * 
	 * @param shutdownParticipant
	 *            the participant to remove
	 */
	public final void removeShutdownParticipant(final IShutdownParticipant shutdownParticipant) {
		shutdownParticipants.remove(shutdownParticipant);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public final void start(final BundleContext context) throws Exception {
		// check id
		if (!context.getBundle().getSymbolicName().equals(getSymbolicName())) {
			final String errorMessage = MessageFormat.format("Declared bundle symbolic name ''{0}'' does not match defined bundle symbolic name ''{1}''!", getSymbolicName(), context.getBundle().getSymbolicName());
			// let's log an error to sys-out in the hope that someone will read it
			System.err.println(errorMessage);
			throw new IllegalArgumentException(errorMessage);
		}

		// remember the bundle
		bundle.set(context.getBundle());

		// configure the bundle log
		getLog().configure(context);

		// initialize debug options
		final Class debugOptions = getDebugOptions();
		if (null != debugOptions) {
			BundleDebugOptions.initializeDebugOptions(this, debugOptions);
		}

		// initialize service helper
		serviceHelper.set(new BundleServiceHelper(context));

		// do start
		doStart(context);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public final void stop(final BundleContext context) throws Exception {
		try {
			// notify shutdown participants
			final Object[] participants = shutdownParticipants.getListeners();
			for (final Object participant : participants) {
				try {
					((IShutdownParticipant) participant).shutdown();
				} catch (final Exception e) {
					getLog().log(createInternalStatus(NLS.bind("Error while shutting down shutdown participant \"{0}\": {1}", participant, e), e), this, LogAudience.DEVELOPER, LogAudience.ADMIN, LogSource.PLATFORM);
				}
			}

			// release listeners
			shutdownParticipants.clear();
		} catch (final Exception e) {
			// TODO consider logging this but continue with shutdown
		}

		try {
			// stop the bundle
			doStop(context);
		} catch (final Exception e) {
			// TODO consider logging this but continue with shutdown
		}

		// dispose the service helper
		serviceHelper.getAndSet(null).dispose();

		// de-configure the plug-in log
		getLog().deconfigure(context);

		// forget the bundle
		bundle.set(null);

		// forget the bundle version
		bundleVersion.set(null);
	}

}
