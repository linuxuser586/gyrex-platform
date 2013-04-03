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
package org.eclipse.gyrex.jobs.internal.externalprocess;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import org.slf4j.Logger;

/**
 * Reads and input stream asynchronously into a logger.
 */
public class AsyncLoggingInputStreamReader extends Thread {

	public static enum Level {
		INFO, ERROR
	}

	private final Logger log;
	private final Level level;
	private final InputStream in;
	private volatile boolean closed;
	private volatile String lastLine;

	/**
	 * Creates a new instance and starts it right away.
	 * 
	 * @param name
	 * @param in
	 * @param log
	 * @param level
	 * @throws IllegalArgumentException
	 */
	public AsyncLoggingInputStreamReader(final String name, final InputStream in, final Logger log, final Level level) throws IllegalArgumentException {
		super(name);
		if (in == null)
			throw new IllegalArgumentException("input stream must not be null");
		if (log == null)
			throw new IllegalArgumentException("logger must not be null");
		if (level == null)
			throw new IllegalArgumentException("level must not be null");
		this.in = in;
		this.log = log;
		this.level = level;
		setDaemon(true);
		closed = false;
		start();
	}

	public void close() {
		closed = true;
		IOUtils.closeQuietly(in);
	}

	/**
	 * Returns the last read line.
	 * 
	 * @return the last read line
	 */
	public String getLastLine() {
		return lastLine;
	}

	@Override
	public void run() {
		try {
			final LineIterator li = IOUtils.lineIterator(in, null);
			while (!closed && li.hasNext()) {
				final String line = lastLine = li.next();
				switch (level) {
					case ERROR:
						log.error(line);
						break;

					case INFO:
					default:
						log.info(line);
						break;
				}
			}
		} catch (final Exception | LinkageError | AssertionError e) {
			log.error("Unable to read from stream: {}", e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}
}