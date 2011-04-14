/**
 * Copyright (c) 2010, 2011 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.frameworklogadapter.internal;

import java.io.IOException;
import java.net.URLConnection;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.equinox.log.ExtendedLogReaderService;

import org.eclipse.osgi.baseadaptor.BaseAdaptor;
import org.eclipse.osgi.baseadaptor.hooks.AdaptorHook;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.util.NLS;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * The adapter hook for the Gyrex extended {@link FrameworkLog}.
 */
public class FrameworkLogAdapterHook implements AdaptorHook {

	public static final String PROP_BUFFER_SIZE = "gyrex.log.forwarder.buffer.size";
	public static final String PROP_LOG_ENABLED = "gyrex.log.forwarder.enabled";

	private static final AtomicReference<FrameworkLogAdapterHook> instanceRef = new AtomicReference<FrameworkLogAdapterHook>();

	/**
	 * Returns the shared instance.
	 * 
	 * @return the shared instance
	 */
	public static FrameworkLogAdapterHook getInstance() {
		FrameworkLogAdapterHook frameworkLogAdapterHook = instanceRef.get();
		while (null == frameworkLogAdapterHook) {
			instanceRef.compareAndSet(null, new FrameworkLogAdapterHook());
			frameworkLogAdapterHook = instanceRef.get();
		}
		return frameworkLogAdapterHook;
	}

	private GyrexSlf4jForwarder logForwarder;
	private BundleContext context;

	private final SortedSet<ServiceReference<?>> loggers = new ConcurrentSkipListSet<ServiceReference<?>>();
	private final ServiceListener slf4jLoggerListener = new ServiceListener() {
		@Override
		public void serviceChanged(final ServiceEvent event) {
			switch (event.getType()) {
				case ServiceEvent.REGISTERED:
					registerLogger(event.getServiceReference());
					break;

				case ServiceEvent.UNREGISTERING:
					unregisterLogger(event.getServiceReference());
					break;
			}
		}
	};

	private final ConcurrentMap<ServiceReference<?>, ExtendedLogReaderService> readerServices = new ConcurrentHashMap<ServiceReference<?>, ExtendedLogReaderService>();
	private final ServiceListener logReaderServiceListener = new ServiceListener() {
		@Override
		public void serviceChanged(final ServiceEvent event) {
			switch (event.getType()) {
				case ServiceEvent.REGISTERED:
					registerWithLogReaderService(event.getServiceReference());
					break;

				case ServiceEvent.UNREGISTERING:
					unregisterFromLogReaderService(event.getServiceReference());
					break;
			}
		}
	};

	/**
	 * Hidden constructor.
	 */
	private FrameworkLogAdapterHook() {
		// empty
	}

	@Override
	public void addProperties(final Properties properties) {
		// empty
	}

	@Override
	public FrameworkLog createFrameworkLog() {
		// we rely on 'eclipse.log.enabled=false' to disable the default Eclipse log
		return null;
	}

	@Override
	public void frameworkStart(final BundleContext context) throws BundleException {
		// allow to disable via system property
		final String enabled = System.getProperty(PROP_LOG_ENABLED, "true");
		if (!"true".equals(enabled)) {
			return;
		}

		this.context = context;

		if (null == logForwarder) {
			logForwarder = new GyrexSlf4jForwarder(Integer.getInteger(PROP_BUFFER_SIZE, 0));
		}

		try {
			// watch for SLF4J logger
			// note, we do not import any SLF4J packages because the dependency is optional
			context.addServiceListener(slf4jLoggerListener, String.format("(& (objectClass=org.slf4j.Logger) (service.pid=org.slf4j.Logger-%s) )", GyrexSlf4jForwarder.EQUINOX_LOGGER_NAME));

			// register forwarder with log reader service
			context.addServiceListener(logReaderServiceListener, String.format("(& (objectClass=%s) )", ExtendedLogReaderService.class.getName()));

			// capture all existing services
			final Collection<ServiceReference<ExtendedLogReaderService>> serviceReferences = context.getServiceReferences(ExtendedLogReaderService.class, null);
			for (final ServiceReference<ExtendedLogReaderService> serviceReference : serviceReferences) {
				registerWithLogReaderService(serviceReference);
			}
		} catch (final InvalidSyntaxException e) {
			// should not happen, we hardcoded the filter
			System.err.println("[Eclipse Gyrex] Please inform the developers. There is an implementation error: " + e);
		}
	}

	@Override
	public void frameworkStop(final BundleContext context) throws BundleException {
		if (logForwarder == null) {
			return;
		}

		context.removeServiceListener(slf4jLoggerListener);
		context.removeServiceListener(logReaderServiceListener);

		// remove from all existing log readers

		try {
			final Collection<ServiceReference<ExtendedLogReaderService>> serviceReferences = context.getServiceReferences(ExtendedLogReaderService.class, null);
			for (final ServiceReference<ExtendedLogReaderService> serviceReference : serviceReferences) {
				unregisterFromLogReaderService(serviceReference);
			}
		} catch (final InvalidSyntaxException e) {
			// should not happen, we hardcoded the filter
			System.err.println("[Eclipse Gyrex] Please inform the developers. There is an implementation error: " + e);
		}

		logForwarder.close();
		logForwarder = null;

		this.context = null;
	}

	@Override
	public void frameworkStopping(final BundleContext context) {
		// empty
	}

	@Override
	public void handleRuntimeError(final Throwable error) {
		// empty
	}

	@Override
	public void initialize(final BaseAdaptor adaptor) {
		// empty
	}

	@Override
	public URLConnection mapLocationToURLConnection(final String location) throws IOException {
		// empty
		return null;
	}

	void registerLogger(final ServiceReference serviceReference) {
		loggers.add(serviceReference);
		updateLogger();
	}

	void registerWithLogReaderService(final ServiceReference<?> serviceReference) {
		if (null == logForwarder) {
			return;
		}

		// get service
		final ExtendedLogReaderService service = (ExtendedLogReaderService) context.getService(serviceReference);

		// register listener
		if (null == readerServices.putIfAbsent(serviceReference, service)) {
			service.addLogListener(logForwarder, logForwarder);
		}
	}

	void unregisterFromLogReaderService(final ServiceReference<?> serviceReference) {
		// unregister listener
		final ExtendedLogReaderService removed = readerServices.remove(serviceReference);
		if (null != removed) {
			removed.removeLogListener(logForwarder);
		}

		// unget service
		context.ungetService(serviceReference);
	}

	void unregisterLogger(final ServiceReference serviceReference) {
		loggers.remove(serviceReference);
		updateLogger();
	}

	private void updateLogger() {
		ServiceReference<?> highestLoggerReference = null;
		try {
			// the highest logger wins
			highestLoggerReference = loggers.last();
			logForwarder.setSLF4JLogger(context.getService(highestLoggerReference));
		} catch (final NoSuchElementException e) {
			// no logger available
			logForwarder.setSLF4JLogger(null);
		} catch (final Throwable e) {
			System.err.println(NLS.bind("[Eclipse Gyrex] Failed to get SLF4J Logger. {0}", e));
			// don't update logger, assume current one is still good
		} finally {
			// we unget the service here so that it can be released when necessary
			if (null != highestLoggerReference) {
				context.ungetService(highestLoggerReference);
			}
		}
	}

}
