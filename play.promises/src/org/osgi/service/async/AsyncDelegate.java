package org.osgi.service.async;

import java.lang.reflect.Method;

import org.osgi.util.promise.Promise;

public interface AsyncDelegate<T> {
	Promise<?> async( Method m, Object[] args) throws Exception;
}
