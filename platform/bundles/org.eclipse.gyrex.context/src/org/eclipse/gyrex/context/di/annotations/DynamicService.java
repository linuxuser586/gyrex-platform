/*******************************************************************************
 * Copyright (c) 2011 <enter-company-name-here> and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     <enter-developer-name-here> - initial API and implementation
 *******************************************************************************/
package org.eclipse.gyrex.context.di.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Qualifier;

import org.osgi.framework.ServiceReference;

/**
 * OSGi service lookup {@linkplain Qualifier qualifier}.
 * <p>
 * This qualifier triggers the injection of OSGi services. OSGi services will be
 * acquired from the OSGi service framework on behalf of the bundle that loaded
 * the class of the object under injection. If the class was not loaded by a
 * bundle no service will be injected.
 * </p>
 * <p>
 * Normally <code>Service</code> annotations will be marked as optional. Those
 * annotations establish a link rather then provide a value at the time of
 * injection.
 * </p>
 * <p>
 * The injected service may be a proxy implementing the service interface that
 * delegates to a real service. If a real service is not available at the time
 * of invoking any operation on the proxy, an {@link IllegalStateException} will
 * be thrown by the proxy.
 * </p>
 * <p>
 * If the dependency injector supports dynamic updates an attempt will be made
 * to injected the highest ranking available service instead of a proxy. If new
 * services become available or services go away the injected service may be
 * replaced dynamically at runtime. The injector may keep a reference to the
 * object under injection around. Please consult the injector documentation for
 * details on un-injecting/disposing objects which are no longer required. The
 * {@link #preferProxy() preferProxy} option can be set in order to suppress
 * dynamic updates and work with proxies instead.
 * </p>
 * <p>
 * The annotation can also be applied to a {@link Collection} or {@link List}
 * parameterized with the service interface class (eg.,
 * <code>Collection{@literal <}ServiceInterface{@literal >}</code>). In this
 * case, the field or method parameter will be injected with a read-only view of
 * all available services. The list of services will be ordered based on the
 * inverse natural order of service objects (which is spec'ed by
 * {@link ServiceReference#compareTo(Object)}), i.e. a service with a higher
 * ranking comes before a service with a lower ranking. The thread-safety
 * semantics of the underlying collection are similar to the ones of
 * {@link CopyOnWriteArrayList}. Therefore, traversal operations don't need to
 * synchronize on the returned collection object but do represent a snapshot of
 * available service at the time of accessing the list.
 * </p>
 * <p>
 * Example usage:
 * 
 * <pre>
 *   public class Car {
 *     {@literal @}Inject <strong>{@literal @}DynamicService</strong> ServiceInterface serviceProxy;
 *     {@literal @}Inject <strong>{@literal @}DynamicService</strong> Collection{@literal <}ServiceInterface{@literal >} services;
 *     {@literal @}Inject
 *     void set(<strong>{@literal @}DynamicService</strong> ServiceInterface service);
 *     ...
 *   }
 * </pre>
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.FIELD, ElementType.PARAMETER })
public @interface DynamicService {
	/**
	 * Allows to specify a filter which shall be applied to the service
	 * registrations. Only service registrations which match the filter will be
	 * considered.
	 */
	String filter() default "";

	/** Indicates that a proxy should be injected instead of dynamic injection. */
	boolean preferProxy() default false;
}
