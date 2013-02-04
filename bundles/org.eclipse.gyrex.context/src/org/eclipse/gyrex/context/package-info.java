/*******************************************************************************
 * Copyright (c) 2010, 2012 AGETO and others.
 * All rights reserved.
 *  
 * This program and the accompanying materials are made available under the 
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *******************************************************************************/
/**
 * This package (and its sub-packages) define the Gyrex Contextual Runtime.
 * <p>
 * Eclipse is a dynamic and extensible platform. For example, the Eclipse
 * extension registry allows for a great extensibility where some code defines
 * an extension point and allows others to provide an implementation for it.
 * Another possibility are OSGi services.
 * </p>
 * <p>
 * The Gyrex Platform includes a context sensitive runtime to support dynamic
 * constraints. Every operation in Gyrex happens within (or on behalf of) a
 * particular context. The contextual runtime is one of the key elements of
 * Gyrex and allows - for example - the development of multi-tenant software
 * offerings.
 * </p>
 * <p>
 * Let's look at an example. In an online shopping system the shopping cart
 * calculation could be made extensible using the extension registry or using
 * OSGi services. A default implementation would be provided which does a simple
 * total calculation. This will be the default for all shops served by your
 * system. The other day a new client comes along and has very special
 * requirements for the shopping cart calculation. With OSGi or the Eclipse
 * extension registry this is a no-brainer. You simply implement the special
 * calculation and provide it as an OSGi service. But now the "host" code needs
 * to deal with two services (or two extension). It need to know which one to
 * use in which shop. Here comes the contextual runtime to the rescue. Using the
 * contextual runtime a shop will be defined as the runtime context. Your code
 * simply delegates the call. Instead of asking the extension registry or the
 * OSGi service registry directly, it asks the context which service (or
 * extension) to use. The rest will all be configurable through an
 * administration interface or via APIs. Your code does not need to implement
 * any filtering or permission checks. The contextual runtime takes care of
 * that.
 * </p>
 * <h4>The Runtime Context</h4>
 * <p>
 * The runtime context is the central element of the contextual runtime. It
 * provides APIs to get a specific, ready-configured service for the client code
 * base to use. The lookup of the service will be very simple. You simply pass a
 * class object to context and it will return an instance of the class for you
 * to work with. Thus, the context is essentially an object registry. However,
 * it's dynamic, i.e. it can change between invocations.
 * </p>
 * <p>
 * Contexts are hierarchical organized in a path like structure. There is a root
 * context ("/") which typically defines the core pieces of a system. In a
 * single system it's actually possible to just work with the root context.
 * </p>
 * <p>
 * Context values are inherited. Thus, if a context does not have a value
 * defined, it's parent will be queried until a value is found. It's also
 * possible to explicitly undefine (set to "null") a value in a client context.
 * </p>
 * <p>
 * Note, for security reasons (see below) a context will not allow simple
 * retrieval of its parent context. Instead always the context registry has to
 * be used to lookup a particular context. For the same reasons, a context
 * does not offer modification APIs to clients.
 * </p>
 * <h4>Contextual Objects</h4>
 * <p>
 * The objects ("values") provided by the context are pure Java objects. They
 * can be OSGi services, Eclipse extensions, Eclipse adapters from the
 * IAdapterFactory or other contextual objects. Contextual objects are objects
 * provided specifically for a context. A provider can be registered as an OSGi
 * service which will provide context specific objects. To some extend, this can
 * be compare to OSGi service factories. The only difference is, that the
 * service implementation is not bound to the bundle requesting the service but
 * to the context requesting the service. This allows for a concept of context
 * singletons.
 * </p>
 * <h4>Context Registry, Manager & Security</h4>
 * <p>
 * The configuration of contexts is persisted and kept across sessions. A
 * central registry is available for "loading" of contexts. A manager is
 * available for manipulating the configuration of a context. However, access to
 * the registry as well as the manager may be guarded by security constraints to
 * allow only trusted code access to a specific set of contexts. This prevents
 * client code with a lower lever of trust to not execute operations outside of
 * the client context.
 * </p>
 * <h4>Context Preferences</h4>
 * <p>
 * Context preferences allow to maintain context specific preferences which
 * support the hierarchical inheritance model of the contextual runtime. They
 * allow to set preferences on a context which will be inherited into
 * sub-contexts. Context preferences are based on vanilla Eclipse preferences.
 * However, in a secure environment not every code may have access to the same
 * set of preferences.
 * </p>
 */
package org.eclipse.gyrex.context;

///**
// */
//class Dummy {
//
//}
