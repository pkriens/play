package org.osgi.util.promise;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utilities for promises.
 */
public class Promises {
	/**
	 * Exception thrown for Parallel execution. Provides access to all the
	 * exceptions thrown.
	 * 
	 */
	public static class ParallelException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private Object[] values;
		private Throwable[] exceptions;

		public ParallelException(Object[] values, Throwable[] exceptions) {
			this.setValues(values);
			this.setExceptions(exceptions);
		}

		public Throwable[] getExceptions() {
			return exceptions;
		}

		public void setExceptions(Throwable[] exceptions) {
			this.exceptions = exceptions;
		}

		public Object[] getValues() {
			return values;
		}

		public void setValues(Object[] values) {
			this.values = values;
		}

	}

	/**
	 * Run all the given promises in parallel. This will return a new promise that will
	 * be resolved when all the given promises have been resolved.
	 * 
	 * @param promises the array of promises
	 * @return a new promise
	 * @throws Exception
	 */
	@SafeVarargs
	static public <T> Promise<T[]> parallel(Promise<T>... promises) throws Exception {
		final Resolver<T[]> resolver = new Resolver<>();
		final AtomicInteger count = new AtomicInteger(promises.length);
		final Throwable[] exceptions = new Throwable[promises.length];
		@SuppressWarnings("unchecked")
		final T[] values = (T[]) new Object[promises.length];
		final AtomicBoolean errors = new AtomicBoolean(false);

		int i = 0;
		for (final Promise<T> p : promises) {
			final int index = i;

			p.onresolve(new Runnable() {

				@Override
				public void run() {
					try {
						if ((exceptions[index] = p.getError()) == null)
							values[index] = p.get();
						else
							errors.set(true);

					} catch (Exception e) {
						exceptions[index] = e;
						errors.set(true);
					}

					if (count.decrementAndGet() == 0) {
						if (errors.get())
							resolver.fail(new ParallelException(values,
									exceptions));
						else
							resolver.resolve(values);
					}
				}

			});
			i++;
		}
		return resolver.getPromise();
	}
}
