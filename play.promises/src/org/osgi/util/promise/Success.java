package org.osgi.util.promise;

/**
 * Callback when the promise was resolved with a value
 *
 * @param <Return> The return value of this callback
 * @param <Value> The given value of this callback
 */
public interface Success<Return,Value> {
	/**
	 * Called when the previous promise was successful. We need to return a new
	 * promise which is used to resolve the next in the chain. Note, this is not
	 * the promise returned in the then method! This is impossible because we
	 * return before this method is called. The then method returns a new promise
	 * that gets resolved after this method has returned. Tricky ... yes.
	 * @param promise
	 * @return
	 */
	Promise<Return> call(Promise<Value> promise) throws Exception;
}
