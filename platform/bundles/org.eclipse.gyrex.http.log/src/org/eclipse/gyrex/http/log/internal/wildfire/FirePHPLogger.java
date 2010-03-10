/**
 * Copyright (c) 2009 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.log.http.internal.wildfire;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.eclipse.equinox.log.ExtendedLogEntry;
import org.eclipse.equinox.log.SynchronousLogListener;
import org.eclipse.gyrex.log.internal.ConsoleLogger;
import org.eclipse.gyrex.log.internal.LogEvent;
import org.eclipse.gyrex.log.internal.LogEventSourceData;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogService;

/**
 * A FirePHP Logger prototype.
 * 
 * @TODO once we decided on a logging backend we may need to change this
 *       implementation
 */
public class FirePHPLogger implements SynchronousLogListener {

	private static final ThreadLocal<HttpServletResponse> responseHolder = new ThreadLocal<HttpServletResponse>();
	private static final ThreadLocal<AtomicInteger> sequenceHolder = new ThreadLocal<AtomicInteger>();

	public static String getFile(final LogEntry entry) {
		final LogEvent event = ConsoleLogger.getLogEvent(entry);
		if (null != event) {
			return event.getSourceData().getFileName();
		}

		return null;
	}

	public static int getLine(final LogEntry entry) {
		final LogEvent event = ConsoleLogger.getLogEvent(entry);
		if (null != event) {
			return event.getSourceData().getLineNumber();
		}

		return 0;
	}

	public static String getSimpleBody(final LogEntry entry) {
		final StringBuilder body = new StringBuilder();
		final LogEvent logEvent = ConsoleLogger.getLogEvent(entry);
		if (null != logEvent) {
			final LogEventSourceData sourceData = logEvent.getSourceData();
			if (null != sourceData) {
				body.append("(").append(sourceData.getFileName()).append(", line ").append(sourceData.getLineNumber()).append(") ");
			}
		}
		if (null != entry.getException()) {
			body.append(entry.getException());
		}
		return body.toString();
	}

	public static String getType(final LogEntry entry) {
		switch (entry.getLevel()) {
			case LogService.LOG_INFO:
				return "INFO";
			case LogService.LOG_WARNING:
				return "WARN";
			case LogService.LOG_ERROR:
				return "ERROR";

			case LogService.LOG_DEBUG:
			default:
				return "LOG";
		}
	}

	public static void setResponse(final HttpServletResponse resp) {
		responseHolder.set(resp);
		sequenceHolder.set(null != resp ? new AtomicInteger() : null);
	}

