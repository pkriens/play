package org.osgi.util.promise;

import java.lang.reflect.InvocationTargetException;

/**
 * A Promise represents a future value, it handles the interaction to do
 * asynchronous processing. Promises are created with a {@link Resolver}. A
 * Resolver is the 'controller' of the Promise. The Promise is used by the
 * caller of an asynchronous function to get the result or handle the errors. It
 * can either get a callback when the Promise is resolved with a value or an
 * error, or it can be used in chaining. With chaining it provides a callbacks
 * that receives the resolved promise (with a value) and then returns a new
 * promise.
 * <p>
 * Both onresolve and chaining (then) can be repeated any number of times, even
 * long after the value has been resolved.
 * <p>
 * Simple on resolve time
 * 
 * <pre>
 * final Promise&lt;String&gt; foo = foo();
 * foo.onresolve(new Runnable() {
 * 	public void run() {
 * 		System.out.println(foo.get());
 * 	}
 * });
 * </pre>
 * 
 * Chaining
 * 
 * <pre>
 *      Success<String,String> doubler = new Success<>() {
 *      	public String call(Promise<String> p) {
 *              return Resolver.getDirectPromise(p.get()+p.get());
 *          }
 *      };
 *  	final Promise<String> foo = foo().then(doubler).then(doubler);
 *      foo.( new Runnable() { public void run() {
 *      	 System.out.println( foo.get() );
 *      } });
 * </pre>
 * 
 * @param <T>
 *            The result type associated with this promise
 */
public interface Promise<T> {

	/**
	 * Chain promise calls so they are executed in sequence. This promise will
	 * call one of the given callbacks when it gets resolved with either a value
	 * or an error. It returns a new promise that will get resolved with an
	 * error if this promise is resolved with an error or when the success
	 * throws an error. Otherwise it will get resolved with the value returned
	 * from the success callback.
	 * 
	 * @param success
	 *            Callback when this promise is successfully resolved.
	 * @param failure
	 *            Callback when this promise has failed
	 * @return A new promise that will get resolved when the success method
	 *         successfully returns or when there is a failure
	 * @throws Exception
	 *             Any other errors
	 */
	<R> Promise<R> then(Success<R, T> success, Failure<T> failure)
			throws Exception;

	/**
	 * Chain promise calls so they are executed in sequence. This promise will
	 * call one of the given callbacks when it gets resolved with either a value
	 * or an error. It returns a new promise that will get resolved with an
	 * error if this promise is resolved with an error or when the success
	 * throws an error. Otherwise it will get resolved with the value returned
	 * from the success callback.
	 * 
	 * @param success
	 *            Callback when this promise is successfully resolved.
	 * @return A new promise that will get resolved when the success method
	 *         successfully returns or when there is an error
	 * @throws Exception
	 *             Any other errors
	 */
	<R> Promise<R> then(Success<R, T> success) throws Exception;

	/**
	 * Called when this promise is resolved with either an error or a value.
	 * 
	 * @param done
	 *            the Runnable called when this promise is resolved.
	 * @throws Exception
	 */
	void onresolve(Runnable done) throws Exception;

	/**
	 * Returns <tt>true</tt> if this task completed.
	 * 
	 * Completion may be due to normal termination, an exception, or
	 * cancellation -- in all of these cases, this method will return
	 * <tt>true</tt>.
	 * 
	 * @return <tt>true</tt> if this task completed
	 */
	boolean isDone();

	/**
	 * Waits if necessary for the computation to complete, and then retrieves
	 * its result.
	 * 
	 * @return the computed result
	 * @throws InvocationTargetException
	 *             if the promise was resolved with an error
	 * @throws InterruptedException
	 *             if the current thread was interrupted while waiting
	 */
	T get() throws Exception;

	/**
	 * Waits if necessary for the computation to complete, and then retrieves
	 * its error. If no error happened, it will return null.
	 * 
	 * @return the error of this promise, can be null
	 * @throws InterruptedException
	 *             if the current thread was interrupted while waiting
	 */
	Throwable getError() throws IllegalStateException, InterruptedException;

	/**
	 * In general the callbacks are executed when the promise is resolved.
	 * However, this means that a callback can get executed before the
	 * {@link #then(Success)} or {@link #onresolve(Runnable)} methods have
	 * returned. Some sissies don't like that. So for those quiche eaters, the
	 * defer method will defer from calling anything back until the
	 * {@link #launch()} method is called.
	 * <p>
	 * The defer status is inherited for the {@link #then(Success)} chain.
	 * 
	 * @return this
	 */
	Promise<T> defer();

	/**
	 * If the {@link #defer()} method has been called, this method will initiate
	 * the callbacks. This can happen directly (i.e. they could be executed
	 * before this method returns) or later when the promise is not resolved
	 * yet.
	 */
	void launch();
}
