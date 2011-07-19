package org.eclipse.gyrex.persistence.mongodb.internal;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;

import org.osgi.framework.BundleContext;

public class MongoDbActivator extends BaseBundleActivator {

	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.persistence.mongodb";

	private static MongoDbActivator instance;

	/**
	 * Returns the instance.
	 * 
	 * @return the instance
	 */
	public static MongoDbActivator getInstance() {
		final MongoDbActivator activator = instance;
		if (activator == null) {
			throw new IllegalStateException("inactive");
		}
		return activator;
	}

	private MongoDbRegistry registry;

	/**
	 * Creates a new instance.
	 */
	public MongoDbActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instance = this;

		registry = new MongoDbRegistry();
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instance = null;

		registry.stop();
		registry = null;
	}

	/**
	 * Returns the registry.
	 * 
	 * @return the registry
	 */
	public MongoDbRegistry getRegistry() {
		final MongoDbRegistry mongoDbRegistry = registry;
		if (null == mongoDbRegistry) {
			throw createBundleInactiveException();
		}
		return mongoDbRegistry;
	}
}
