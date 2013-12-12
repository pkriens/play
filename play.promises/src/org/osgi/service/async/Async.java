package org.osgi.service.async;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Resolver;



public interface Async {
	<T> T mediate(T target);
	<R> Promise<R> invoke(R r);
	<R> Promise<R> promise(R r);
	<T> Resolver<T> getResolver();
}
