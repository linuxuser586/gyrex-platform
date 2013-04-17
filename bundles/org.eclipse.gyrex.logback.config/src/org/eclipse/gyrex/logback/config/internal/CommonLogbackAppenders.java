/*******************************************************************************
 * Copyright (c) 2013 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.logback.config.internal;

import org.eclipse.gyrex.logback.config.model.Appender;
import org.eclipse.gyrex.logback.config.model.ConsoleAppender;
import org.eclipse.gyrex.logback.config.model.FileAppender;
import org.eclipse.gyrex.logback.config.model.FileAppender.RotationPolicy;
import org.eclipse.gyrex.logback.config.spi.AppenderProvider;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import org.apache.commons.lang.StringUtils;

/**
 * Provides default appenders
 */
public class CommonLogbackAppenders extends AppenderProvider {

	private static final String FILE = "file";
	private static final String CONSOLE = "console";

	private static final String PATTERN = "pattern";
	private static final String ROTATION_POLICY = "rotationPolicy";
	private static final String SIFTING_MDC_PROPERTYDEFAULT_VALUE = "siftingMdcPropertydefaultValue";
	private static final String SIFTING_MDC_PROPERTY_NAME = "siftingMdcPropertyName";

	private static final String COMPRESS_ROTATED_LOGS = "compressRotatedLogs";
	private static final String MAX_FILE_SIZE = "maxFileSize";
	private static final String MAX_HISTORY = "maxHistory";
	private static final String FILE_NAME = "fileName";

	public CommonLogbackAppenders() {
		super(CONSOLE, FILE);
	}

	@Override
	public Appender createAppender(final String typeId) throws Exception {
		switch (typeId) {
			case CONSOLE:
				return new ConsoleAppender();
			case FILE:
				return new FileAppender();

			default:
				return null;
		}
	}

	@Override
	public String getName(final String typeId) {
		switch (typeId) {
			case CONSOLE:
				return "Console";
			case FILE:
				return "File";

			default:
				return null;
		}
	}

	@Override
	public Appender loadAppender(final String typeId, final Preferences node) throws Exception {
		switch (typeId) {
			case CONSOLE:
				return loadConsoleAppender(node);
			case FILE:
				return loadFileAppender(node);

			default:
				return null;
		}
	}

	private Appender loadConsoleAppender(final Preferences node) throws BackingStoreException {
		final ConsoleAppender appender = new ConsoleAppender();
		appender.setName(node.name());
		appender.setPattern(node.get(PATTERN, null));
		return appender;
	}

	private Appender loadFileAppender(final Preferences node) throws BackingStoreException {
		final FileAppender fileAppender = new FileAppender();
		fileAppender.setName(node.name());
		fileAppender.setPattern(node.get(PATTERN, null));
		fileAppender.setFileName(node.get(FILE_NAME, null));
		try {
			fileAppender.setRotationPolicy(RotationPolicy.valueOf(node.get(ROTATION_POLICY, null)));
			fileAppender.setMaxHistory(node.get(MAX_HISTORY, null));
			fileAppender.setMaxFileSize(node.get(MAX_FILE_SIZE, null));
			if (null != node.get(COMPRESS_ROTATED_LOGS, null)) {
				fileAppender.setCompressRotatedLogs(node.getBoolean(COMPRESS_ROTATED_LOGS, true));
			}
		} catch (final IllegalArgumentException e) {
			fileAppender.setRotationPolicy(null);
		}
		if (null != node.get(SIFTING_MDC_PROPERTY_NAME, null)) {
			fileAppender.setSiftingMdcPropertyName(node.get(SIFTING_MDC_PROPERTY_NAME, null));
			if (null != node.get(SIFTING_MDC_PROPERTYDEFAULT_VALUE, null)) {
				fileAppender.setSiftingMdcPropertyDefaultValue(node.get(SIFTING_MDC_PROPERTYDEFAULT_VALUE, null));
			}
		}
		return fileAppender;
	}

	private void saveConsoleAppender(final ConsoleAppender appender, final Preferences node) {
		if (null != appender.getPattern()) {
			node.put(PATTERN, appender.getPattern());
		} else {
			node.remove(PATTERN);
		}
	}

	private void saveFileAppender(final FileAppender appender, final Preferences node) {
		if (null != appender.getPattern()) {
			node.put(PATTERN, appender.getPattern());
		} else {
			node.remove(PATTERN);
		}
		if (null != appender.getFileName()) {
			node.put(FILE_NAME, appender.getFileName());
		} else {
			node.remove(FILE_NAME);
		}
		if (null != appender.getRotationPolicy()) {
			node.put(ROTATION_POLICY, appender.getRotationPolicy().name());
		} else {
			node.remove(ROTATION_POLICY);
		}
		if (null != appender.getMaxHistory()) {
			node.put(MAX_HISTORY, appender.getMaxHistory());
		} else {
			node.remove(MAX_HISTORY);
		}
		if (null != appender.getMaxFileSize()) {
			node.put(MAX_FILE_SIZE, appender.getMaxFileSize());
		} else {
			node.remove(MAX_FILE_SIZE);
		}
		if (appender.isSeparateLogOutputsPerMdcProperty()) {
			node.put(SIFTING_MDC_PROPERTY_NAME, appender.getSiftingMdcPropertyName());
			node.put(SIFTING_MDC_PROPERTYDEFAULT_VALUE, StringUtils.trimToEmpty(appender.getSiftingMdcPropertyDefaultValue()));
		} else {
			node.remove(SIFTING_MDC_PROPERTY_NAME);
			node.remove(SIFTING_MDC_PROPERTYDEFAULT_VALUE);
		}
		if (!appender.isCompressRotatedLogs()) {
			node.putBoolean(COMPRESS_ROTATED_LOGS, false);
		} else {
			node.remove(COMPRESS_ROTATED_LOGS);
		}
	}

	@Override
	public void writeAppender(final Appender appender, final Preferences node) throws Exception {
		if (appender instanceof ConsoleAppender) {
			saveConsoleAppender((ConsoleAppender) appender, node);
		} else if (appender instanceof FileAppender) {
			saveFileAppender((FileAppender) appender, node);
		}
	}

}
