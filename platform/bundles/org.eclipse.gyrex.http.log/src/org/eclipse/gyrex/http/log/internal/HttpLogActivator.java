package org.eclipse.gyrex.log.http.internal;

import javax.servlet.ServletException;

import org.eclipse.equinox.http.servlet.ExtendedHttpService;

import org.eclipse.gyrex.log.http.wildfire.WidlfireLogWriterFilter;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.util.tracker.ServiceTracker;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

public class HttpLogActivator implements BundleActivator {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(HttpLogActivator.class);
	private static final WidlfireLogWriterFilter FILTER = new WidlfireLogWriterFilter();
	private WildfireAppender wa;
	private ServiceTracker tracker;

	@Override
	public void start(final BundleContext context) throws Exception {
		// get log context and root logger
		final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		final Logger rootLogger = lc.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

		LOG.info("Adding Wildfire Appender...");

		// create appender
		wa = new WildfireAppender();
		wa.setContext(lc);
		wa.setName("wildfire");
		wa.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
		wa.start();

		// add appender
		rootLogger.addAppender(wa);

		// register filter
		tracker = new ServiceTracker(context, HttpService.class.getName(), null) {
			@Override
			public Object addingService(final org.osgi.framework.ServiceReference reference) {
				final HttpService service = (HttpService) super.addingService(reference);
				if ((null != service) && (service instanceof ExtendedHttpService)) {
					try {
						((ExtendedHttpService) service).registerFilter("/", FILTER, null, null);
					} catch (final ServletException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (final NamespaceException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				return service;
			};

			@Override
			public void removedService(final org.osgi.framework.ServiceReference reference, final Object service) {
				if ((null != service) && (service instanceof ExtendedHttpService)) {
					try {
						((ExtendedHttpService) service).unregisterFilter(FILTER);
					} catch (final IllegalArgumentException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				super.removedService(reference, service);
			};
		};
		tracker.open();
	}

	@Override
	public void stop(final BundleContext context) throws Exception {
		if (null != tracker) {
			tracker.close();
		}

		// remove and stop appender
		if (null != wa) {
			final LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
			final Logger rootLogger = lc.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
			rootLogger.detachAppender(wa);
			wa.stop();
			wa = null;
		}
	}

}
