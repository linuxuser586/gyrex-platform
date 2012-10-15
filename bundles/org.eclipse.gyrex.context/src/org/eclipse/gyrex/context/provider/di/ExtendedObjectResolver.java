/*******************************************************************************
 * Copyright (c) 2012 AGETO Service GmbH and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 * 
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context.provider.di;

import java.lang.annotation.Annotation;

import org.eclipse.gyrex.context.IRuntimeContext;
import org.eclipse.gyrex.context.di.IRuntimeContextInjector;

import org.osgi.framework.BundleContext;

/**
 * Extension to {@link IRuntimeContextInjector} for enhanced resolution of
 * objects using {@link javax.inject.Qualifier} annotations found during
 * injection.
 * <p>
 * This class must be subclassed by clients that want to contribute a resolver
 * to assist in injection of contextual objects with a qualifier. It is part of
 * the contextual runtime injection API and should never be used directly by
 * clients. Resolvers must be made available as OSGi services using
 * {@link #SERVICE_NAME} (whiteboard pattern) and must have a property
 * {@link #ANNOTATION_PROPERTY} which defines the processed annotation type.
 * </p>
 * <p>
 * Note, this class is part of a service provider API which may evolve faster
 * than the general contextual runtime API. Please get in touch with the
 * development team through the prefered channels listed on <a
 * href="http://www.eclipse.org/gyrex">the Gyrex website</a> to stay up-to-date
 * of possible changes.
 * </p>
 */
public abstract class ExtendedObjectResolver {

	/**
	 * The OSGi service name for an annotation resolver service. This name can
	 * be used to obtain instances of the service.
	 * 
	 * @see BundleContext#getServiceReference(String)
	 */
	public static final String SERVICE_NAME = ExtendedObjectResolver.class.getName();

	/**
	 * An OSGi service property used to indicate the full qualified name of the
	 * annotation this resolver resolves.
	 * 
	 * @see BundleContext#getServiceReference(String)
	 */
	public static final String ANNOTATION_PROPERTY = "dependency.injection.annotation"; //$NON-NLS-1$

	/**
	 * This method is called by the dependency injection mechanism to obtain an
	 * object corresponding to the type and annotation.
	 * 
	 * @param type
	 *            the requested object type
	 * @param context
	 *            the context of the injection
	 * @param annotation
	 *            the found annotation (instance of type specified by
	 *            {@link #ANNOTATION_PROPERTY})
	 * @return object corresponding to the specified type and annotation (may be
	 *         <code>null</code>)
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public abstract Object get(Class<?> type, IRuntimeContext context, Annotation annotation);
}
