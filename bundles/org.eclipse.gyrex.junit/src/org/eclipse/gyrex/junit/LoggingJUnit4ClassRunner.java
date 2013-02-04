/*******************************************************************************
 * Copyright (c) 2011, 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.junit;

import java.lang.reflect.Method;

import org.apache.commons.lang.exception.ExceptionUtils;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link BlockJUnit4ClassRunner} that logs a message when executing tests.
 */
public class LoggingJUnit4ClassRunner extends BlockJUnit4ClassRunner {

	static class LoggingStatemant extends Statement {

		private final Method method;
		private final Statement delegate;

		public LoggingStatemant(final Method method, final Statement delegate) {
			this.method = method;
			this.delegate = delegate;
		}

		@Override
		public void evaluate() throws Throwable {
			LOG.info("[JUnit] STARTING: {}#{}", method.getDeclaringClass().getSimpleName(), method.getName());
			try {
				delegate.evaluate();
			} catch (final Throwable t) {
				LOG.info("[JUnit] FAILED: {}#{} ({})", new Object[] { method.getDeclaringClass().getSimpleName(), method.getName(), ExceptionUtils.getRootCauseMessage(t), t });
				throw t;
			}
			LOG.info("[JUnit] FINISHED: {}#{}.", method.getDeclaringClass().getSimpleName(), method.getName());
		}
	}

	static final Logger LOG = LoggerFactory.getLogger(LoggingJUnit4ClassRunner.class);

	/**
	 * Creates a new instance.
	 * 
	 * @param klass
	 * @throws InitializationError
	 */
	public LoggingJUnit4ClassRunner(final Class<?> klass) throws InitializationError {
		super(klass);
	}

	@Override
	protected Statement methodInvoker(final FrameworkMethod method, final Object test) {
		return new LoggingStatemant(method.getMethod(), super.methodInvoker(method, test));
	}
}
