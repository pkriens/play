package org.osgi.util.promise;

/**
 * Callback when the promise was resolved with a value
 *
 * @param <Return> The return value of this callback
 * @param <Value> The given value of this callback
 */
public interface Success<Return,Value> {
	Promise<Return> call(Promise<Value> promise) throws Exception;
}
