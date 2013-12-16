package org.osgi.service.async;

import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Deferred;

/**
 * An async service. This service can be used to call methods asynchronously if
 * the underlying implementor understands this Async service. Other services
 * that are unaware of the async service are executed synchronously.
 * <p>
 * To use this service, you need to have a service defined by an interface.
 * Using the {@link #mediate(Object)} method you turn the service then in a
 * mediator between you and the async service.
 * <p>
 * When a method is called on the mediator, the async service will set a flag
 * that makes this invocation ok to go asynchronous. If the called object
 * understands async, it will ask the async service for a {@link Deferred}. If
 * it does not get such an object, the invocation must be executed
 * synchronously.
 * 
 * If the implementation has a {@link Deferred}, it should initiate the
 * asynchronous request. If the request is finished, it can signal the result or
 * error through the {@link Deferred}.
 * <p>
 * To get the promise, the caller must invoke the asynchronous method specially.
 * It should invoke it so that the result value (which could be a dummy) is
 * received by the {@link #hold(Object)} or {@link #call(Object)} method. Since
 * the async method is executed on the same thread by definition, the async
 * service can link the invocation of these methods to the {@link Deferred} that
 * potentially was created by the async method. If there was such a
 * {@link Deferred}, this is returned. Otherwise the return value is used to
 * create an immediate promise.
 */
public interface Async {

	/**
	 * Create a mediator on the given object. The mediator will call the methods
	 * on the interfaces implemented by target while marking the thread. If some
	 * party calls {@link #createDeferred()} while this mark is active, then the
	 * {@link Deferred} will be associated with this call.
	 * <p>
	 * Calls this mediator should take place inside a call to
	 * {@link #call(Object)}
	 * 
	 * <pre>
	 * I i = async.mediate(s);
	 * Promise&lt;String&gt; p = async.call(i.foo());
	 * </pre>
	 * 
	 * @param target
	 *            The service object
	 * @return A mediator on the service object
	 */
	<T> T mediate(T target);

	/**
	 * The mediator will mark the thread when invoking a method. If the
	 * implementation calls {@link #createDeferred()} during this marking it
	 * will get a Deferred back associated with the invocation. This method will
	 * then return the corresponding promise.
	 * 
	 * @param r
	 *            the return value (is ignored if not async)
	 * @return a Promise
	 */
	<R> Promise<R> call(R r);

	/**
	 * Same as {@link #call(Object)} but it returns a Promise that will not call
	 * its callbacks until {@link Promise#launch()} is called.
	 * 
	 * @param r
	 *            the return value (is ignored if not async)
	 * @return a Promise that is holding its callbacks until the launch method
	 *         is called
	 */
	<R> Promise<R> hold(R r);

	/**
	 * This method is called by implementations that can execute their
	 * invocations asynchronously. If this method is null, the method was
	 * invoked synchronously and the implementation should not return until the
	 * asynchronous request has been finalized. If it is not null, it should
	 * return immediately and signal the result by resolving the returned
	 * Deferred object. It is allowed that the Deferred is resolved with a
	 * failure if the request surpasses a timeout.
	 * 
	 * @return A deferred or null
	 * 
	 * TODO maybe we should hand it a Promise?
	 */
	<T> Deferred<T> createDeferred();
	
}
