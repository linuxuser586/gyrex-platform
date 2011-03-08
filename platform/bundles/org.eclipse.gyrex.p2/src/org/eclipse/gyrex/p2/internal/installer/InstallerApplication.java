package org.eclipse.gyrex.p2.internal.installer;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

/**
 * Application which scans for new packages and installs them on the local node.
 */
public class InstallerApplication implements IApplication {

	private static final AtomicReference<PackageScanner> jobRef = new AtomicReference<PackageScanner>();
	private final AtomicReference<IApplicationContext> contextRef = new AtomicReference<IApplicationContext>();

	@Override
	public Object start(final IApplicationContext context) throws Exception {
		final PackageScanner job = new PackageScanner();
		if (!jobRef.compareAndSet(null, job)) {
			throw new IllegalStateException("installer application already started");
		}

		// schedule job
		job.schedule(PackageScanner.INITIAL_SLEEP_TIME);

		// signal running
		context.applicationRunning();

		// remember context and return async
		contextRef.set(context);
		return IApplicationContext.EXIT_ASYNC_RESULT;
	}

	@Override
	public void stop() {
		final PackageScanner job = jobRef.getAndSet(null);
		if (job == null) {
			return;
		}

		// cancel job
		job.cancel();

		// wait for finish
		try {
			job.join();
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		// set result
		final IApplicationContext context = contextRef.getAndSet(null);
		if (null != context) {
			context.setResult(EXIT_OK, this);
		}
	}

}
