package org.osgi.util.promise;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Resolver<T> {
	Object lock = new Object();
	boolean resolved;
	T value;
	Throwable error;
	ConcurrentLinkedDeque<Runnable> onresolve = new ConcurrentLinkedDeque<>();
	final Promise<T> promise = new Promise<T>() {

		@Override
		public T get() throws InvocationTargetException, InterruptedException {
			synchronized (lock) {
				while (!resolved)
					lock.wait();

				if (error != null)
					throw new InvocationTargetException(error);
				else
					return value;
			}
		}

		@Override
		public boolean isDone() {
			synchronized (lock) {
				return resolved;
			}
		}

		@Override
		public <R> Promise<R> then(final Success<R, T> ok, final Failure<T> fail)
				throws Exception {

			final Resolver<R> nextStage = new Resolver<R>();

			onresolve(new Runnable() {
				@Override
				public void run() {
					stage(nextStage, ok, fail);
				}
			});
			dequeue();
			return nextStage.getPromise();
		}

		@Override
		public <R> Promise<R> then(Success<R, T> success) throws Exception {
			return then(success, null);
		}

		@Override
		public void onresolve(Runnable done) throws Exception {
			synchronized (lock) {
				onresolve.add(done);
			}
			dequeue();
		}

		@Override
		public Throwable getError() throws IllegalStateException,
				InterruptedException {
			synchronized (lock) {
				while (!resolved)
					lock.wait();
			}
			return error;
		}

		/**
		 * Called when this promise is resolved. We need to callback our
		 * associated success and failure callbacks and chain the results.
		 * 
		 * @param nextStage
		 *            The next stage to
		 * @param ok
		 * @param fail
		 */

		private <R> void stage(final Resolver<R> nextStage,
				final Success<R, T> ok, final Failure<T> fail) {
			try {
				if (error == null)
					success(nextStage, ok);
				else
					fail(nextStage, fail);

			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

		private <R> void fail(final Resolver<R> nextStage, final Failure<T> fail)
				throws Exception {
			if (fail != null)
				fail.fail(this);
			nextStage.fail(error);
		}

		private <R> void success(final Resolver<R> nextStage,
				final Success<R, T> ok) throws Exception {
			try {
				final Promise<R> nextResult = ok.call(this);
				if (nextResult == null) {

					//
					// We directly resolve it if we have no
					// promise
					//

					nextStage.resolve(null);

				} else {
					
					//
					// We have to wait for the next result
					// to become available so we register
					// a callback
					//
					
					nextResult.onresolve(new Runnable() {
						public void run() {
							try {
								R value = nextResult.get();
								nextStage.resolve(value);
							} catch (Exception e) {
								nextStage.fail(e);
							}
						}
					});
				}
			} catch (Exception e) {
				nextStage.fail(e);
			}
		}
	};

	public Promise<T> getPromise() {
		return promise;
	}

	public void resolve(T value) {
		done(value, null);
	}

	public void fail(Throwable t) {
		done(null, t);
	}

	private void done(T value, Throwable error) {
		synchronized (lock) {
			if (resolved)
				throw new IllegalStateException("Already resolved " + this);
			this.value = value;
			this.error = error;
			resolved = true;
			lock.notifyAll();
		}
		dequeue();
	}

	private void dequeue() {
		if (!resolved)
			return;

		Runnable r;
		while ((r = onresolve.pollFirst()) != null) {
			try {
				r.run();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
