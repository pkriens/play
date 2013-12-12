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
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.Resolver;

/**
 * Provide an impemen
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
	
	// TODO This likely needs to be pushed on a stack
	// for potential recursion. Not got my head around it yet
	
	ThreadLocal<Resolver<?>> resolvers = new ThreadLocal<>();
	ThreadLocal<Object> active = new ThreadLocal<>();

	class Handler<T> implements InvocationHandler {
		private T target;

		public Handler(T target) {
			this.target = target;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			System.out.println("invoke " + method.getName() + "("
					+ Arrays.toString(args) + ")");

			assert resolvers.get() == null;

			try {
				active.set(true);
				resolvers.set(null);
				Object result = method.invoke(target, args);
				if (resolvers.get() == null) {
					System.out.println("was not async " + method.getName()
							+ "(" + Arrays.toString(args) + ")");
					return result;
				}

			} catch (Throwable e) {
				System.out.println("exception " + method.getName() + "("
						+ Arrays.toString(args) + ") " + e);
				throw e;
			} finally {
				active.set(null);
			}
			Class<?> c = method.getReturnType();
			if (c.isPrimitive()) {
				return nulls.get(c);
			} else
				return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T mediate(T target) {
		Set<Class<?>> interfaces = new HashSet<>();
		collectInterfaces(interfaces, target.getClass());
		return (T) Proxy.newProxyInstance(target.getClass().getClassLoader(),
				interfaces.toArray(new Class[interfaces.size()]),
				new Handler<T>(target));
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
	public <R> Promise<R> invoke(R value) {
		if (resolvers.get() != null) {
			System.out.println("invoke result  " + value + " is async");
			@SuppressWarnings("unchecked")
			Promise<R> promise = (Promise<R>) resolvers.get().getPromise();
			resolvers.set(null);
			return promise;
		} else {
			System.out.println("invoke result  " + value + " is sync");
			Resolver<R> resolver = new Resolver<>();
			resolver.resolve(value);
			return resolver.getPromise();
		}
	}

	@Override
	public <T> Resolver<T> getResolver() {
		System.out.println("get resolver");
		if (active.get()!= null) {
			Resolver<T> resolver = new Resolver<>();
			resolvers.set(resolver);
			return resolver;
		} else
			return null;
	}

	@Override
	public <R> Promise<R> deferred(R r) {
		return invoke(r).defer();
	}

}
