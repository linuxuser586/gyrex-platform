package org.eclipse.gyrex.logback.config.internal;

import org.eclipse.gyrex.common.runtime.BaseBundleActivator;

import org.osgi.framework.BundleContext;

public class LogbackConfigActivator extends BaseBundleActivator {

	public static final String SYMBOLIC_NAME = "org.eclipse.gyrex.logback.config";
	private static LogbackConfigActivator instance;

	/**
	 * Returns the instance.
	 * 
	 * @return the instance
	 */
	public static LogbackConfigActivator getInstance() {
		return instance;
	}

	/**
	 * Creates a new instance.
	 */
	public LogbackConfigActivator() {
		super(SYMBOLIC_NAME);
	}

	@Override
	protected void doStart(final BundleContext context) throws Exception {
		instance = this;
	}

	@Override
	protected void doStop(final BundleContext context) throws Exception {
		instance = null;
	}
}
