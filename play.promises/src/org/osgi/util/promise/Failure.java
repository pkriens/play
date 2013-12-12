package org.osgi.util.promise;

/**
 * Callback when the promise resolves with a failure.
 *
 * @param <Value> The value type of the promise.
 */
public interface Failure<Value> {
	void fail(Promise<Value> promise) throws Exception;
}
