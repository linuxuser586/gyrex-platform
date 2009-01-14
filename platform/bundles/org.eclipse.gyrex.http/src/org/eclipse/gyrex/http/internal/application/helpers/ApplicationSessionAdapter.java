/*******************************************************************************
 * Copyright (c) 2008 Gunnar Wagenknecht and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.cloudfree.http.internal.application.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import org.eclipse.cloudfree.http.internal.application.manager.ApplicationRegistration;

/**
 * Adapts a {@link HttpSession} so that the correct application specific
 * {@link ServletContext} is returned.
 */
@SuppressWarnings("deprecation")
public class ApplicationSessionAdapter implements HttpSession {

	private final HttpSession session;
	private final ServletContext servletContext;
	private final Map<String, Object> attributes;
	private final String applicationAttributesKey;

	@SuppressWarnings("unchecked")
	public ApplicationSessionAdapter(final HttpSession session, final ServletContext servletContext, final ApplicationRegistration applicationRegistration) {
		this.session = session;
		this.servletContext = servletContext;
		applicationAttributesKey = ApplicationSessionAdapter.class.getName().concat(applicationRegistration.getApplicationId());
		final Object existingApplicationAttribute = session.getAttribute(applicationAttributesKey);
		if (existingApplicationAttribute instanceof Map) {
			attributes = (Map<String, Object>) existingApplicationAttribute;
		} else {
			attributes = new ConcurrentHashMap<String, Object>(3);
			session.setAttribute(applicationAttributesKey, attributes);
		}
	}

	@Override
	public Object getAttribute(final String name) {
		return attributes.get(name);
	}

	@Override
	public Enumeration getAttributeNames() {
		return Collections.enumeration(new ArrayList<String>(attributes.keySet()));
	}

	@Override
	public long getCreationTime() {
		return session.getCreationTime();
	}

	@Override
	public String getId() {
		return session.getId();
	}

	@Override
	public long getLastAccessedTime() {
		return session.getLastAccessedTime();
	}

	@Override
	public int getMaxInactiveInterval() {
		return session.getMaxInactiveInterval();
	}

	@Override
	public ServletContext getServletContext() {
		return servletContext;
	}

	@Override
	@Deprecated
	public HttpSessionContext getSessionContext() {
		throw new IllegalStateException("HttpSession#getSessionContext is deprecated and not supported in CloudFree");
	}

	@Override
	@Deprecated
	public Object getValue(final String name) {
		return attributes.get(name);
	}

	@Override
	@Deprecated
	public String[] getValueNames() {
		return new ArrayList<String>(attributes.keySet()).toArray(new String[0]);
	}

	@Override
	public void invalidate() {
		try {
			session.invalidate();
		} finally {
			attributes.clear();
		}
	}

	@Override
	public boolean isNew() {
		return session.isNew();
	}

	@Override
	@Deprecated
	public void putValue(final String name, final Object value) {
		setAttribute(name, value);
	}

	@Override
	public void removeAttribute(final String name) {
		attributes.remove(name);

		// touch session
		session.setAttribute(applicationAttributesKey, attributes);
	}

	@Override
	@Deprecated
	public void removeValue(final String name) {
		removeAttribute(name);
	}

	@Override
	public void setAttribute(final String name, final Object value) {
		attributes.put(name, value);

		// touch session
		session.setAttribute(applicationAttributesKey, attributes);
	}

	@Override
	public void setMaxInactiveInterval(final int interval) {
		session.setMaxInactiveInterval(interval);
	}
}
