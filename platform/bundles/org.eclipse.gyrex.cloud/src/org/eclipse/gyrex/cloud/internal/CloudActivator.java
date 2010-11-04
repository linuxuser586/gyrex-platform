package org.eclipse.gyrex.cloud.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.common.services.IServiceProxy;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.util.NLS;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle activator.
 */
public class CloudActivator extends BaseBundleActivator {

	static final Logger LOG = LoggerFactory.getLogger(CloudActivator.class);

	/** SYMBOLIC_NAME */
	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.cloud";

	/** shared instance (only set when active) */
	private static final AtomicReference<CloudActivator> instanceRef = new AtomicReference<CloudActivator>();

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static CloudActivator getInstance() {
		final CloudActivator instance = instanceRef.get();
		if (instance == null) {
			throw new IllegalArgumentException(NLS.bind("Bundle {0} is not active.", SYMBOLIC_NAME));
		}
		return instance;
	}

	private final AtomicReference<IServiceProxy<Location>> instanceLocationServiceRef = new AtomicReference<IServiceProxy<Location>>();
	private final AtomicReference<IServiceProxy<IPreferencesService>> preferenceServiceRef = new AtomicReference<IServiceProxy<IPreferencesService>>();

	final AtomicReference<ServiceTracker> zkServerAppTrackerRef = new AtomicReference<ServiceTracker>();

	/**
	 * Creates a new instance.
	 */
	public CloudActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instanceRef.set(this);

		// eager read server id in order to prevent activation in case of errors
		final String nodeId = new NodeInfo().getNodeId();
		LOG.info("Initializing cloud services for node id {}.", nodeId);

		// track services
		instanceLocationServiceRef.set(getServiceHelper().trackService(Location.class, context.createFilter(Location.INSTANCE_FILTER)));
		preferenceServiceRef.set(getServiceHelper().trackService(IPreferencesService.class));

		// register node with cloud
		CloudState.registerNode();

		// prepare ZooKeeper apps tracker
		// TODO should use a server role for this!
		zkServerAppTrackerRef.set(new EquinoxApplicationLauncher(context, "org.eclipse.gyrex.cloud.zookeeper.server.application"));

		// perform expensive registration asynchronously
		final Job initCloudJob = new Job("Cloud initialization.") {
			@Override
			protected IStatus run(final IProgressMonitor monitor) {
				// open tracker for starting ZooKeeper server (if necessary)
				final ServiceTracker tracker = zkServerAppTrackerRef.get();
				if (tracker != null) {
					try {
						tracker.open();
					} catch (final Exception e) {
						// log a warning but do not shutdown the platform
						LOG.error("Unable to start ZooKeeper server. {}", e.getMessage(), e);
					}
				}
				return Status.OK_STATUS;
			}
		};
		initCloudJob.setSystem(true);
		initCloudJob.setPriority(Job.SHORT);
		initCloudJob.schedule(500l);
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		// unregister node with cloud
		CloudState.unregisterNode();

		// stop ZooKeeper server
		final ServiceTracker tracker = zkServerAppTrackerRef.getAndSet(null);
		if (tracker != null) {
			tracker.close();
		}

		instanceRef.set(null);
		instanceLocationServiceRef.set(null);
		preferenceServiceRef.set(null);
	}

	@Override
	protected Class getDebugOptions() {
		return CloudDebug.class;
	}

	public IPreferencesService getPreferenceService() {
		final IServiceProxy<IPreferencesService> serviceProxy = preferenceServiceRef.get();
		if (null == serviceProxy) {
			throw createBundleInactiveException();
		}
		return serviceProxy.getService();
	}
}
