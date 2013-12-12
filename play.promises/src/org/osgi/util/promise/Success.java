package org.osgi.util.promise;

public interface Success<Return,Value> {
	Promise<Return> call(Promise<Value> promise) throws Exception;
}
