package org.osgi.util.promise;

/**
 * Callback when the promise resolves with a failure.
 *
 * @param <Value> The value type of the promise.
 */
public interface Failure<Value> {
	/**
	 * Called when the promise was resolved with an error.
	 * @param promise the promise this callback was registered on
	 */
	void fail(Promise<Value> promise) throws Exception;
}