	private String createLogResponse(final LogEntry entry) throws IOException {

		final StringWriter writer = new StringWriter(2048);
		final JsonGenerator json = new JsonFactory().createJsonGenerator(writer);

		final String type = getType(entry);
		final String file = getFile(entry);
		final int line = getLine(entry);
		final String label = ConsoleLogger.getLabel(entry);

		// start
		json.writeStartArray();

		// header/meta
		json.writeStartObject();
		json.writeFieldName("Type");
		json.writeString(type);
		json.writeFieldName("File");
		if (null != file) {
			json.writeString(file);
		} else {
			json.writeNull();
		}
		json.writeFieldName("Line");
		if (line > 0) {
			json.writeNumber(line);
		} else {
			json.writeNull();
		}
		json.writeFieldName("Label");
		if (null != label) {
			json.writeString(label);
		} else {
			json.writeNull();
		}
		json.writeEndObject();

		// body
		json.writeString(getSimpleBody(entry));

		// end
		json.writeEndArray();

		json.flush();

		return writer.toString();
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.log.LogListener#logged(org.osgi.service.log.LogEntry)
	 */
	@Override
	public void logged(final LogEntry entry) {
		// get response
		final HttpServletResponse servletResponse = responseHolder.get();
		if (null == servletResponse) {
			return;
		}

		try {
			// send header
			if (!servletResponse.containsHeader("X-Wf-Protocol-1")) {
				//X-Wf-Protocol-1     http://meta.wildfirehq.org/Protocol/JsonStream/0.2
				//X-Wf-1-Plugin-1     http://meta.firephp.org/Wildfire/Plugin/FirePHP/Library-FirePHPCore/0.3
				//X-Wf-1-Structure-1  http://meta.firephp.org/Wildfire/Structure/FirePHP/FirebugConsole/0.1

				servletResponse.setHeader("X-Wf-Protocol-1", "http://meta.wildfirehq.org/Protocol/JsonStream/0.2");
				servletResponse.setHeader("X-Wf-1-Plugin-1", "http://meta.firephp.org/Wildfire/Plugin/FirePHP/Library-FirePHPCore/0.3");
				servletResponse.setHeader("X-Wf-1-Structure-1", "http://meta.firephp.org/Wildfire/Structure/FirePHP/FirebugConsole/0.1");
			}

			String response = createLogResponse(entry);
			if (null != response) {
				final StringBuilder header = new StringBuilder(4100);

				// append full length to first line
				header.append(response.length());

				// send multiple lines if more than 4000 chars long
				while (response.length() > 4000) {
					header.append('|');
					header.append(response.substring(0, 4000));
					header.append("|\\");
					servletResponse.setHeader("X-Wf-1-1-1-" + sequenceHolder.get().incrementAndGet(), header.toString());
					header.setLength(0);
					response = response.substring(4000);
				}

				// send (last) line
				header.append('|');
				header.append(response);
				header.append('|');
				servletResponse.setHeader("X-Wf-1-1-1-" + sequenceHolder.get().incrementAndGet(), header.toString());
			}
		} catch (final Exception e) {
			// TODO consider logging this (but with what?)
			e.printStackTrace();
		}
	}

	void writeEnhancedBody(final LogEntry entry, final JsonGenerator json) throws IOException, JsonGenerationException {
		json.writeStartObject();
		json.writeFieldName("Bundle");
		if (null != entry.getBundle()) {
			json.writeString(entry.getBundle().toString());
		} else {
			json.writeNull();
		}
		json.writeFieldName("ServiceReference");
		if (null != entry.getServiceReference()) {
			json.writeString(entry.getServiceReference().toString());
		} else {
			json.writeNull();
		}
		if (entry instanceof ExtendedLogEntry) {
			final ExtendedLogEntry extEntry = (ExtendedLogEntry) entry;
			json.writeFieldName("LoggerName");
			if (null != extEntry.getLoggerName()) {
				json.writeString(extEntry.getLoggerName().toString());
			} else {
				json.writeNull();
			}
			final LogEvent logEvent = ConsoleLogger.getLogEvent(extEntry);
			if (null != logEvent) {
				json.writeFieldName("Source");
				if (null != logEvent.getSourceData()) {
					json.writeString(logEvent.getSourceData().toString());
				} else {
					json.writeNull();
				}
				json.writeFieldName("Attributes");
				if (null != logEvent.getAttributes()) {
					json.writeStartObject();
					final Set<Entry<String, String>> entrySet = logEvent.getAttributes().entrySet();
					for (final Entry<String, String> attributeEntry : entrySet) {
						json.writeFieldName(attributeEntry.getKey());
						if (null != attributeEntry.getValue()) {
							json.writeString(attributeEntry.getValue().toString());
						} else {
							json.writeNull();
						}
					}
					json.writeEndObject();
				} else {
					json.writeNull();
				}
				json.writeFieldName("Tags");
				final Set<String> tags = logEvent.getTags();
				if ((null != tags) && !tags.isEmpty()) {
					json.writeStartArray();
					for (final String tag : tags) {
						json.writeString(tag);
					}
					json.writeEndArray();
				} else {
					json.writeNull();
				}
			}
		}
		json.writeEndObject();
	}

}
