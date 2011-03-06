package org.eclipse.gyrex.p2.internal;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;
import org.eclipse.gyrex.p2.packages.IPackageManager;
import org.eclipse.gyrex.p2.repositories.IRepositoryManager;

import org.osgi.framework.BundleContext;

public class P2Activator extends BaseBundleActivator {

	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.p2";
	private static volatile P2Activator instance;

	/**
	 * Returns the instance.
	 * 
	 * @return the instance
	 */
	public static P2Activator getInstance() {
		final P2Activator activator = instance;
		if (activator == null) {
			throw new IllegalArgumentException("inactive");
		}
		return activator;
	}

	private volatile PackageManager packageManager;
	private volatile RepoManager repoManager;

	/**
	 * Creates a new instance.
	 */
	public P2Activator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instance = this;
		packageManager = new PackageManager();
		repoManager = new RepoManager();
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instance = null;
		packageManager = null;
		repoManager = null;
	}

	@Override
	protected Class getDebugOptions() {
		return P2Debug.class;
	}

	public IPackageManager getPackageManager() {
		final PackageManager manager = packageManager;
		if (manager == null) {
			throw createBundleInactiveException();
		}
		return manager;
	}

	public IRepositoryManager getRepositoryManager() {
		final RepoManager manager = repoManager;
		if (manager == null) {
			throw createBundleInactiveException();
		}
		return manager;
	}

}
