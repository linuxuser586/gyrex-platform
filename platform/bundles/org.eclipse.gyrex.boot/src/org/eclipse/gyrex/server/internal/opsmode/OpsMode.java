/*******************************************************************************
 * Copyright (c) 2010 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.server.internal.opsmode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.gyrex.boot.internal.app.AppActivator;
import org.eclipse.gyrex.server.Platform;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ops mode util.
 */
public class OpsMode {

	/**
	 * Returns the instance node id file.
	 * 
	 * @return the instance node id file
	 */
	private static File getInstanceStateFile() {
		return Platform.getStateLocation(AppActivator.getInstance().getBundle()).append(STATE_FILE_NAME).toFile();
	}

	/**
	 * property key '<code>gyrex.operation.mode</code>' for
	 * <code>config.ini</code>
	 */
	String PROPERTY_KEY_CONFIGURATION_MODE = "gyrex.operation.mode";

	private static final Logger LOG = LoggerFactory.getLogger(OpsMode.class);
	private static final String STATE_FILE_NAME = "operationMode";
	private static final String DEVELOPMENT = "development";
	private static final String PRODUCTION = "production";

	/** operation mode */
	private final AtomicReference<OperationMode> operationMode = new AtomicReference<OperationMode>();

	/**
	 * Creates a new instance.
	 */
	public OpsMode() {
		initializeConfigurationMode();
	}

	/**
	 * Returns the mode.
	 * 
	 * @return the mode (defaults to {@link OperationMode#DEVELOPMENT} if not
	 *         {@link #isSet() set})
	 */
	public OperationMode getMode() {
		final OperationMode mode = operationMode.get();
		if (null != mode) {
			return mode;
		}
		return OperationMode.DEVELOPMENT;
	}

	/**
	 * Initialize the configuration mode.
	 * 
	 * @param context
	 */
	private void initializeConfigurationMode() {
		// read instance state
		String mode = readInstanceState();

		// fallback to config.ini property
		if (null == mode) {
			mode = AppActivator.getInstance().getContext().getProperty(PROPERTY_KEY_CONFIGURATION_MODE);

			// persist state
			if (null != mode) {
				persistInstanceState(OperationMode.fromString(mode));
			}
		}

		// initialize
		if (null != mode) {
			operationMode.compareAndSet(null, OperationMode.fromString(mode));
		}
	}

	/**
	 * Indicates if the operation mode is set (explicitly initialized).
	 * 
	 * @return <code>true</code> if the operation mode is set,
	 *         <code>false</code> otherwise
	 */
	public boolean isSet() {
		return null != operationMode.get();
	}

	/**
	 * Writes the instance state
	 * 
	 * @param mode
	 */
	private void persistInstanceState(final OperationMode mode) {
		if (null == mode) {
			return;
		}
		final File instanceState = getInstanceStateFile();
		if ((null == instanceState) || instanceState.exists()) {
			// never overwrite existing file
			return;
		}

		// make sure the folders exists
		instanceState.getParentFile().mkdirs();

		FileOutputStream stream = null;
		try {
			stream = FileUtils.openOutputStream(instanceState);
			switch (mode) {
				case PRODUCTION:
					stream.write('p');
					break;

				case DEVELOPMENT:
				default:
					stream.write('d');
					break;
			}
		} catch (final IOException e) {
			LOG.warn("Error saving operation mode. {}", e.getMessage());
		} finally {
			IOUtils.closeQuietly(stream);
		}

	}

	/**
	 * Reads the persistent state from the instance.
	 * 
	 * @return the persistent state
	 */
	private String readInstanceState() {
		final File instanceState = getInstanceStateFile();
		if ((null == instanceState) || !instanceState.canRead()) {
			return null;
		}

		FileInputStream stream = null;
		try {
			stream = new FileInputStream(instanceState);
			final int data = stream.read();
			if (data == 'p') {
				return PRODUCTION;
			} else {
				return DEVELOPMENT;
			}
		} catch (final IOException e) {
			// ignore;
			return null;
		} finally {
			if (null != stream) {
				try {
					stream.close();
				} catch (final IOException e) {
					// ignore
				}
			}
		}
	}

	public void setMode(final OperationMode mode) {
		if (mode != null) {
			persistInstanceState(mode);
		}
	}
}
