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
package org.eclipse.gyrex.logback.config.model;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang.StringUtils;

import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;

/**
 * An appender which writes to a log file.
 */
public class FileAppender extends Appender {

	public static enum RotationPolicy {
		DAILY, WEEKLY, MONTHLY, SIZE;
	}

	private String fileName;
	private RotationPolicy rotationPolicy;
	private String maxFileSize; // only used when rotationPolicy==SIZE
	private String maxHistory; // only used when rotationPolicy!=SIZE
	private boolean compressRotatedLogs;

	/**
	 * Creates a new instance.
	 */
	public FileAppender() {
		super("file");
	}

	@Override
	public final boolean canSift() {
		return true;
	}

	@Override
	public String getAppenderClassName() {
		return RollingFileAppender.class.getName();
	}

	public String getFileName() {
		return fileName;
	}

	public String getMaxFileSize() {
		return maxFileSize;
	}

	public String getMaxHistory() {
		return maxHistory;
	}

	public RotationPolicy getRotationPolicy() {
		return rotationPolicy;
	}

	public boolean isCompressRotatedLogs() {
		return compressRotatedLogs;
	}

	public void setCompressRotatedLogs(final boolean compressRotatedLogs) {
		this.compressRotatedLogs = compressRotatedLogs;
	}

	public void setFileName(final String fileName) {
		this.fileName = fileName;
	}

	public void setMaxFileSize(final String maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	public void setMaxHistory(final String maxHistory) {
		this.maxHistory = maxHistory;
	}

	public void setRotationPolicy(final RotationPolicy rotationPolicy) {
		this.rotationPolicy = rotationPolicy;
	}

	@Override
	protected void writeAppenderContent(final XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement("file");
		writer.writeCharacters(String.format("${BASE_PATH}/%s", getFileName()));
		writer.writeEndElement();
		writeRotation(writer);
	}

	private void writeRotation(final XMLStreamWriter writer) throws XMLStreamException {
		final RotationPolicy policy = getRotationPolicy();
		if (null == policy)
			return;

		switch (policy) {
			case SIZE:
				writeSizeBasedRotation(writer);
				break;

			default:
			case DAILY:
			case WEEKLY:
			case MONTHLY:
				writeTimeBasedRotation(writer, policy);
				break;

		}
	}

	private void writeSizeBasedRotation(final XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement("rollingPolicy");
		writer.writeAttribute("class", FixedWindowRollingPolicy.class.getName());
		{
			writer.writeStartElement("fileNamePattern");
			writer.writeCharacters(StringUtils.substringBeforeLast(getFileName(), "."));
			writer.writeCharacters(".%i");
			final String extension = StringUtils.substringAfter(getFileName(), ".");
			if (StringUtils.isNotBlank(extension)) {
				writer.writeCharacters(".");
				writer.writeCharacters(extension);
			}
			if (isCompressRotatedLogs()) {
				writer.writeCharacters(".gz");
			}
			writer.writeEndElement();

			writer.writeStartElement("minIndex");
			writer.writeCharacters("1");
			writer.writeEndElement();

			writer.writeStartElement("maxIndex");
			writer.writeCharacters("3");
			writer.writeEndElement();
		}
		writer.writeEndElement();

		writer.writeStartElement("triggeringPolicy");
		writer.writeAttribute("class", SizeBasedTriggeringPolicy.class.getName());
		{
			String maxFileSize = getMaxFileSize();
			if (StringUtils.isBlank(maxFileSize)) {
				maxFileSize = "1MB";
			}
			writer.writeStartElement("maxFileSize");
			writer.writeCharacters(maxFileSize);
			writer.writeEndElement();
		}
		writer.writeEndElement();
	}

	private void writeTimeBasedRotation(final XMLStreamWriter writer, final RotationPolicy policy) throws XMLStreamException {
		writer.writeStartElement("rollingPolicy");
		writer.writeAttribute("class", TimeBasedRollingPolicy.class.getName());
		{
			writer.writeStartElement("fileNamePattern");
			writer.writeCharacters(StringUtils.substringBeforeLast(getFileName(), "."));
			switch (policy) {
				case MONTHLY:
					writer.writeCharacters(".%d{yyyyMM}");
					break;
				case WEEKLY:
					writer.writeCharacters(".%d{yyyyww}");
					break;
				case DAILY:
				default:
					writer.writeCharacters(".%d{yyyyMMdd}");
					break;
			}
			final String extension = StringUtils.substringAfter(getFileName(), ".");
			if (StringUtils.isNotBlank(extension)) {
				writer.writeCharacters(".");
				writer.writeCharacters(extension);
			}
			if (isCompressRotatedLogs()) {
				writer.writeCharacters(".gz");
			}
			writer.writeEndElement();

			writer.writeStartElement("maxHistory");
			String maxHistory = getMaxHistory();
			if (StringUtils.isBlank(maxHistory)) {
				maxHistory = policy == RotationPolicy.DAILY ? "30" : policy == RotationPolicy.WEEKLY ? "52" : "12";
			}
			writer.writeCharacters(maxHistory);
			writer.writeEndElement();
		}
		writer.writeEndElement();
	}
}
