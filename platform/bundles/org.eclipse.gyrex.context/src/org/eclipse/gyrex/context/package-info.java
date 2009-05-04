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
 * Let's look at an example. In an e-commerce shopping system the shopping cart
 * calculation could be made extensible using the extension registry or using
 * OSGi services. A default implementation would e provided which does a simple
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
 * single system it's acutally possible to just work with the root context.
 * </p>
 * <p>
 * Context values are inherited. Thus, if a context does not have a value
 * defined, it's parent will be queried until a value is found. It's also
 * possible to explicitly undefine (set to "null") a value in a client context.
 * </p>
 * <p>
 * Note, for security reasons (see below) a context will not allow simple
 * retrieval of its parent context. Instead always the context registry has to
 * be used to lookup a particular context.
 * </p>
 * <h4>Contextual Objects</h4>
 * <p>
 * The objects ("values") provided by the context are pure Java objects. They
 * can be OSGi services, Eclipse extensions, Eclipse adapters from the
 * IAdapterFactory or other contextual objects. Contextual objects are objects
 * provided specifically for a context. A provider can be registred as an OSGi
 * service which will provide context specific objects. To some extend, this can
 * be compare to OSGi service factories. The only difference is, that the
 * service implementation is not bound to the bundle requesting the service but
 * to the context requesting the service. This allows for a concept of context
 * singletons.
 * </p>
 * <h4>Context Registry & Security</h4>
 * <p>
 * The configuration of contexts is persisted and kept across sessions. A
 * central registry is available of "loading" of contexts. However, access to
 * the registry may be guarded by security constraints to allow only trusted
 * code access to a specific set of contexts. This prevents client code with a
 * lower lever of trust to not execute operations outside of the client context.
 * </p>
 */
package org.eclipse.gyrex.context;

///**
// */
//class Dummy {
//
//}
