/**
 * Copyright (c) 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.log.frameworklogadapter.internal;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.adaptor.EclipseStarter;
import org.eclipse.osgi.baseadaptor.BaseAdaptor;
import org.eclipse.osgi.baseadaptor.hooks.AdaptorHook;
import org.eclipse.osgi.framework.internal.core.FrameworkProperties;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * The adapter hook for the Gyrex extended {@link FrameworkLog}.
 */
public class FrameworkLogAdapterHook implements AdaptorHook {

	/** GET_LOGGER */
	private static final String GET_LOGGER_METHOD_NAME = "getLogger";
	/** PARAMETER_TYPES */
	private static final Class[] GET_LOGGER_METHOD_ARGUMENTS = new Class[] { String.class };
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

	private BaseAdaptor baseAdaptor;
	private GyrexFrameworkLog log;
	private ServiceRegistration logServiceRegistration;
	private BundleContext context;

	private final SortedSet<ServiceReference> loggerFactories = new ConcurrentSkipListSet<ServiceReference>();
	private final ServiceListener slf4jBridge = new ServiceListener() {
		@Override
		public void serviceChanged(final ServiceEvent event) {
			switch (event.getType()) {
				case ServiceEvent.REGISTERED:
					registerLoggerFactory(event.getServiceReference());
					break;

				case ServiceEvent.UNREGISTERING:
					unregisterLoggerFactory(event.getServiceReference());
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
		if (null == log) {
			log = new GyrexFrameworkLog();
		}
		if ("true".equals(FrameworkProperties.getProperty(EclipseStarter.PROP_CONSOLE_LOG))) {
			log.setConsoleLog(true);
		}
		return log;
	}

	@Override
	public void frameworkStart(final BundleContext context) throws BundleException {
		this.context = context;

		final FrameworkLog frameworkLog = baseAdaptor.getFrameworkLog();
		if (frameworkLog != log) {
			System.err.println(NLS.bind("[Eclipse.org - Gyrex] Failed to install the GyrexFrameworkLog. FrameworkLog is of type \"{0}\". It seems that another framework hook already created the FrameworkLog. Please check the hook configuration.", frameworkLog.getClass().getName()));
			return;
		}

		final Hashtable<String, Object> properties = new Hashtable<String, Object>(3);
		properties.put(org.osgi.framework.Constants.SERVICE_VENDOR, "Eclipse.org - Gyrex");//$NON-NLS-1$
		properties.put(org.osgi.framework.Constants.SERVICE_RANKING, new Integer(Integer.MAX_VALUE));
		properties.put(org.osgi.framework.Constants.SERVICE_PID, "org.eclipse.gyrex.log.frameworklogadapter"); //$NON-NLS-1$
		logServiceRegistration = context.registerService(FrameworkLog.class.getName(), frameworkLog, null);

		try {
			// note, we do not import any SLF4J packages because the dependency is optional
			context.addServiceListener(slf4jBridge, "(objectClass=org.slf4j.LoggerFactory)");
		} catch (final InvalidSyntaxException e) {
			// should not happen, we hardcoded the filter
			System.err.println("[Eclipse.org - Gyrex] Please inform the developers. There is an implementation error: " + e);
		}
	}

	@Override
	public void frameworkStop(final BundleContext context) throws BundleException {
		context.removeServiceListener(slf4jBridge);
		if (null != logServiceRegistration) {
			logServiceRegistration.unregister();
			logServiceRegistration = null;
		}
		if (null != log) {
			log.close();
			log = null;
		}
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
		baseAdaptor = adaptor;
	}

	@Override
	public URLConnection mapLocationToURLConnection(final String location) throws IOException {
		// empty
		return null;
	}

	void registerLoggerFactory(final ServiceReference serviceReference) {
		loggerFactories.add(serviceReference);
		updateLogger();
	}

	void unregisterLoggerFactory(final ServiceReference serviceReference) {
		loggerFactories.remove(serviceReference);
		updateLogger();
	}

	private void updateLogger() {
		ServiceReference highestLoggerReference = null;
		try {
			// the highest logger wins
			highestLoggerReference = loggerFactories.last();
			final Object loggerFactory = context.getService(highestLoggerReference);
			final Method method = loggerFactory.getClass().getMethod(GET_LOGGER_METHOD_NAME, GET_LOGGER_METHOD_ARGUMENTS);
			final Object logger = method.invoke(loggerFactory, FrameworkLog.class.getName());
			log.setSLF4JLogger(logger);
		} catch (final NoSuchElementException e) {
			// no logger available
			log.setSLF4JLogger(null);
		} catch (final Throwable e) {
			System.err.println(NLS.bind("[Eclipse.org - Gyrex] Failed to get SLF4J Logger. {0}", e));
			// don't update logger, assume current one is still good
		} finally {
			if (null != highestLoggerReference) {
				context.ungetService(highestLoggerReference);
			}
		}
	}

}
