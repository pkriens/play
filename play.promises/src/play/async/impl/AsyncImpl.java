package play.async.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.osgi.service.async.Async;
import org.osgi.service.async.AsyncDelegate;
import org.osgi.util.promise.Deferred;
import org.osgi.util.promise.Promise;

/**
 * Provide an impemen
 * 
 * @author aqute
 * 
 */
public class AsyncImpl implements Async {
	static Map<Class<?>, Object> nulls = new HashMap<>();
	static {
		nulls.put(boolean.class, Boolean.FALSE);
		nulls.put(byte.class, Byte.valueOf((byte) 0));
		nulls.put(short.class, Short.valueOf((short) 0));
		nulls.put(char.class, Character.valueOf((char) 0));
		nulls.put(int.class, Integer.valueOf(0));
		nulls.put(long.class, Long.valueOf(0L));
		nulls.put(float.class, Float.valueOf(0));
		nulls.put(double.class, Double.valueOf(0));
	}
	ThreadLocal<Promise<?>> results = new ThreadLocal<>();

	class AsyncHandler<T> implements InvocationHandler {
		private T target;
		private boolean async;

		public AsyncHandler(T target) {
			this.target = target;
			this.async = target instanceof AsyncDelegate;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			System.out.println("invoke " + method.getName() + "("
					+ Arrays.toString(args) + ")");

			Promise<?> promise = null;
			try {
				if (async)
					promise = ((AsyncDelegate<?>) target).async(method, args);

				if (promise == null) {
					// Synchronous
					Deferred<Object> deferred = new Deferred<>();
					promise = deferred.getPromise();
					try {
						Object result = method.invoke(target, args);
						deferred.resolve(result);
					} catch (Exception e) {
						deferred.fail(e);
					}
				}

				results.set(promise);

				Class<?> c = method.getReturnType();
				if (c.isPrimitive()) {
					return nulls.get(c);
				} else
					return null;

			} catch (Throwable e) {
				System.out.println("exception " + method.getName() + "("
						+ Arrays.toString(args) + ") " + e);
				throw e;
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T mediate(T target) {
		if (!(target instanceof AsyncDelegate))
			return target;


		Set<Class<?>> interfaces = new HashSet<>();
		collectInterfaces(interfaces, target.getClass());
		return (T) Proxy.newProxyInstance(target.getClass().getClassLoader(),
				interfaces.toArray(new Class[interfaces.size()]),
				new AsyncHandler<T>(target));
	}


	private void collectInterfaces(Set<Class<?>> interfaces,
			Class<? extends Object> clazz) {
		if (clazz == null || clazz == Object.class)
			return;

		if (clazz.isInterface()
				&& !clazz.getName().startsWith("java.lang.Serializable"))
			interfaces.add(clazz);

		collectInterfaces(interfaces, clazz.getSuperclass());
		for (Class<?> sub : clazz.getInterfaces()) {
			collectInterfaces(interfaces, sub);
		}
	}

	@Override
	public <R> Promise<R> call(R value) {
		@SuppressWarnings("unchecked")
		Promise<R> promise = (Promise<R>) results.get();
		if (promise == null) {
			promise = Deferred.getDirectPromise(value);
		}
		results.set(null);
		return promise;
	}


	@Override
	public <R> Promise<R> hold(R r) {
		return call(r).hold();
	}
}
