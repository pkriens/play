package org.osgi.service.async;

import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Resolver;

/**
 * An async service. This service can be used to call methods asynchronously if
 * the underlying implementor understands this service. Other services that are
 * unaware of the async service are executed synchronously.
 * <p>
 * To use this service, you need to have a service defined by an interface.
 * Using the {@link #mediate(Object)} method you turn the service then in a
 * mediator between you and the async service.
 * <p>
 * When a method is called on the mediator, the async service will set a flag
 * that makes this invocation ok to go asynchronous. If the called object
 * understands async, it will ask the async service for a Resolver. If it does
 * not get this object, the invocation must be executed synchronously.
 * 
 * If it has a resolver, it should initiate the asynchronous request and return
 * the associated promise. If the async process is finished, it can signal the result
 * or error through the Resolver.
 * <p>
 * To get the promise, the caller must invoke the asynchronous method specially/ It should
 * invoke it so that the result value (which could be a dummy) is received by the
 * {@link #deferred(Object)} or {@link #invoke(Object)} method. Since the async method
 * is executed on the same thread by definition, the async service can link the invocation
 * of these methods to the Resolver that potentially was created by the async method.
 * If there was such a Resolver, this is returned. Otherwise the return value is used
 * to create an immediate promise.
 */
public interface Async {
	<T> T mediate(T target);

	<R> Promise<R> invoke(R r);

	<R> Promise<R> deferred(R r);

	<T> Resolver<T> getResolver();
}
