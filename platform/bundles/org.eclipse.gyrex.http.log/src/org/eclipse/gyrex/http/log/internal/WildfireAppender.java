/**
 * Copyright (c) 2010 Gunnar Wagenknecht and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 */
package org.eclipse.gyrex.log.http.internal;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import ch.qos.logback.core.status.ErrorStatus;

/**
 *
 */
public class WildfireAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {

	private static final ThreadLocal<HttpServletResponse> responseHolder = new ThreadLocal<HttpServletResponse>();
	private static final ThreadLocal<AtomicInteger> sequenceHolder = new ThreadLocal<AtomicInteger>();

	public static String getType(final ILoggingEvent event) {
		switch (event.getLevel().levelInt) {
			case Level.INFO_INT:
			case Level.WARN_INT:
			case Level.ERROR_INT:
				return event.getLevel().levelStr;

			default:
				return "LOG";
		}
	}

	public static void setResponse(final HttpServletResponse resp) {
		responseHolder.set(resp);
		sequenceHolder.set(null != resp ? new AtomicInteger() : null);
	}

	Layout<ILoggingEvent> layout;
	String pattern;

	@Override
	protected void append(final ILoggingEvent event) {
		if (!isStarted()) {
			return;
		}

		try {
			// this step avoids LBCLASSIC-139
			if (event instanceof DeferredProcessingAware) {
				((DeferredProcessingAware) event).prepareForDeferredProcessing();
			}

			// get response
			final HttpServletResponse servletResponse = responseHolder.get();
			if (null == servletResponse) {
				return;
			}

			// send header
			if (!servletResponse.containsHeader("X-Wf-Protocol-1")) {
				//X-Wf-Protocol-1     http://meta.wildfirehq.org/Protocol/JsonStream/0.2
				//X-Wf-1-Plugin-1     http://meta.firephp.org/Wildfire/Plugin/FirePHP/Library-FirePHPCore/0.3
				//X-Wf-1-Structure-1  http://meta.firephp.org/Wildfire/Structure/FirePHP/FirebugConsole/0.1

				servletResponse.setHeader("X-Wf-Protocol-1", "http://meta.wildfirehq.org/Protocol/JsonStream/0.2");
				servletResponse.setHeader("X-Wf-1-Plugin-1", "http://meta.firephp.org/Wildfire/Plugin/FirePHP/Library-FirePHPCore/0.3");
				servletResponse.setHeader("X-Wf-1-Structure-1", "http://meta.firephp.org/Wildfire/Structure/FirePHP/FirebugConsole/0.1");
			}

			String response = createLogResponse(event);
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

		} catch (final IOException ioe) {
			// as soon as an exception occurs, move to non-started state
			// and add a single ErrorStatus to the SM.
			started = false;
			addStatus(new ErrorStatus("IO failure in appender", this, ioe));
		}
	}

	private String createLogResponse(final ILoggingEvent event) throws IOException {

		final StringWriter writer = new StringWriter(2048);
		final JsonGenerator json = new JsonFactory().createJsonGenerator(writer);

		final String type = getType(event);

		final String file = event.hasCallerData() ? event.getCallerData()[0].getFileName() : null;
		final int line = event.hasCallerData() ? event.getCallerData()[0].getLineNumber() : 0;
		final String label = event.getFormattedMessage();

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
		final Layout<ILoggingEvent> patternLayout = layout;
		if (null != patternLayout) {
			json.writeString(patternLayout.doLayout(event));
		} else {
			json.writeString("");
		}

		// end
		json.writeEndArray();

		json.flush();

		return writer.toString();
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(final String pattern) {
		this.pattern = pattern;
	}

	@Override
	public void start() {
		final PatternLayout patternLayout = new PatternLayout();
		patternLayout.setContext(context);
		patternLayout.setPattern(getPattern());
		patternLayout.start();
		layout = patternLayout;
		super.start();
	}

	@Override
	public void stop() {
		super.stop();
		final PatternLayout patternLayout = (PatternLayout) layout;
		if (null != patternLayout) {
			patternLayout.stop();
			layout = null;
		}
	}

}
