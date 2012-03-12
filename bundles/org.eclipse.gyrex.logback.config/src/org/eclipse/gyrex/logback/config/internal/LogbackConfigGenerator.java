/**
 * Copyright (c) 2012 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.logback.config.internal;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.eclipse.gyrex.logback.config.internal.model.LogbackConfig;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateFormatUtils;

/**
 * Generated a Logback configuration file based on snippets.
 */
public class LogbackConfigGenerator {

	private final long lastModified;
	private final File parentFolder;

	public LogbackConfigGenerator(final long lastModified, final File parentFolder) {
		this.lastModified = lastModified;
		this.parentFolder = parentFolder;
	}

	public File generateConfig() {
		// get state location
		if (!parentFolder.isDirectory() && !parentFolder.mkdirs()) {
			throw new IllegalStateException(String.format("Unable to create configs directory (%s).", parentFolder));
		}

		// save file
		final File configFile = new File(parentFolder, String.format("logback.%s.xml", DateFormatUtils.format(lastModified, "yyyyMMdd-HHmmssSSS")));
		OutputStream outputStream = null;
		XMLStreamWriter xmlStreamWriter = null;
		try {
			outputStream = new BufferedOutputStream(FileUtils.openOutputStream(configFile));
			final LogbackConfig config = new LogbackConfig();
			final XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
			xmlStreamWriter = outputFactory.createXMLStreamWriter(outputStream, CharEncoding.UTF_8);
			config.toXml(xmlStreamWriter);
			xmlStreamWriter.flush();
		} catch (final IOException e) {
			throw new IllegalStateException(String.format("Unable to create config file (%s).", ExceptionUtils.getRootCauseMessage(e)), e);
		} catch (final XMLStreamException e) {
			throw new IllegalStateException(String.format("Error writing config (%s).", ExceptionUtils.getRootCauseMessage(e)), e);
		} finally {
			if (null != xmlStreamWriter) {
				try {
					xmlStreamWriter.close();
				} catch (final Exception e) {
					// ignore
				}
			}
			IOUtils.closeQuietly(outputStream);
		}

		// cleanup directory
		removeOldFiles(parentFolder);

		return configFile;
	}

	private void removeOldFiles(final File parentFolder) {
		// only keep last 5 files
		File[] files = parentFolder.listFiles();
		if (null != files) {
			// remove any directories
			for (final File file : files) {
				if (file.isDirectory()) {
					FileUtils.deleteQuietly(file);
				}
			}

			// refresh
			files = parentFolder.listFiles();

			// remove old files
			if ((null != files) && (files.length > 5)) {
				// sort based on file name (reverse order)
				Arrays.sort(files, new Comparator<File>() {
					@Override
					public int compare(final File o1, final File o2) {
						return o2.getName().compareTo(o1.getName());
					}
				});

				for (int i = 5; i < files.length; i++) {
					FileUtils.deleteQuietly(files[i]);
				}
			}
		}
	}

}
