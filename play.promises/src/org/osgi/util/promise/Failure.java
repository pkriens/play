package org.osgi.util.promise;

public interface Failure<Value> {
	void fail(Promise<Value> promise) throws Exception;
}
